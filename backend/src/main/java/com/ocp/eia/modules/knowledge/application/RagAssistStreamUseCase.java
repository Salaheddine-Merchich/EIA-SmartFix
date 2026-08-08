package com.ocp.eia.modules.knowledge.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocp.eia.application.dto.AiDto.AiAssistRequest;
import com.ocp.eia.application.dto.AiDto.AiAssistResponse;
import com.ocp.eia.application.dto.AiDto.AiSuggestions;
import com.ocp.eia.application.dto.AiDto.SimilarInterventionDto;
import com.ocp.eia.config.AppProperties;
import com.ocp.eia.modules.knowledge.domain.model.AiDiagnosticTrace;
import com.ocp.eia.modules.knowledge.domain.model.RetrievedDocument;
import com.ocp.eia.modules.knowledge.domain.model.SearchContext;
import com.ocp.eia.modules.knowledge.domain.model.SimilarIntervention;
import com.ocp.eia.modules.knowledge.domain.model.SimilarKnowledgeDocument;
import com.ocp.eia.modules.knowledge.domain.port.EmbeddingProviderPort;
import com.ocp.eia.modules.knowledge.domain.port.InterventionTextSearchPort;
import com.ocp.eia.modules.knowledge.domain.port.KnowledgeDocumentSearchPort;
import com.ocp.eia.modules.knowledge.domain.port.LlmProviderPort;
import com.ocp.eia.modules.knowledge.domain.port.VectorStorePort;
import com.ocp.eia.domain.repository.EquipmentRepository;
import com.ocp.eia.domain.repository.FailureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@ConditionalOnProperty(name = "app.knowledge.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class RagAssistStreamUseCase {

    private static final String DISCLAIMER =
            "Assistance uniquement — les décisions finales restent celles du technicien ou de l'ingénieur.";

    private static final String SYSTEM_PROMPT = """
            Tu es un assistant technique EIA chez OCP (Office Chérifien des Phosphates).
            Tu ne poses JAMAIS de diagnostic définitif. Tu analyses les interventions passées similaires et proposes des pistes.
            Le technicien prend toujours la décision finale.
            
            INSTRUCTIONS CRITIQUES pour les correctiveActions:
            - Tu dois TOUJOURS générer entre 2 et 5 actions correctives précises et techniques
            - Chaque action doit être spécifique, avec des références techniques quand possible
            - Inclure les codes de pièces, références constructeurs, procédures normalisées
            - Ordonner par priorité d'intervention (urgence puis préventif)
            - Utiliser un vocabulaire technique professionnel
            
            EXEMPLES d'actions correctives de qualité:
            - "Vérifier l'isolement électrique avec mégohmmètre (spec >5 MΩ à 500V)"
            - "Remplacer roulement SKF 6312 côté accouplement selon procédure MT-R-001"
            - "Effectuer étalonnage 2 points avec configurateur HART selon norme ISA-5.1"
            - "Contrôler tension bobine contacteur (nominal 220V ±10%)"
            - "Appliquer couple de serrage 45 N.m sur brides DN150 selon DIN 2633"
            
            Réponds UNIQUEMENT en JSON valide avec cette structure exacte:
            {
                "probableCauses": ["cause technique 1", "cause technique 2"],
                "correctiveActions": [
                    "Action prioritaire avec référence technique",
                    "Action complémentaire avec procédure",
                    "Action préventive avec norme/standard"
                ],
                "summary": "Diagnostic technique basé sur les interventions similaires",
                "advice": "Conseil de maintenance préventive avec références"
            }
            
            IMPORTANT: Ne JAMAIS laisser correctiveActions vide. Si aucune intervention similaire précise n'est trouvée, propose des actions génériques mais techniques appropriées au type d'équipement mentionné.
            """;

    private final EmbeddingProviderPort embeddingProvider;
    private final VectorStorePort vectorStore;
    private final InterventionTextSearchPort interventionTextSearchPort;
    private final KnowledgeDocumentSearchPort knowledgeDocumentSearchPort;
    private final LlmProviderPort llmProvider;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final EquipmentRepository equipmentRepository;
    private final FailureRepository failureRepository;

    /**
     * Génère une assistance IA en streaming via Server-Sent Events
     */
    public Flux<ServerSentEvent<String>> assistStream(AiAssistRequest request) {
        log.info("Démarrage assistance streaming RAG pour: {}", request.description());
        
        return Flux.<ServerSentEvent<String>>create(sink -> {
            try {
                long retrievalStart = System.nanoTime();
                sink.next(ServerSentEvent.<String>builder()
                    .event("status")
                    .data("Recherche des documents similaires...")
                    .build());

                RetrievalResult retrieval = performRetrieval(request);
                long retrievalDurationMs = (System.nanoTime() - retrievalStart) / 1_000_000;
                
                sink.next(ServerSentEvent.<String>builder()
                    .event("context")
                    .data(String.format("Trouvé %d interventions et %d documents techniques", 
                         retrieval.interventions.size(), retrieval.knowledgeDocuments.size()))
                    .build());

                if (retrieval.interventions.isEmpty() && retrieval.knowledgeDocuments.isEmpty()) {
                    sink.next(ServerSentEvent.<String>builder()
                        .event("fallback")
                        .data("Aucun document similaire trouvé, conseil générique...")
                        .build());
                    
                    AiSuggestions fallback = fallbackSuggestions(request.description(), retrieval.knowledgeDocuments);
                    sink.next(ServerSentEvent.<String>builder()
                        .event("complete")
                        .data(serializeResponse(buildResponse(request, retrieval, fallback, retrievalDurationMs, 0L)))
                        .build());
                    
                    sink.complete();
                    return;
                }

                sink.next(ServerSentEvent.<String>builder()
                    .event("status")
                    .data("Génération de l'analyse...")
                    .build());

                String userPrompt = buildUserPrompt(request.description(), retrieval.interventions, retrieval.knowledgeDocuments);
                StringBuilder responseBuffer = new StringBuilder();
                long llmStart = System.nanoTime();
                
                llmProvider.stream(SYSTEM_PROMPT, userPrompt)
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
                            AiSuggestions suggestions = parseResponse(responseBuffer.toString());
                            sink.next(ServerSentEvent.<String>builder()
                                .event("complete")
                                .data(serializeResponse(buildResponse(request, retrieval, suggestions, retrievalDurationMs, llmDurationMs)))
                                .build());
                        } catch (Exception e) {
                            log.error("Erreur parsing réponse streaming: {}", e.getMessage());
                            AiSuggestions fallback = fallbackSuggestions(request.description(), retrieval.knowledgeDocuments);
                            
                            sink.next(ServerSentEvent.<String>builder()
                                .event("error")
                                .data("Erreur de parsing, utilisation de conseil générique")
                                .build());
                                
                            sink.next(ServerSentEvent.<String>builder()
                                .event("complete") 
                                .data(serializeResponse(buildResponse(request, retrieval, fallback, retrievalDurationMs, llmDurationMs)))
                                .build());
                        }
                        
                        sink.complete();
                    })
                    .doOnError(error -> {
                        log.error("Erreur streaming LLM: {}", error.getMessage());
                        long llmDurationMs = (System.nanoTime() - llmStart) / 1_000_000;
                        AiSuggestions fallback = fallbackSuggestions(request.description(), retrieval.knowledgeDocuments);
                        
                        sink.next(ServerSentEvent.<String>builder()
                            .event("error")
                            .data("Erreur LLM: " + error.getMessage())
                            .build());
                            
                        sink.next(ServerSentEvent.<String>builder()
                            .event("complete")
                            .data(serializeResponse(buildResponse(request, retrieval, fallback, retrievalDurationMs, llmDurationMs)))
                            .build());
                            
                        sink.complete();
                    })
                    .subscribe();

            } catch (Exception e) {
                log.error("Erreur critique lors de l'assistance streaming: {}", e.getMessage());
                sink.error(e);
            }
        })
        .timeout(Duration.ofSeconds(30)) // Timeout global
        .onErrorResume(error -> {
            log.error("Timeout ou erreur dans le streaming: {}", error.getMessage());
            AiSuggestions fallback = fallbackSuggestions(request.description(), List.of());
            RetrievalResult emptyRetrieval = new RetrievalResult(List.of(), List.of());
            String completePayload = serializeResponse(
                    buildResponse(request, emptyRetrieval, fallback, 0L, 0L));
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

    private RetrievalResult performRetrieval(AiAssistRequest request) throws Exception {
        int topK = request.topK() != null ? request.topK() : appProperties.getAi().getRag().getTopK();
        boolean hybridEnabled = appProperties.getAi().getRag().isHybridTextEnabled();

        // Génération embedding
        float[] queryEmbedding = embeddingProvider.embed(request.description());
        
        // Contexte de recherche
        SearchContext searchContext = buildSearchContext(request);

        // Recherches parallèles
        CompletableFuture<List<SimilarIntervention>> vectorFuture = CompletableFuture.supplyAsync(() -> 
            vectorStore.findSimilar(queryEmbedding, topK, searchContext));
        
        CompletableFuture<List<SimilarIntervention>> textFuture = hybridEnabled 
            ? CompletableFuture.supplyAsync(() -> interventionTextSearchPort.searchValidated(request.description(), topK, searchContext))
            : CompletableFuture.completedFuture(List.of());
        
        CompletableFuture<List<SimilarKnowledgeDocument>> knowledgeTextFuture = CompletableFuture.supplyAsync(() -> 
            knowledgeDocumentSearchPort.searchDocuments(request.description(), Math.min(topK, 3)));
            
        CompletableFuture<List<SimilarKnowledgeDocument>> knowledgeVectorFuture = CompletableFuture.supplyAsync(() -> 
            knowledgeDocumentSearchPort.searchByEmbedding(queryEmbedding, Math.min(topK, 3)));

        // Attendre tous les résultats
        CompletableFuture.allOf(vectorFuture, textFuture, knowledgeTextFuture, knowledgeVectorFuture).join();
        
        // Fusion des résultats
        List<SimilarKnowledgeDocument> mergedKnowledgeResults = mergeKnowledgeResults(
            knowledgeTextFuture.get(), knowledgeVectorFuture.get(), Math.min(topK, 3));
            
        HybridRetrievalMerger.UnifiedResults unifiedResults = HybridRetrievalMerger.mergeAll(
            vectorFuture.get(), textFuture.get(), mergedKnowledgeResults, topK);
        
        // Filtrage par seuil de similarité
        double threshold = appProperties.getAi().getRag().getSimilarityThreshold();
        List<SimilarIntervention> relevantInterventions = unifiedResults.interventions().stream()
            .filter(s -> s.similarity() >= threshold)
            .toList();
            
        return new RetrievalResult(relevantInterventions, unifiedResults.knowledgeDocuments());
    }

    // Méthodes utilitaires copiées/adaptées de RagAssistUseCase
    private SearchContext buildSearchContext(AiAssistRequest request) {
        try {
            // Si on a un equipmentId, récupérer les informations d'équipement
            if (request.equipmentId() != null) {
                var equipment = equipmentRepository.findById(request.equipmentId()).orElse(null);
                if (equipment != null) {
                    // Récupérer les boosts depuis la configuration ou utiliser les valeurs par défaut
                    double equipmentBoost = appProperties.getAi().getRag().getContext() != null 
                        ? appProperties.getAi().getRag().getContext().getEquipmentBoost() : 2.0;
                    double familyBoost = appProperties.getAi().getRag().getContext() != null 
                        ? appProperties.getAi().getRag().getContext().getFamilyBoost() : 1.5;
                    double zoneBoost = appProperties.getAi().getRag().getContext() != null 
                        ? appProperties.getAi().getRag().getContext().getZoneBoost() : 1.2;
                    
                    return SearchContext.withBoosts(
                        equipment.getId(),
                        request.failureId(),
                        equipment.getFamille(),
                        equipment.getZone(),
                        equipmentBoost,
                        familyBoost,
                        zoneBoost
                    );
                }
            }
            
            // Si on a un failureId mais pas d'equipmentId, récupérer via la panne
            if (request.failureId() != null) {
                var failure = failureRepository.findByIdWithDetails(request.failureId()).orElse(null);
                if (failure != null && failure.getEquipment() != null) {
                    var equipment = failure.getEquipment();
                    
                    double equipmentBoost = appProperties.getAi().getRag().getContext() != null 
                        ? appProperties.getAi().getRag().getContext().getEquipmentBoost() : 2.0;
                    double familyBoost = appProperties.getAi().getRag().getContext() != null 
                        ? appProperties.getAi().getRag().getContext().getFamilyBoost() : 1.5;
                    double zoneBoost = appProperties.getAi().getRag().getContext() != null 
                        ? appProperties.getAi().getRag().getContext().getZoneBoost() : 1.2;
                    
                    return SearchContext.withBoosts(
                        equipment.getId(),
                        failure.getId(),
                        equipment.getFamille(),
                        equipment.getZone(),
                        equipmentBoost,
                        familyBoost,
                        zoneBoost
                    );
                }
            }
            
            // Aucun contexte spécifique
            return SearchContext.none();
            
        } catch (Exception e) {
            log.warn("Erreur lors de la construction du contexte de recherche: {}", e.getMessage());
            return SearchContext.none();
        }
    }

    private List<SimilarKnowledgeDocument> mergeKnowledgeResults(
            List<SimilarKnowledgeDocument> textResults,
            List<SimilarKnowledgeDocument> vectorResults,
            int limit) {
        // Implementation similaire à RagAssistUseCase
        Map<UUID, SimilarKnowledgeDocument> mergedResults = new HashMap<>();
        
        textResults.forEach(doc -> mergedResults.put(doc.documentId(), doc));
        vectorResults.forEach(doc -> {
            SimilarKnowledgeDocument existing = mergedResults.get(doc.documentId());
            if (existing == null || doc.similarity() > existing.similarity()) {
                mergedResults.put(doc.documentId(), doc);
            }
        });
        
        return mergedResults.values().stream()
                .sorted((a, b) -> Double.compare(b.similarity(), a.similarity()))
                .limit(limit)
                .toList();
    }

    private String buildUserPrompt(String description, List<SimilarIntervention> interventions, List<SimilarKnowledgeDocument> knowledgeDocuments) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Description de la panne : ").append(description).append("\n\n");
        
        if (!interventions.isEmpty()) {
            prompt.append("Interventions similaires passées :\n");
            for (int i = 0; i < Math.min(5, interventions.size()); i++) {
                SimilarIntervention intervention = interventions.get(i);
                prompt.append(String.format("- %s: %s → %s\n",
                    intervention.equipmentCode(),
                    intervention.symptomes(),
                    intervention.actionsCorrectives()));
            }
            prompt.append("\n");
        }
        
        if (!knowledgeDocuments.isEmpty()) {
            prompt.append("Documentation technique pertinente :\n");
            for (SimilarKnowledgeDocument doc : knowledgeDocuments) {
                prompt.append(String.format("- %s: %s\n", doc.title(), doc.contentExcerpt()));
            }
        }
        
        return prompt.toString();
    }

    private AiSuggestions parseResponse(String response) {
        try {
            String json = extractJson(response);
            return objectMapper.readValue(json, AiSuggestions.class);
        } catch (JsonProcessingException e) {
            log.warn("Impossible de parser la réponse JSON: {}", response);
            throw new RuntimeException("Invalid JSON response", e);
        }
    }

    private String extractJson(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }

    private AiSuggestions fallbackSuggestions(String description, List<SimilarKnowledgeDocument> knowledgeDocuments) {
        List<String> causes = List.of("Cause non identifiée avec les données disponibles");
        List<String> actions = List.of(
            "Vérifier les connexions et l'alimentation",
            "Consulter la documentation constructeur",
            "Contacter un responsable EIA si le problème persiste"
        );
        
        String summary = "Analyse basée sur la documentation disponible uniquement.";
        String advice = knowledgeDocuments.isEmpty() 
            ? "Aucun document technique trouvé pour cette panne."
            : "Consulter les documents techniques identifiés pour plus de détails.";
            
        return new AiSuggestions(causes, actions, summary, advice);
    }

    private AiAssistResponse buildResponse(
            AiAssistRequest request,
            RetrievalResult retrieval,
            AiSuggestions suggestions,
            long retrievalDurationMs,
            long llmDurationMs
    ) {
        List<SimilarInterventionDto> similarDtos = retrieval.interventions.stream()
                .map(s -> new SimilarInterventionDto(
                        s.interventionId(),
                        s.equipmentCode(),
                        s.symptomes(),
                        s.causeRacine(),
                        s.actionsCorrectives(),
                        s.similarity()
                ))
                .toList();

        boolean hybridEnabled = appProperties.getAi().getRag().isHybridTextEnabled();
        AiDiagnosticTrace trace = buildTrace(
                request.description(),
                retrieval.interventions,
                retrieval.interventions.size(),
                retrieval.interventions.size(),
                retrieval.interventions.size(),
                "OK",
                hybridEnabled,
                retrievalDurationMs,
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

    private record RetrievalResult(
        List<SimilarIntervention> interventions,
        List<SimilarKnowledgeDocument> knowledgeDocuments
    ) {}
}