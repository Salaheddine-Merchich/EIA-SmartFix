package com.ocp.eia.modules.knowledge.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocp.eia.application.dto.AiDto.AiAssistRequest;
import com.ocp.eia.application.dto.AiDto.AiAssistResponse;
import com.ocp.eia.application.dto.AiDto.AiSuggestions;
import com.ocp.eia.application.dto.AiDto.SimilarInterventionDto;
import com.ocp.eia.modules.knowledge.application.RagRetrievalService.RetrievalOutcome;
import com.ocp.eia.modules.knowledge.domain.model.AiDiagnosticTrace;
import com.ocp.eia.modules.knowledge.domain.model.RetrievedDocument;
import com.ocp.eia.modules.knowledge.domain.model.SimilarIntervention;
import com.ocp.eia.modules.knowledge.domain.port.LlmProviderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

@Service
@ConditionalOnProperty(name = "app.knowledge.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class RagAssistStreamUseCase {

    private static final String DISCLAIMER =
            "Assistance uniquement — les décisions finales restent celles du technicien ou de l'ingénieur.";

    private static final AiSuggestions UNAVAILABLE_SUGGESTIONS = new AiSuggestions(
            List.of("L'assistance IA est temporairement indisponible"),
            List.of("Consulter la documentation constructeur ou contacter un responsable EIA"),
            "Le service de recherche intelligente n'a pas pu traiter votre demande pour le moment.",
            "Réessayez ultérieurement ou poursuivez le diagnostic manuellement."
    );

    private final RagRetrievalService ragRetrievalService;
    private final RagPromptBuilder ragPromptBuilder;
    private final RagSuggestionParser ragSuggestionParser;
    private final LlmProviderPort llmProvider;
    private final ObjectMapper objectMapper;

    /**
     * Génère une assistance IA en streaming via Server-Sent Events
     */
    public Flux<ServerSentEvent<String>> assistStream(AiAssistRequest request) {
        log.info("Démarrage assistance streaming RAG pour: {}", request.description());

        return Flux.<ServerSentEvent<String>>create(sink -> {
                    try {
                        sink.next(ServerSentEvent.<String>builder()
                                .event("status")
                                .data("Recherche des documents similaires...")
                                .build());

                        RetrievalOutcome retrieval = ragRetrievalService.retrieve(request);
                        if (retrieval.unavailable()) {
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

                        sink.next(ServerSentEvent.<String>builder()
                                .event("context")
                                .data(String.format("Trouvé %d interventions et %d documents techniques",
                                        retrieval.relevant().size(), retrieval.knowledgeDocuments().size()))
                                .build());

                        if (retrieval.relevant().isEmpty() && retrieval.knowledgeDocuments().isEmpty()) {
                            sink.next(ServerSentEvent.<String>builder()
                                    .event("fallback")
                                    .data("Aucun document similaire trouvé, conseil générique...")
                                    .build());

                            AiSuggestions fallback = ragSuggestionParser.noEvidenceFallback();
                            sink.next(ServerSentEvent.<String>builder()
                                    .event("complete")
                                    .data(serializeResponse(buildResponse(request, retrieval, fallback, 0L)))
                                    .build());

                            sink.complete();
                            return;
                        }

                        sink.next(ServerSentEvent.<String>builder()
                                .event("status")
                                .data("Génération de l'analyse...")
                                .build());

                        String userPrompt = ragPromptBuilder.userPrompt(
                                request.description(),
                                retrieval.relevant(),
                                retrieval.knowledgeDocuments()
                        );
                        StringBuilder responseBuffer = new StringBuilder();
                        long llmStart = System.nanoTime();

                        llmProvider.stream(ragPromptBuilder.systemPrompt(), userPrompt)
                                .doOnNext(token -> {
                                    responseBuffer.append(token);
                                    sink.next(ServerSentEvent.<String>builder()
                                            .event("token")
                                            .data(token)
                                            .build());
                                })
                                .doOnComplete(() -> {
                                    long llmDurationMs = (System.nanoTime() - llmStart) / 1_000_000;
                                    try {
                                        AiSuggestions suggestions = ragSuggestionParser.parse(responseBuffer.toString());
                                        sink.next(ServerSentEvent.<String>builder()
                                                .event("complete")
                                                .data(serializeResponse(buildResponse(request, retrieval, suggestions, llmDurationMs)))
                                                .build());
                                    } catch (Exception e) {
                                        log.error("Erreur parsing réponse streaming: {}", e.getMessage());
                                        AiSuggestions fallback = ragSuggestionParser.fallbackFromHistory(
                                                retrieval.relevant(),
                                                retrieval.knowledgeDocuments()
                                        );

                                        sink.next(ServerSentEvent.<String>builder()
                                                .event("error")
                                                .data("Erreur de parsing, utilisation de conseil générique")
                                                .build());

                                        sink.next(ServerSentEvent.<String>builder()
                                                .event("complete")
                                                .data(serializeResponse(buildResponse(request, retrieval, fallback, llmDurationMs)))
                                                .build());
                                    }

                                    sink.complete();
                                })
                                .doOnError(error -> {
                                    log.error("Erreur streaming LLM: {}", error.getMessage());
                                    long llmDurationMs = (System.nanoTime() - llmStart) / 1_000_000;
                                    AiSuggestions fallback = ragSuggestionParser.fallbackFromHistory(
                                            retrieval.relevant(),
                                            retrieval.knowledgeDocuments()
                                    );

                                    sink.next(ServerSentEvent.<String>builder()
                                            .event("error")
                                            .data("Erreur LLM: " + error.getMessage())
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
                        sink.error(e);
                    }
                })
                .timeout(Duration.ofSeconds(30))
                .onErrorResume(error -> {
                    log.error("Timeout ou erreur dans le streaming: {}", error.getMessage());
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
                });
    }

    private AiAssistResponse buildResponse(
            AiAssistRequest request,
            RetrievalOutcome retrieval,
            AiSuggestions suggestions,
            long llmDurationMs
    ) {
        List<SimilarInterventionDto> similarDtos = retrieval.relevant().stream()
                .map(s -> new SimilarInterventionDto(
                        s.interventionId(),
                        s.equipmentCode(),
                        s.symptomes(),
                        s.causeRacine(),
                        s.actionsCorrectives(),
                        s.similarity()
                ))
                .toList();

        AiDiagnosticTrace trace = buildTrace(
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

        return new AiAssistResponse(
                similarDtos,
                suggestions,
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

    private String serializeResponse(AiAssistResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            log.error("Erreur sérialisation réponse: {}", e.getMessage());
            return "{\"error\":\"Serialization failed\"}";
        }
    }
}
