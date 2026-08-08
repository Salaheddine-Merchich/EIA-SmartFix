package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.application.dto.AiDto.AiAssistRequest;
import com.ocp.eia.application.dto.AiDto.AiAssistResponse;
import com.ocp.eia.application.dto.AiDto.AiSuggestions;
import com.ocp.eia.application.dto.AiDto.SimilarInterventionDto;
import com.ocp.eia.modules.knowledge.application.RagRetrievalService.RetrievalOutcome;
import com.ocp.eia.modules.knowledge.domain.model.AiDiagnosticTrace;
import com.ocp.eia.modules.knowledge.domain.model.RetrievedDocument;
import com.ocp.eia.modules.knowledge.domain.model.SimilarIntervention;
import com.ocp.eia.modules.knowledge.domain.model.SimilarKnowledgeDocument;
import com.ocp.eia.modules.knowledge.domain.port.LlmProviderPort;
import com.ocp.eia.modules.knowledge.infrastructure.observability.RagObservabilityService;
import com.ocp.eia.modules.knowledge.infrastructure.observability.RagRetrievalMetrics;
import com.ocp.eia.modules.monitoring.application.event.AiServiceUnavailableEvent;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "app.knowledge.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class RagAssistUseCase {

    private static final String DISCLAIMER =
            "Assistance uniquement — les décisions finales restent celles du technicien ou de l'ingénieur.";

    private static final String UNAVAILABLE_CAUSE =
            "L'assistance IA est temporairement indisponible";
    private static final String UNAVAILABLE_ACTION =
            "Consulter la documentation constructeur ou contacter un responsable EIA";
    private static final String UNAVAILABLE_SUMMARY =
            "Le service de recherche intelligente n'a pas pu traiter votre demande pour le moment.";
    private static final String UNAVAILABLE_ADVICE =
            "Réessayez ultérieurement ou poursuivez le diagnostic manuellement.";

    private final RagRetrievalService ragRetrievalService;
    private final RagPromptBuilder ragPromptBuilder;
    private final RagSuggestionParser ragSuggestionParser;
    private final LlmProviderPort llmProvider;
    private final RagRetrievalMetrics ragRetrievalMetrics;
    private final RagObservabilityService ragObservabilityService;
    private final ApplicationEventPublisher eventPublisher;
    private final AiDiagnosticStatsService diagnosticStatsService;

    public AiAssistResponse assist(AiAssistRequest request) {
        Timer.Sample retrievalTimer = ragRetrievalMetrics.startRetrievalTimer();
        try {
            return assistInternal(request);
        } finally {
            ragRetrievalMetrics.recordRetrievalDuration(retrievalTimer);
        }
    }

    private AiAssistResponse assistInternal(AiAssistRequest request) {
        ragObservabilityService.incrementActiveQueries();

        try {
            RetrievalOutcome outcome = ragRetrievalService.retrieve(request);
            if (outcome.unavailable()) {
                publishAiUnavailable(outcome.unavailableReason());
                return unavailableResponse(
                        request.description(),
                        outcome.hybridEnabled(),
                        outcome.retrievalDurationMs(),
                        outcome.embeddingStatus()
                );
            }

            List<SimilarIntervention> relevant = outcome.relevant();
            List<SimilarKnowledgeDocument> knowledgeResults = outcome.knowledgeDocuments();

            List<SimilarInterventionDto> similarDtos = relevant.stream()
                    .map(s -> new SimilarInterventionDto(
                            s.interventionId(),
                            s.equipmentCode(),
                            s.symptomes(),
                            s.causeRacine(),
                            s.actionsCorrectives(),
                            s.similarity()
                    ))
                    .toList();

            SuggestionResult suggestionResult = generateSuggestions(request.description(), relevant, knowledgeResults);
            AiDiagnosticTrace trace = buildTrace(
                    request.description(),
                    relevant,
                    outcome.vectorCount(),
                    outcome.textCount(),
                    outcome.mergedCount(),
                    outcome.embeddingStatus(),
                    outcome.hybridEnabled(),
                    outcome.retrievalDurationMs(),
                    suggestionResult.llmDurationMs()
            );
            diagnosticStatsService.record(trace);

            AiAssistResponse response = new AiAssistResponse(
                    similarDtos,
                    suggestionResult.suggestions(),
                    DISCLAIMER,
                    AiDiagnosticTraceMapper.toDto(trace)
            );

            ragObservabilityService.recordSuccessfulQuery();
            if (suggestionResult.suggestions() != null
                    && (suggestionResult.suggestions().probableCauses().isEmpty()
                    || suggestionResult.suggestions().correctiveActions().isEmpty())) {
                ragObservabilityService.recordLowConfidenceResponse();
            }

            return response;

        } catch (Exception e) {
            ragObservabilityService.recordError("assist_internal_error");
            throw e;
        } finally {
            ragObservabilityService.decrementActiveQueries();
        }
    }

    private AiAssistResponse unavailableResponse(
            String query,
            boolean hybridEnabled,
            long retrievalDurationMs,
            String embeddingStatus
    ) {
        AiDiagnosticTrace trace = new AiDiagnosticTrace(
                query,
                List.of(),
                0,
                0,
                0,
                0,
                0.0,
                0.0,
                retrievalDurationMs,
                0L,
                embeddingStatus,
                hybridEnabled
        );
        ragObservabilityService.recordFallbackResponse();

        return new AiAssistResponse(
                List.of(),
                new AiSuggestions(
                        List.of(UNAVAILABLE_CAUSE),
                        List.of(UNAVAILABLE_ACTION),
                        UNAVAILABLE_SUMMARY,
                        UNAVAILABLE_ADVICE
                ),
                DISCLAIMER,
                AiDiagnosticTraceMapper.toDto(trace)
        );
    }

    private AiDiagnosticTrace buildTrace(
            String query,
            List<SimilarIntervention> relevant,
            int vectorCount,
            int textCount,
            int mergedCount,
            String embeddingStatus,
            boolean hybridEnabled,
            long retrievalDurationMs,
            long llmDurationMs
    ) {
        List<RetrievedDocument> documents = relevant.stream()
                .map(s -> new RetrievedDocument(
                        s.interventionId(),
                        s.equipmentCode(),
                        s.symptomes(),
                        s.causeRacine(),
                        s.similarity()
                ))
                .toList();

        double averageSimilarity = relevant.isEmpty()
                ? 0.0
                : relevant.stream().mapToDouble(SimilarIntervention::similarity).average().orElse(0.0);
        double confidenceScore = ConfidenceCalculator.compute(averageSimilarity, relevant.size());

        return new AiDiagnosticTrace(
                query,
                documents,
                vectorCount,
                textCount,
                mergedCount,
                relevant.size(),
                averageSimilarity,
                confidenceScore,
                retrievalDurationMs,
                llmDurationMs,
                embeddingStatus,
                hybridEnabled
        );
    }

    private void publishAiUnavailable(String reason) {
        eventPublisher.publishEvent(new AiServiceUnavailableEvent(reason));
    }

    private SuggestionResult generateSuggestions(
            String description,
            List<SimilarIntervention> similar,
            List<SimilarKnowledgeDocument> knowledgeDocuments
    ) {
        if (similar.isEmpty() && knowledgeDocuments.isEmpty()) {
            return new SuggestionResult(ragSuggestionParser.noEvidenceFallback(), 0L);
        }

        String userPrompt = ragPromptBuilder.userPrompt(description, similar, knowledgeDocuments);

        long llmStart = System.nanoTime();
        try {
            ragRetrievalMetrics.recordLlmCall();
            String response = llmProvider.complete(ragPromptBuilder.systemPrompt(), userPrompt);
            return new SuggestionResult(ragSuggestionParser.parse(response), elapsedMs(llmStart));
        } catch (Exception e) {
            log.error("Erreur génération LLM: {}", e.getMessage());
            return new SuggestionResult(
                    ragSuggestionParser.fallbackFromHistory(similar, knowledgeDocuments),
                    elapsedMs(llmStart)
            );
        }
    }

    private static long elapsedMs(long startNano) {
        return (System.nanoTime() - startNano) / 1_000_000L;
    }

    private record SuggestionResult(AiSuggestions suggestions, long llmDurationMs) {}
}
