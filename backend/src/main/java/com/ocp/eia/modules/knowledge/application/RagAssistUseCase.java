package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.application.dto.AiDto.AiAssistRequest;
import com.ocp.eia.application.dto.AiDto.AiAssistResponse;
import com.ocp.eia.application.dto.AiDto.AiSuggestions;
import com.ocp.eia.modules.knowledge.application.RagRetrievalService.RetrievalOutcome;
import com.ocp.eia.modules.knowledge.application.RagSuggestionService.SuggestionResult;
import com.ocp.eia.modules.knowledge.domain.model.AiDiagnosticTrace;
import com.ocp.eia.modules.knowledge.domain.model.SimilarIntervention;
import com.ocp.eia.modules.knowledge.domain.model.SimilarKnowledgeDocument;
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

    private static final String UNAVAILABLE_CAUSE =
            "L'assistance IA est temporairement indisponible";
    private static final String UNAVAILABLE_ACTION =
            "Consulter la documentation constructeur ou contacter un responsable EIA";
    private static final String UNAVAILABLE_SUMMARY =
            "Le service de recherche intelligente n'a pas pu traiter votre demande pour le moment.";
    private static final String UNAVAILABLE_ADVICE =
            "Réessayez ultérieurement ou poursuivez le diagnostic manuellement.";

    private final RagRetrievalService ragRetrievalService;
    private final RagSuggestionService ragSuggestionService;
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
        String description = request.description() != null ? request.description() : "";
        log.info("Démarrage assistance RAG: queryLength={}", description.length());
        if (log.isDebugEnabled()) {
            log.debug("RAG assist query received: chars={}", description != null ? description.length() : 0);
        }

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

            SuggestionResult suggestionResult = ragSuggestionService.generateSuggestions(
                    request.description(), relevant, knowledgeResults);
            AiDiagnosticTrace trace = AiDiagnosticTraceFactory.buildTrace(
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

            AiAssistResponse response = AiDiagnosticTraceFactory.toResponse(
                    relevant,
                    suggestionResult.suggestions(),
                    trace
            );

            log.info(
                    "Assistance RAG terminée: retrievalMs={}, llmMs={}, fastPath={}, interventions={}, docs={}, embeddingStatus={}",
                    outcome.retrievalDurationMs(),
                    suggestionResult.llmDurationMs(),
                    suggestionResult.fastPathUsed(),
                    relevant.size(),
                    knowledgeResults.size(),
                    outcome.embeddingStatus()
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
        AiDiagnosticTrace trace = AiDiagnosticTraceFactory.buildTrace(
                query,
                List.of(),
                0,
                0,
                0,
                embeddingStatus,
                hybridEnabled,
                retrievalDurationMs,
                0L
        );
        ragObservabilityService.recordFallbackResponse();

        return AiDiagnosticTraceFactory.toResponse(
                List.of(),
                new AiSuggestions(
                        List.of(UNAVAILABLE_CAUSE),
                        List.of(UNAVAILABLE_ACTION),
                        UNAVAILABLE_SUMMARY,
                        UNAVAILABLE_ADVICE
                ),
                trace
        );
    }

    private void publishAiUnavailable(String reason) {
        eventPublisher.publishEvent(new AiServiceUnavailableEvent(reason));
    }
}
