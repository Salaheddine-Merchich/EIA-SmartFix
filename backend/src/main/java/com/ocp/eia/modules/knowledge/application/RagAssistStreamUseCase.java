package com.ocp.eia.modules.knowledge.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocp.eia.application.dto.AiDto.AiAssistRequest;
import com.ocp.eia.application.dto.AiDto.AiAssistResponse;
import com.ocp.eia.application.dto.AiDto.AiSuggestions;
import com.ocp.eia.application.dto.AiDto.EquipmentSchemaDto;
import com.ocp.eia.config.AppProperties;
import com.ocp.eia.modules.knowledge.domain.model.QuerySignals;
import com.ocp.eia.modules.knowledge.domain.model.SearchContext;
import com.ocp.eia.modules.knowledge.application.RagRetrievalService.RetrievalOutcome;
import com.ocp.eia.modules.knowledge.application.RagSuggestionService.SuggestionResult;
import com.ocp.eia.modules.knowledge.domain.model.AiDiagnosticTrace;
import com.ocp.eia.modules.knowledge.domain.port.LlmProviderPort;
import com.ocp.eia.modules.knowledge.infrastructure.observability.RagObservabilityService;
import com.ocp.eia.modules.knowledge.infrastructure.observability.RagRetrievalMetrics;
import com.ocp.eia.modules.monitoring.application.event.AiServiceUnavailableEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@ConditionalOnProperty(name = "app.knowledge.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class RagAssistStreamUseCase {

    private static final AiSuggestions UNAVAILABLE_SUGGESTIONS = new AiSuggestions(
            List.of("L'assistance IA est temporairement indisponible"),
            List.of("Consulter la documentation constructeur ou contacter un responsable EIA"),
            "Le service de recherche intelligente n'a pas pu traiter votre demande pour le moment.",
            "Réessayez ultérieurement ou poursuivez le diagnostic manuellement."
    );

    private final RagRetrievalService ragRetrievalService;
    private final RagSuggestionService ragSuggestionService;
    private final RagPromptBuilder ragPromptBuilder;
    private final RagSuggestionParser ragSuggestionParser;
    private final LlmProviderPort llmProvider;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;
    private final RagObservabilityService ragObservabilityService;
    private final RagRetrievalMetrics ragRetrievalMetrics;
    private final AiDiagnosticStatsService diagnosticStatsService;
    private final ApplicationEventPublisher eventPublisher;
    private final EquipmentSchemaMatcher equipmentSchemaMatcher;
    private final SearchContextFactory searchContextFactory;

    /**
     * Génère une assistance IA en streaming via Server-Sent Events
     */
    public Flux<ServerSentEvent<String>> assistStream(AiAssistRequest request) {
        String description = request.description() != null ? request.description() : "";
        log.info("Démarrage assistance streaming RAG: queryLength={}", description.length());
        if (log.isDebugEnabled()) {
            log.debug("RAG stream query received: chars={}", description != null ? description.length() : 0);
        }

        Duration streamTimeout = DurationParse.of(appProperties.getAi().getRag().getPerformance().getLlmTimeout());
        AtomicBoolean recordedOutcome = new AtomicBoolean(false);

        return Flux.<ServerSentEvent<String>>create(sink -> {
                    try {
                        if (!AssistQueryValidator.isValid(description)) {
                            recordFallbackOnce(recordedOutcome);
                            AiSuggestions vague = ragSuggestionParser.vagueQueryFallback();
                            sink.next(ServerSentEvent.<String>builder()
                                    .event("fallback")
                                    .data("Description trop vague")
                                    .build());
                            RetrievalOutcome emptyRetrieval = emptyRetrievalOutcome();
                            sink.next(ServerSentEvent.<String>builder()
                                    .event("complete")
                                    .data(serializeResponse(buildResponse(request, emptyRetrieval, vague, 0L)))
                                    .build());
                            sink.complete();
                            return;
                        }

                        sink.next(ServerSentEvent.<String>builder()
                                .event("status")
                                .data("Recherche des documents similaires...")
                                .build());

                        RetrievalOutcome retrieval = ragRetrievalService.retrieve(request);
                        if (retrieval.unavailable()) {
                            publishAiUnavailable(retrieval.unavailableReason());
                            recordFallbackOnce(recordedOutcome);
                            sink.next(ServerSentEvent.<String>builder()
                                    .event("error")
                                    .data(retrieval.unavailableReason() != null
                                            ? retrieval.unavailableReason()
                                            : "Recherche indisponible")
                                    .build());
                            sink.next(ServerSentEvent.<String>builder()
                                    .event("complete")
                                    .data(serializeResponse(buildResponse(request, retrieval, UNAVAILABLE_SUGGESTIONS, 0L)))
                                    .build());
                            sink.complete();
                            return;
                        }

                        if (retrieval.codeNotFound()) {
                            recordFallbackOnce(recordedOutcome);
                            AiSuggestions unknownCodeSuggestions = ragSuggestionParser.unknownFaultCodeFallback(
                                    retrieval.unknownFaultCode());
                            sink.next(ServerSentEvent.<String>builder()
                                    .event("fallback")
                                    .data("Code défaut non trouvé dans la base de connaissances")
                                    .build());
                            sink.next(ServerSentEvent.<String>builder()
                                    .event("complete")
                                    .data(serializeResponse(buildResponse(
                                            request, retrieval, unknownCodeSuggestions, 0L)))
                                    .build());
                            sink.complete();
                            return;
                        }

                        sink.next(ServerSentEvent.<String>builder()
                                .event("context")
                                .data(String.format("Trouvé %d interventions et %d documents techniques",
                                        retrieval.relevant().size(), retrieval.knowledgeDocuments().size()))
                                .build());

                        if (!ragSuggestionService.hasProjectEvidence(
                                request.description(),
                                retrieval.relevant(),
                                retrieval.knowledgeDocuments())) {
                            recordFallbackOnce(recordedOutcome);
                            AiSuggestions insufficient = ragSuggestionParser.insufficientEvidenceFallback();
                            sink.next(ServerSentEvent.<String>builder()
                                    .event("fallback")
                                    .data("Aucune donnée fiable du projet pour cette description")
                                    .build());
                            sink.next(ServerSentEvent.<String>builder()
                                    .event("complete")
                                    .data(serializeResponse(buildResponse(
                                            request, retrieval, insufficient, 0L)))
                                    .build());
                            sink.complete();
                            return;
                        }

                        if (ragSuggestionService.shouldUseFastPath(retrieval.relevant())) {
                            sink.next(ServerSentEvent.<String>builder()
                                    .event("status")
                                    .data("Réponse basée sur l'historique (mode rapide)")
                                    .build());

                            SuggestionResult fastPathResult = ragSuggestionService.generateSuggestions(
                                    request.description(),
                                    retrieval.relevant(),
                                    retrieval.knowledgeDocuments()
                            );
                            recordSuccessOnce(recordedOutcome);
                            sink.next(ServerSentEvent.<String>builder()
                                    .event("complete")
                                    .data(serializeResponse(buildResponse(
                                            request, retrieval, fastPathResult.suggestions(), fastPathResult.llmDurationMs())))
                                    .build());
                            sink.complete();
                            return;
                        }

                        sink.next(ServerSentEvent.<String>builder()
                                .event("status")
                                .data("Génération de l'analyse...")
                                .build());

                        String systemPrompt = ragPromptBuilder.systemPrompt();
                        String userPrompt = ragPromptBuilder.userPrompt(
                                request.description(),
                                retrieval.relevant(),
                                retrieval.knowledgeDocuments()
                        );
                        log.info(
                                "LLM stream start: systemPromptChars={}, userPromptChars={}, interventions={}, docs={}",
                                systemPrompt.length(),
                                userPrompt.length(),
                                retrieval.relevant().size(),
                                retrieval.knowledgeDocuments().size()
                        );
                        if (log.isDebugEnabled()) {
                            log.debug("LLM stream userPrompt prepared: chars={}", userPrompt.length());
                        }
                        StringBuilder responseBuffer = new StringBuilder();
                        long llmStart = System.nanoTime();
                        ragRetrievalMetrics.recordLlmCall();

                        llmProvider.stream(systemPrompt, userPrompt)
                                .doOnNext(token -> {
                                    responseBuffer.append(token);
                                    sink.next(ServerSentEvent.<String>builder()
                                            .event("token")
                                            .data(token)
                                            .build());
                                })
                                .doOnComplete(() -> {
                                    long llmDurationMs = (System.nanoTime() - llmStart) / 1_000_000;
                                    log.info(
                                            "LLM stream complete: responseChars={}, durationMs={}",
                                            responseBuffer.length(),
                                            llmDurationMs
                                    );
                                    try {
                                        AiSuggestions suggestions = ragSuggestionParser.parse(responseBuffer.toString());
                                        recordSuccessOnce(recordedOutcome);
                                        sink.next(ServerSentEvent.<String>builder()
                                                .event("complete")
                                                .data(serializeResponse(buildResponse(request, retrieval, suggestions, llmDurationMs)))
                                                .build());
                                    } catch (Exception e) {
                                        log.error("Erreur parsing réponse streaming: {}", e.getMessage());
                                        recordFallbackOnce(recordedOutcome);
                                        AiSuggestions fallback = retrieval.relevant().isEmpty()
                                                ? ragSuggestionParser.insufficientEvidenceFallback()
                                                : ragSuggestionParser.fallbackFromHistory(
                                                        retrieval.relevant(),
                                                        retrieval.knowledgeDocuments()
                                                );

                                        sink.next(ServerSentEvent.<String>builder()
                                                .event("error")
                                                .data("Erreur de parsing, réponse basée sur les données disponibles")
                                                .build());

                                        sink.next(ServerSentEvent.<String>builder()
                                                .event("complete")
                                                .data(serializeResponse(buildResponse(request, retrieval, fallback, llmDurationMs)))
                                                .build());
                                    }

                                    sink.complete();
                                })
                                .doOnError(error -> {
                                    long llmDurationMs = (System.nanoTime() - llmStart) / 1_000_000;
                                    log.error(
                                            "Erreur streaming LLM after {}ms (partialResponseChars={}): {}",
                                            llmDurationMs,
                                            responseBuffer.length(),
                                            error.getMessage()
                                    );
                                    recordFallbackOnce(recordedOutcome);
                                    AiSuggestions fallback = retrieval.relevant().isEmpty()
                                            ? ragSuggestionParser.insufficientEvidenceFallback()
                                            : ragSuggestionParser.fallbackFromHistory(
                                                    retrieval.relevant(),
                                                    retrieval.knowledgeDocuments()
                                            );

                                    sink.next(ServerSentEvent.<String>builder()
                                            .event("error")
                                            .data("Service IA temporairement indisponible")
                                            .build());

                                    sink.next(ServerSentEvent.<String>builder()
                                            .event("complete")
                                            .data(serializeResponse(buildResponse(request, retrieval, fallback, llmDurationMs)))
                                            .build());

                                    sink.complete();
                                })
                                .subscribe();

                    } catch (Exception e) {
                        log.error("Erreur critique lors de l'assistance streaming: {}", e.getMessage());
                        ragObservabilityService.recordError("assist_stream_internal_error");
                        sink.error(e);
                    }
                })
                .timeout(streamTimeout)
                .onErrorResume(error -> {
                    log.error("Timeout ou erreur dans le streaming: {}", error.getMessage());
                    recordFallbackOnce(recordedOutcome);
                    publishAiUnavailable("Timeout ou erreur système");
                    AiSuggestions fallback = ragSuggestionParser.noEvidenceFallback();
                    RetrievalOutcome emptyRetrieval = RetrievalOutcome.unavailable(false, 0L, "FAILED", "Timeout ou erreur système");
                    String completePayload = serializeResponse(
                            buildResponse(request, emptyRetrieval, fallback, 0L));
                    return Flux.just(
                            ServerSentEvent.<String>builder()
                                    .event("error")
                                    .data("Timeout ou erreur système")
                                    .build(),
                            ServerSentEvent.<String>builder()
                                    .event("complete")
                                    .data(completePayload)
                                    .build()
                    );
                })
                .doOnSubscribe(subscription -> ragObservabilityService.incrementActiveQueries())
                .doFinally(signal -> ragObservabilityService.decrementActiveQueries());
    }

    private static RetrievalOutcome emptyRetrievalOutcome() {
        return new RetrievalOutcome(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                0,
                0,
                0,
                "SKIPPED",
                false,
                0L,
                false,
                null,
                false,
                null
        );
    }

    private void recordSuccessOnce(AtomicBoolean recordedOutcome) {
        if (recordedOutcome.compareAndSet(false, true)) {
            ragObservabilityService.recordSuccessfulQuery();
        }
    }

    private void recordFallbackOnce(AtomicBoolean recordedOutcome) {
        if (recordedOutcome.compareAndSet(false, true)) {
            ragObservabilityService.recordFallbackResponse();
        }
    }

    private void publishAiUnavailable(String reason) {
        eventPublisher.publishEvent(new AiServiceUnavailableEvent(reason));
    }

    private AiAssistResponse buildResponse(
            AiAssistRequest request,
            RetrievalOutcome retrieval,
            AiSuggestions suggestions,
            long llmDurationMs
    ) {
        AiDiagnosticTrace trace = AiDiagnosticTraceFactory.buildTrace(
                request.description(),
                retrieval.relevant(),
                retrieval.vectorCount(),
                retrieval.textCount(),
                retrieval.mergedCount(),
                retrieval.embeddingStatus(),
                retrieval.hybridEnabled(),
                retrieval.retrievalDurationMs(),
                llmDurationMs
        );
        diagnosticStatsService.record(trace);
        if (suggestions != null
                && (suggestions.probableCauses().isEmpty() || suggestions.correctiveActions().isEmpty())) {
            ragObservabilityService.recordLowConfidenceResponse();
        }

        return AiDiagnosticTraceFactory.toResponse(
                retrieval.relevant(),
                suggestions,
                trace,
                matchSchemas(request)
        );
    }

    private List<EquipmentSchemaDto> matchSchemas(AiAssistRequest request) {
        QuerySignals signals = QuerySignalExtractor.extract(request.description());
        SearchContext context = searchContextFactory.from(request, signals);
        return equipmentSchemaMatcher.match(signals, context);
    }

    private String serializeResponse(AiAssistResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            log.error("Erreur sérialisation réponse: {}", e.getMessage());
            return "{\"error\":\"Serialization failed\"}";
        }
    }

    private static String truncateForLog(String value, int maxChars) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, maxChars) + "...";
    }
}
