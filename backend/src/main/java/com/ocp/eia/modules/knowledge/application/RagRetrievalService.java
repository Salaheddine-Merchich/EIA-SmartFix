package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.application.dto.AiDto.AiAssistRequest;
import com.ocp.eia.config.AppProperties;
import com.ocp.eia.modules.knowledge.domain.model.SearchContext;
import com.ocp.eia.modules.knowledge.domain.model.SimilarIntervention;
import com.ocp.eia.modules.knowledge.domain.model.SimilarKnowledgeDocument;
import com.ocp.eia.modules.knowledge.domain.port.EmbeddingProviderPort;
import com.ocp.eia.modules.knowledge.domain.port.InterventionTextSearchPort;
import com.ocp.eia.modules.knowledge.domain.port.KnowledgeDocumentSearchPort;
import com.ocp.eia.modules.knowledge.domain.port.VectorStorePort;
import com.ocp.eia.modules.knowledge.infrastructure.observability.RagRetrievalMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Component
@ConditionalOnProperty(name = "app.knowledge.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class RagRetrievalService {

    private final EmbeddingProviderPort embeddingProvider;
    private final VectorStorePort vectorStore;
    private final InterventionTextSearchPort interventionTextSearchPort;
    private final KnowledgeDocumentSearchPort knowledgeDocumentSearchPort;
    private final AppProperties appProperties;
    private final SearchContextFactory searchContextFactory;
    private final RagRetrievalMetrics ragRetrievalMetrics;

    @Qualifier("ragExecutor")
    private final Executor ragExecutor;

    public RetrievalOutcome retrieve(AiAssistRequest request) {
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
            return RetrievalOutcome.unavailable(
                    hybridEnabled,
                    elapsedMs(retrievalStart),
                    "FAILED",
                    "Embedding indisponible"
            );
        }

        SearchContext searchContext = searchContextFactory.from(request);
        log.debug("Contexte de recherche: equipmentId={}, failureId={}, family={}, zone={}",
                searchContext.equipmentId(), searchContext.failureId(),
                searchContext.equipmentFamily(), searchContext.equipmentZone());

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
                        log.warn("Erreur recherche texte RAG: {}, poursuite avec résultats vectoriels uniquement",
                                e.getMessage());
                        return List.<SimilarIntervention>of();
                    }
                }, ragExecutor)
                : CompletableFuture.completedFuture(List.of());

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
            if (e.getCause() instanceof RuntimeException
                    && e.getCause().getMessage() != null
                    && e.getCause().getMessage().contains("Vector search failed")) {
                return RetrievalOutcome.unavailable(
                        hybridEnabled,
                        elapsedMs(retrievalStart),
                        embeddingStatus,
                        "Recherche vectorielle indisponible"
                );
            }
            try {
                vectorResults = vectorFuture.get();
                textResults = textFuture.isCompletedExceptionally() ? List.of() : textFuture.get();
                knowledgeTextResults = knowledgeTextFuture.isCompletedExceptionally()
                        ? List.of() : knowledgeTextFuture.get();
                knowledgeVectorResults = knowledgeVectorFuture.isCompletedExceptionally()
                        ? List.of() : knowledgeVectorFuture.get();

                log.warn("Poursuite avec résultats partiels - Interventions: {}, Documents texte: {}, Documents vectoriels: {}",
                        vectorResults.size() + textResults.size(),
                        knowledgeTextResults.size(),
                        knowledgeVectorResults.size());
            } catch (Exception vectorEx) {
                log.error("Erreur recherche vectorielle critique: {}", vectorEx.getMessage());
                return RetrievalOutcome.unavailable(
                        hybridEnabled,
                        elapsedMs(retrievalStart),
                        embeddingStatus,
                        "Recherche vectorielle indisponible"
                );
            }
        }

        ragRetrievalMetrics.recordVectorCount(vectorResults.size());
        ragRetrievalMetrics.recordTextCount(textResults.size());

        List<SimilarKnowledgeDocument> mergedKnowledgeResults =
                mergeKnowledgeResults(knowledgeTextResults, knowledgeVectorResults, Math.min(topK, 3));
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

        return new RetrievalOutcome(
                relevant,
                knowledgeResults,
                vectorResults,
                textResults,
                vectorResults.size(),
                textResults.size(),
                similar.size(),
                embeddingStatus,
                hybridEnabled,
                elapsedMs(retrievalStart),
                false,
                null
        );
    }

    private List<SimilarKnowledgeDocument> mergeKnowledgeResults(
            List<SimilarKnowledgeDocument> textResults,
            List<SimilarKnowledgeDocument> vectorResults,
            int limit
    ) {
        Map<UUID, SimilarKnowledgeDocument> mergedResults = new HashMap<>();

        for (SimilarKnowledgeDocument doc : textResults) {
            mergedResults.put(doc.documentId(), doc);
        }

        for (SimilarKnowledgeDocument doc : vectorResults) {
            SimilarKnowledgeDocument existing = mergedResults.get(doc.documentId());
            if (existing == null || doc.similarity() > existing.similarity()) {
                mergedResults.put(doc.documentId(), doc);
            }
        }

        return mergedResults.values().stream()
                .sorted((a, b) -> Double.compare(b.similarity(), a.similarity()))
                .limit(limit)
                .toList();
    }

    private static long elapsedMs(long startNano) {
        return (System.nanoTime() - startNano) / 1_000_000L;
    }

    public record RetrievalOutcome(
            List<SimilarIntervention> relevant,
            List<SimilarKnowledgeDocument> knowledgeDocuments,
            List<SimilarIntervention> vectorResults,
            List<SimilarIntervention> textResults,
            int vectorCount,
            int textCount,
            int mergedCount,
            String embeddingStatus,
            boolean hybridEnabled,
            long retrievalDurationMs,
            boolean unavailable,
            String unavailableReason
    ) {
        public static RetrievalOutcome unavailable(
                boolean hybridEnabled,
                long retrievalDurationMs,
                String embeddingStatus,
                String reason
        ) {
            return new RetrievalOutcome(
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    0,
                    0,
                    0,
                    embeddingStatus,
                    hybridEnabled,
                    retrievalDurationMs,
                    true,
                    reason
            );
        }
    }
}
