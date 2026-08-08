package com.ocp.eia.modules.knowledge.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocp.eia.application.dto.AiDto.AiAssistRequest;
import com.ocp.eia.application.dto.AiDto.AiAssistResponse;
import com.ocp.eia.application.dto.AiDto.AiSuggestions;
import com.ocp.eia.application.dto.AiDto.SimilarInterventionDto;
import com.ocp.eia.config.AppProperties;
import com.ocp.eia.domain.model.Equipment;
import com.ocp.eia.domain.model.Failure;
import com.ocp.eia.domain.repository.EquipmentRepository;
import com.ocp.eia.domain.repository.FailureRepository;
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
import com.ocp.eia.modules.knowledge.infrastructure.observability.RagRetrievalMetrics;
import com.ocp.eia.modules.knowledge.infrastructure.observability.RagObservabilityService;
import com.ocp.eia.modules.monitoring.application.event.AiServiceUnavailableEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Timer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

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
    private final RagRetrievalMetrics ragRetrievalMetrics;
    private final RagObservabilityService ragObservabilityService;
    private final ApplicationEventPublisher eventPublisher;
    private final AiDiagnosticStatsService diagnosticStatsService;
    private final EquipmentRepository equipmentRepository;
    private final FailureRepository failureRepository;
    
    @Qualifier("ragExecutor")
    private final Executor ragExecutor;

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
            int topK = request.topK() != null ? request.topK() : appProperties.getAi().getRag().getTopK();
            boolean hybridEnabled = appProperties.getAi().getRag().isHybridTextEnabled();
            long retrievalStart = System.nanoTime();

        float[] queryEmbedding;
        String embeddingStatus;
        try {
            queryEmbedding = embeddingProvider.embed(request.description());
            embeddingStatus = "OK";
        } catch (Exception e) {
            log.error("Erreur embedding RAG: {}", e.getMessage());
            publishAiUnavailable("Embedding indisponible");
            return unavailableResponse(request.description(), hybridEnabled, elapsedMs(retrievalStart), "FAILED");
        }

        // Construire le contexte de recherche pour le filtrage et la pondération
        SearchContext searchContext = buildSearchContext(request);
        log.debug("Contexte de recherche: equipmentId={}, failureId={}, family={}, zone={}", 
                 searchContext.equipmentId(), searchContext.failureId(), 
                 searchContext.equipmentFamily(), searchContext.equipmentZone());

        // Parallel retrieval: vector, text, and knowledge document searches in parallel using dedicated executor
        CompletableFuture<List<SimilarIntervention>> vectorFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return vectorStore.findSimilar(queryEmbedding, topK, searchContext);
            } catch (Exception e) {
                log.error("Erreur recherche vectorielle RAG: {}", e.getMessage());
                throw new RuntimeException("Vector search failed", e);
            }
        }, ragExecutor);

        CompletableFuture<List<SimilarIntervention>> textFuture = hybridEnabled 
            ? CompletableFuture.supplyAsync(() -> {
                try {
                    return interventionTextSearchPort.searchValidated(request.description(), topK, searchContext);
                } catch (Exception e) {
                    log.warn("Erreur recherche texte RAG: {}, poursuite avec résultats vectoriels uniquement", e.getMessage());
                    return List.<SimilarIntervention>of();
                }
            }, ragExecutor)
            : CompletableFuture.completedFuture(List.<SimilarIntervention>of());

        // Add knowledge documents search in parallel (both vector and text)
        CompletableFuture<List<SimilarKnowledgeDocument>> knowledgeTextFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return knowledgeDocumentSearchPort.searchDocuments(request.description(), Math.min(topK, 3));
            } catch (Exception e) {
                log.warn("Erreur recherche texte documents connaissances: {}", e.getMessage());
                return List.<SimilarKnowledgeDocument>of();
            }
        }, ragExecutor);
        
        CompletableFuture<List<SimilarKnowledgeDocument>> knowledgeVectorFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return knowledgeDocumentSearchPort.searchByEmbedding(queryEmbedding, Math.min(topK, 3));
            } catch (Exception e) {
                log.warn("Erreur recherche vectorielle documents connaissances: {}", e.getMessage());
                return List.<SimilarKnowledgeDocument>of();
            }
        }, ragExecutor);

        // Wait for all searches to complete
        List<SimilarIntervention> vectorResults;
        List<SimilarIntervention> textResults;
        List<SimilarKnowledgeDocument> knowledgeTextResults;
        List<SimilarKnowledgeDocument> knowledgeVectorResults;
        try {
            CompletableFuture.allOf(vectorFuture, textFuture, knowledgeTextFuture, knowledgeVectorFuture).join();
            vectorResults = vectorFuture.get();
            textResults = textFuture.get();
            knowledgeTextResults = knowledgeTextFuture.get();
            knowledgeVectorResults = knowledgeVectorFuture.get();
            
        } catch (Exception e) {
            log.error("Erreur lors de la recherche parallèle RAG: {}", e.getMessage());
            if (e.getCause() instanceof RuntimeException && e.getCause().getMessage().contains("Vector search failed")) {
                publishAiUnavailable("Recherche vectorielle indisponible");
                return unavailableResponse(request.description(), hybridEnabled, elapsedMs(retrievalStart), embeddingStatus);
            }
            // If only text or knowledge search failed, continue with available results
            try {
                vectorResults = vectorFuture.get();
                textResults = textFuture.isCompletedExceptionally() ? List.of() : textFuture.get();
                knowledgeTextResults = knowledgeTextFuture.isCompletedExceptionally() 
                    ? List.of() : knowledgeTextFuture.get();
                knowledgeVectorResults = knowledgeVectorFuture.isCompletedExceptionally() 
                    ? List.of() : knowledgeVectorFuture.get();
                
                log.warn("Poursuite avec résultats partiels - Interventions: {}, Documents texte: {}, Documents vectoriels: {}", 
                         vectorResults.size() + textResults.size(), knowledgeTextResults.size(), knowledgeVectorResults.size());
            } catch (Exception vectorEx) {
                log.error("Erreur recherche vectorielle critique: {}", vectorEx.getMessage());
                publishAiUnavailable("Recherche vectorielle indisponible");
                return unavailableResponse(request.description(), hybridEnabled, elapsedMs(retrievalStart), embeddingStatus);
            }
        }
        
        ragRetrievalMetrics.recordVectorCount(vectorResults.size());
        ragRetrievalMetrics.recordTextCount(textResults.size());

        // Fusion unifiée de toutes les sources avec RRF
        List<SimilarKnowledgeDocument> mergedKnowledgeResults = mergeKnowledgeResults(knowledgeTextResults, knowledgeVectorResults, Math.min(topK, 3));
        HybridRetrievalMerger.UnifiedResults unifiedResults = HybridRetrievalMerger.mergeAll(
            vectorResults, textResults, mergedKnowledgeResults, topK);
        
        List<SimilarIntervention> similar = unifiedResults.interventions();
        List<SimilarKnowledgeDocument> knowledgeResults = unifiedResults.knowledgeDocuments();
        
        ragRetrievalMetrics.recordMergedCount(similar.size());
        log.debug("RAG unified: {} interventions vectorielles, {} interventions texte, {} docs texte, {} docs vectoriels → {} interventions finales, {} documents finaux",
                vectorResults.size(), textResults.size(), knowledgeTextResults.size(), knowledgeVectorResults.size(), 
                similar.size(), knowledgeResults.size());

        double similarityThreshold = appProperties.getAi().getRag().getSimilarityThreshold();
        List<SimilarIntervention> relevant = similar.stream()
                .filter(s -> s.similarity() >= similarityThreshold)
                .toList();
        ragRetrievalMetrics.recordFilteredCount(relevant.size());

        long retrievalDurationMs = elapsedMs(retrievalStart);

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
                vectorResults.size(),
                textResults.size(),
                similar.size(),
                embeddingStatus,
                hybridEnabled,
                retrievalDurationMs,
                suggestionResult.llmDurationMs()
        );
        diagnosticStatsService.record(trace);

            AiAssistResponse response = new AiAssistResponse(
                    similarDtos,
                    suggestionResult.suggestions(),
                    DISCLAIMER,
                    AiDiagnosticTraceMapper.toDto(trace)
            );
            
            // Enregistrer le succès et la qualité
            ragObservabilityService.recordSuccessfulQuery();
            if (suggestionResult.suggestions() != null && 
                (suggestionResult.suggestions().probableCauses().isEmpty() || 
                 suggestionResult.suggestions().correctiveActions().isEmpty())) {
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

    private SuggestionResult generateSuggestions(String description, List<SimilarIntervention> similar, List<SimilarKnowledgeDocument> knowledgeDocuments) {
        if (similar.isEmpty() && knowledgeDocuments.isEmpty()) {
            return new SuggestionResult(
                    new AiSuggestions(
                            List.of("Aucune intervention similaire validée trouvée"),
                            List.of("Consulter la documentation constructeur"),
                            "Pas assez de données historiques validées pour cette description.",
                            "Documentez cette intervention pour enrichir la base de connaissances."
                    ),
                    0L
            );
        }

        StringBuilder userPromptBuilder = new StringBuilder();
        userPromptBuilder.append("Description de la panne actuelle:\n").append(description).append("\n\n");
        
        if (!similar.isEmpty()) {
            userPromptBuilder.append("Interventions passées similaires (validées):\n")
                           .append(buildInterventionContext(similar)).append("\n\n");
        }
        
        if (!knowledgeDocuments.isEmpty()) {
            userPromptBuilder.append("Documentation technique pertinente:\n")
                           .append(buildKnowledgeContext(knowledgeDocuments)).append("\n\n");
        }
        
        userPromptBuilder.append("Analyse la situation et propose:\n")
                         .append("- CAUSES PROBABLES: Identifie 2-4 causes techniques possibles basées sur les symptômes\n")
                         .append("- ACTIONS CORRECTIVES: Fournis 2-5 actions techniques précises avec références (pièces, procédures, normes)\n")
                         .append("- RÉSUMÉ: Synthèse technique du diagnostic\n")
                         .append("- CONSEILS: Recommandations de maintenance préventive\n\n")
                         .append("Concentre-toi sur des actions correctives concrètes et réalisables par un technicien qualifié.");
        
        String userPrompt = userPromptBuilder.toString();

        long llmStart = System.nanoTime();
        try {
            ragRetrievalMetrics.recordLlmCall();
            String response = llmProvider.complete(SYSTEM_PROMPT, userPrompt);
            return new SuggestionResult(parseSuggestions(response), elapsedMs(llmStart));
        } catch (Exception e) {
            log.error("Erreur génération LLM: {}", e.getMessage());
            return new SuggestionResult(fallbackSuggestions(similar, knowledgeDocuments), elapsedMs(llmStart));
        }
    }

    private AiSuggestions fallbackSuggestions(List<SimilarIntervention> similar, List<SimilarKnowledgeDocument> knowledgeDocuments) {
        List<String> causes = new ArrayList<>();
        List<String> actions = new ArrayList<>();
        
        // Extract and split causes from interventions
        similar.stream()
                .map(SimilarIntervention::causeRacine)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .limit(3)
                .forEach(cause -> {
                    // Split multi-sentence causes
                    String[] parts = cause.split("[.;]");
                    for (String part : parts) {
                        String cleaned = part.trim();
                        if (cleaned.length() > 15) {
                            causes.add(cleaned);
                            if (causes.size() >= 3) break;
                        }
                    }
                });
        
        // Extract and enhance actions from interventions
        similar.stream()
                .map(SimilarIntervention::actionsCorrectives)
                .filter(a -> a != null && !a.isBlank())
                .distinct()
                .limit(4)
                .forEach(actionText -> {
                    // Split multi-sentence actions
                    String[] parts = actionText.split("[.;,]");
                    for (String part : parts) {
                        String cleaned = part.trim();
                        if (cleaned.length() > 20 && !cleaned.toLowerCase().startsWith("remplacer") 
                            && !actions.stream().anyMatch(existing -> existing.contains(cleaned.substring(0, Math.min(cleaned.length(), 30))))) {
                            // Enhance with technical precision if generic
                            String finalAction = cleaned;
                            if (cleaned.toLowerCase().contains("vérifier") && !cleaned.contains("avec")) {
                                finalAction = cleaned + " avec instruments de mesure appropriés";
                            }
                            actions.add(finalAction);
                            if (actions.size() >= 4) break;
                        }
                    }
                });
        
        // Add technical guidance from knowledge documents
        if (!knowledgeDocuments.isEmpty()) {
            boolean hasManual = knowledgeDocuments.stream().anyMatch(d -> "manual".equals(d.documentType()));
            boolean hasProcedure = knowledgeDocuments.stream().anyMatch(d -> "procedure".equals(d.documentType()));
            
            if (hasManual && causes.size() < 3) {
                causes.add("Consulter manuel constructeur pour diagnostic détaillé");
            }
            if (hasProcedure && actions.size() < 3) {
                actions.add("Suivre procédure maintenance préventive selon documentation technique");
            }
        }
        
        // Ensure minimum robust fallback actions
        if (actions.isEmpty()) {
            actions.addAll(List.of(
                "Effectuer inspection visuelle complète de l'équipement",
                "Vérifier alimentations électriques et connexions",
                "Contrôler paramètres de fonctionnement nominal"
            ));
        } else if (actions.size() == 1) {
            actions.add("Documenter l'intervention pour enrichir la base de connaissances");
        }
        
        // Ensure minimum causes
        if (causes.isEmpty()) {
            causes.addAll(List.of(
                "Défaillance composant ou usure normale",
                "Paramètres de fonctionnement hors tolérances"
            ));
        }
        
        String summary = similar.isEmpty() && knowledgeDocuments.isEmpty()
                ? "Diagnostic basé sur procédures générales de maintenance industrielle"
                : String.format("Diagnostic basé sur %d intervention(s) similaire(s) et %d document(s) technique(s)",
                        similar.size(), knowledgeDocuments.size());
        
        String advice = similar.isEmpty() && knowledgeDocuments.isEmpty() 
                ? "Consulter un expert technique ou le responsable EIA pour cas complexes non documentés"
                : "Valider diagnostic avec interventions similaires avant action corrective";
        
        return new AiSuggestions(causes, actions, summary, advice);
    }

    private String buildInterventionContext(List<SimilarIntervention> similar) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < similar.size(); i++) {
            SimilarIntervention row = similar.get(i);
            sb.append("--- Intervention ").append(i + 1).append(" (équipement: ").append(row.equipmentCode())
                    .append(", similarité: ").append(String.format("%.2f", row.similarity())).append(") ---\n");
            if (row.symptomes() != null) sb.append("Symptômes: ").append(truncateField(row.symptomes())).append("\n");
            if (row.causeRacine() != null) sb.append("Cause: ").append(truncateField(row.causeRacine())).append("\n");
            if (row.actionsCorrectives() != null) sb.append("Actions: ").append(truncateField(row.actionsCorrectives())).append("\n");
            // Note: analyseTechnique supprimée du prompt LLM pour réduire la taille (gardée en base pour recherche)
        }
        return sb.toString();
    }

    /**
     * Build context from knowledge documents for LLM prompt
     */
    private String buildKnowledgeContext(List<SimilarKnowledgeDocument> knowledgeDocuments) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < knowledgeDocuments.size(); i++) {
            SimilarKnowledgeDocument doc = knowledgeDocuments.get(i);
            sb.append("--- Document ").append(i + 1).append(" (").append(doc.documentType())
                    .append(", pertinence: ").append(String.format("%.2f", doc.similarity())).append(") ---\n");
            sb.append("Source: ").append(doc.source()).append("\n");
            if (doc.equipmentFamily() != null) {
                sb.append("Famille: ").append(doc.equipmentFamily()).append("\n");
            }
            sb.append("Contenu: ").append(truncateField(doc.contentExcerpt())).append("\n");
        }
        return sb.toString();
    }

    /**
     * Tronque un champ texte à 250 caractères maximum pour optimiser le contexte LLM
     */
    private String truncateField(String field) {
        if (field == null || field.length() <= 250) {
            return field;
        }
        return field.substring(0, 247) + "...";
    }

    private AiSuggestions parseSuggestions(String response) throws JsonProcessingException {
        String json = extractJson(response);
        var node = objectMapper.readTree(json);
        
        List<String> probableCauses = toStringList(node.get("probableCauses"));
        List<String> correctiveActions = toStringList(node.get("correctiveActions"));
        String summary = node.path("summary").asText("");
        String advice = node.path("advice").asText("");
        
        // Validation critique : ne jamais retourner d'actions vides
        if (correctiveActions.isEmpty()) {
            log.warn("LLM a généré des correctiveActions vides, application du fallback technique");
            correctiveActions = List.of(
                "Effectuer diagnostic approfondi avec documentation technique",
                "Vérifier paramètres de fonctionnement et conformité aux spécifications",
                "Consulter responsable EIA pour intervention spécialisée"
            );
        }
        
        // Validation causes (minimum)
        if (probableCauses.isEmpty()) {
            log.warn("LLM a généré des probableCauses vides, application du fallback");
            probableCauses = List.of("Analyse technique requise pour diagnostic précis");
        }
        
        // Validation résumé
        if (summary.isBlank()) {
            summary = "Diagnostic technique basé sur analyse des données disponibles";
        }
        
        // Validation conseil
        if (advice.isBlank()) {
            advice = "Valider diagnostic avant intervention et documenter l'action corrective";
        }
        
        return new AiSuggestions(probableCauses, correctiveActions, summary, advice);
    }

    private List<String> toStringList(com.fasterxml.jackson.databind.JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(n -> list.add(n.asText()));
        }
        return list;
    }

    private String extractJson(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }

    /**
     * Fusionne les résultats de recherche texte et vectorielle sur les documents de connaissance
     */
    private List<SimilarKnowledgeDocument> mergeKnowledgeResults(
            List<SimilarKnowledgeDocument> textResults,
            List<SimilarKnowledgeDocument> vectorResults,
            int limit) {
        
        // Map par ID de document pour éviter les doublons
        Map<UUID, SimilarKnowledgeDocument> mergedResults = new HashMap<>();
        
        // Ajouter les résultats texte
        for (SimilarKnowledgeDocument doc : textResults) {
            mergedResults.put(doc.documentId(), doc);
        }
        
        // Ajouter/fusionner les résultats vectoriels (garder le meilleur score)
        for (SimilarKnowledgeDocument doc : vectorResults) {
            SimilarKnowledgeDocument existing = mergedResults.get(doc.documentId());
            if (existing == null || doc.similarity() > existing.similarity()) {
                mergedResults.put(doc.documentId(), doc);
            }
        }
        
        // Trier par similarité et limiter
        return mergedResults.values().stream()
                .sorted((a, b) -> Double.compare(b.similarity(), a.similarity()))
                .limit(limit)
                .toList();
    }

    /**
     * Construit le contexte de recherche à partir de la requête
     */
    private SearchContext buildSearchContext(AiAssistRequest request) {
        try {
            // Si on a un equipmentId, récupérer les informations d'équipement
            if (request.equipmentId() != null) {
                Equipment equipment = equipmentRepository.findById(request.equipmentId()).orElse(null);
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
                Failure failure = failureRepository.findByIdWithDetails(request.failureId()).orElse(null);
                if (failure != null && failure.getEquipment() != null) {
                    Equipment equipment = failure.getEquipment();
                    
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

    private static long elapsedMs(long startNano) {
        return (System.nanoTime() - startNano) / 1_000_000L;
    }

    private record SuggestionResult(AiSuggestions suggestions, long llmDurationMs) {}
}
