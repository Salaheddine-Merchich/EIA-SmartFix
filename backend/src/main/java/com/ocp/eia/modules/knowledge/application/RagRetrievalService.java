package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.application.dto.AiDto.AiAssistRequest;
import com.ocp.eia.config.AppProperties;
import com.ocp.eia.modules.knowledge.domain.model.QuerySignals;
import com.ocp.eia.modules.knowledge.domain.model.SearchContext;
import com.ocp.eia.modules.knowledge.domain.model.SimilarIntervention;
import com.ocp.eia.modules.knowledge.domain.model.SimilarKnowledgeDocument;
import com.ocp.eia.modules.knowledge.domain.port.EmbeddingProviderPort;
import com.ocp.eia.modules.knowledge.domain.port.ExactFaultCodeSearchPort;
import com.ocp.eia.modules.knowledge.domain.port.InterventionTextSearchPort;
import com.ocp.eia.modules.knowledge.domain.port.KnowledgeDocumentSearchPort;
import com.ocp.eia.modules.knowledge.domain.port.VectorStorePort;
import com.ocp.eia.modules.knowledge.infrastructure.observability.RagRetrievalMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private final ExactFaultCodeSearchPort exactFaultCodeSearchPort;
    private final AppProperties appProperties;
    private final SearchContextFactory searchContextFactory;
    private final RagRetrievalMetrics ragRetrievalMetrics;

    @Qualifier("ragExecutor")
    private final Executor ragExecutor;

    public RetrievalOutcome retrieve(AiAssistRequest request) {
        int topK = request.topK() != null ? request.topK() : appProperties.getAi().getRag().getTopK();
        boolean hybridEnabled = appProperties.getAi().getRag().isHybridTextEnabled();
        long retrievalStart = System.nanoTime();
        int queryLength = request.description() != null ? request.description().length() : 0;
        QuerySignals querySignals = QuerySignalExtractor.extract(request.description());
        log.info("RAG retrieve start: queryLength={}, topK={}, hybrid={}, faultCodes={}",
                queryLength, topK, hybridEnabled, querySignals.faultCodes());

        if (shouldReturnCodeNotFound(querySignals)) {
            String unknownCode = querySignals.primaryFaultCode();
            log.info("RAG code inconnu détecté: {}", unknownCode);
            return RetrievalOutcome.codeNotFound(
                    unknownCode,
                    hybridEnabled,
                    elapsedMs(retrievalStart)
            );
        }

        List<SimilarIntervention> exactMatches = findExactMatches(querySignals, topK);
        SearchContext searchContext = searchContextFactory.from(request, querySignals);
        log.debug("Contexte de recherche: equipmentId={}, failureId={}, family={}, zone={}, manufacturer={}",
                searchContext.equipmentId(), searchContext.failureId(),
                searchContext.equipmentFamily(), searchContext.equipmentZone(),
                searchContext.manufacturer());

        float[] queryEmbedding;
        String embeddingStatus;
        try {
            queryEmbedding = embeddingProvider.embed(request.description());
            embeddingStatus = "OK";
        } catch (Exception e) {
            log.error("Erreur embedding RAG (queryLength={}): {}", queryLength, e.getMessage());
            return RetrievalOutcome.unavailable(
                    hybridEnabled,
                    elapsedMs(retrievalStart),
                    "FAILED",
                    "Embedding indisponible"
            );
        }

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
                        List<SimilarIntervention> standard = interventionTextSearchPort.searchValidated(
                                request.description(), topK, searchContext, querySignals);
                        int semanticTopK = Math.max(topK, 8);
                        List<SimilarIntervention> semantic = interventionTextSearchPort.searchBySemanticContext(
                                querySignals, searchContext, semanticTopK);
                        return mergeInterventionLists(standard, semantic);
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

        List<SimilarIntervention> similar = RetrievalReranker.rerank(
                unifiedResults.interventions(),
                exactMatches,
                querySignals,
                searchContext,
                appProperties.getAi().getRag().getExactCodeBoost(),
                symptomBoost(),
                zoneMismatchPenalty()
        );
        if (similar.size() > topK) {
            similar = similar.subList(0, topK);
        }

        List<SimilarKnowledgeDocument> knowledgeResults = unifiedResults.knowledgeDocuments();

        ragRetrievalMetrics.recordMergedCount(similar.size());
        log.debug("RAG unified: {} interventions vectorielles, {} interventions texte, {} exactes, {} docs → {} finales",
                vectorResults.size(), textResults.size(), exactMatches.size(),
                knowledgeResults.size(), similar.size());

        double similarityThreshold = appProperties.getAi().getRag().getSimilarityThreshold();
        List<SimilarIntervention> thresholdFiltered = similar.stream()
                .filter(s -> passesSimilarityThreshold(s, querySignals, similarityThreshold))
                .toList();
        List<SimilarIntervention> relevant = FaultCodeInterventionFilter.apply(
                thresholdFiltered, querySignals, exactMatches);
        relevant = SemanticContextFilter.apply(relevant, querySignals);
        if (querySignals.hasSemanticContext() && !querySignals.hasFaultCodes()) {
            relevant = ensureSemanticBreadth(relevant, querySignals, searchContext, similarityThreshold, 3);
            relevant = sortBySimilarity(relevant).stream().limit(3).toList();
        }
        ragRetrievalMetrics.recordFilteredCount(relevant.size());

        if (querySignals.hasFaultCodes() && thresholdFiltered.size() != relevant.size()) {
            log.info("RAG filtre code défaut: {} → {} interventions (codes={})",
                    thresholdFiltered.size(), relevant.size(), querySignals.faultCodes());
        }

        long retrievalDurationMs = elapsedMs(retrievalStart);
        log.info(
                "RAG retrieve done: durationMs={}, vector={}, text={}, exact={}, merged={}, relevant={}, knowledgeDocs={}, embeddingStatus={}",
                retrievalDurationMs,
                vectorResults.size(),
                textResults.size(),
                exactMatches.size(),
                similar.size(),
                relevant.size(),
                knowledgeResults.size(),
                embeddingStatus
        );

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
                retrievalDurationMs,
                false,
                null,
                false,
                null
        );
    }

    private boolean shouldReturnCodeNotFound(QuerySignals querySignals) {
        if (!appProperties.getAi().getRag().isCodeNotFoundEnabled() || !querySignals.hasFaultCodes()) {
            return false;
        }
        String primaryCode = querySignals.primaryFaultCode();
        return primaryCode != null && !exactFaultCodeSearchPort.existsFaultCode(primaryCode);
    }

    private List<SimilarIntervention> findExactMatches(QuerySignals querySignals, int topK) {
        if (!querySignals.hasFaultCodes()) {
            return List.of();
        }

        Map<UUID, SimilarIntervention> unique = new LinkedHashMap<>();
        for (String code : querySignals.faultCodes()) {
            List<SimilarIntervention> matches = exactFaultCodeSearchPort.searchByExactCode(
                    code, querySignals.manufacturer(), topK);
            for (SimilarIntervention match : matches) {
                unique.putIfAbsent(match.interventionId(), match);
            }
        }
        return new ArrayList<>(unique.values());
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

    private static List<SimilarIntervention> mergeInterventionLists(
            List<SimilarIntervention> first,
            List<SimilarIntervention> second
    ) {
        Map<UUID, SimilarIntervention> merged = new LinkedHashMap<>();
        for (SimilarIntervention item : first) {
            merged.put(item.interventionId(), item);
        }
        for (SimilarIntervention item : second) {
            merged.merge(item.interventionId(), item, (a, b) ->
                    a.similarity() >= b.similarity() ? a : b);
        }
        return List.copyOf(merged.values());
    }

    private List<SimilarIntervention> ensureSemanticBreadth(
            List<SimilarIntervention> current,
            QuerySignals signals,
            SearchContext context,
            double similarityThreshold,
            int minCount
    ) {
        if (current.size() >= minCount) {
            return current;
        }

        List<SimilarIntervention> supplemental = interventionTextSearchPort.searchBySemanticContext(
                signals, context, Math.max(minCount * 3, 8));
        List<SimilarIntervention> merged = mergeInterventionLists(current, supplemental);
        List<SimilarIntervention> filtered = merged.stream()
                .filter(item -> passesSimilarityThreshold(item, signals, similarityThreshold))
                .toList();
        filtered = SemanticContextFilter.apply(filtered, signals);
        if (filtered.size() >= minCount) {
            return filtered;
        }
        return filtered.isEmpty() ? current : filtered;
    }

    private static List<SimilarIntervention> sortBySimilarity(List<SimilarIntervention> interventions) {
        return interventions.stream()
                .sorted(Comparator.comparingDouble(SimilarIntervention::similarity).reversed())
                .toList();
    }

    private boolean passesSimilarityThreshold(
            SimilarIntervention item,
            QuerySignals signals,
            double defaultThreshold
    ) {
        if (item.similarity() >= defaultThreshold) {
            return true;
        }
        if (!signals.hasSemanticContext()
                || signals.symptomKeywords() == null
                || signals.symptomKeywords().isEmpty()) {
            return false;
        }
        String combined = String.join(" ",
                item.symptomes(),
                item.causeRacine(),
                item.faultCode());
        int overlap = SymptomQueryExpander.countSymptomOverlap(combined, signals.symptomKeywords());
        double relaxedThreshold = Math.min(defaultThreshold, 0.55);
        return overlap >= 1 && item.similarity() >= relaxedThreshold;
    }

    private double symptomBoost() {
        return appProperties.getAi().getRag().getContext() != null
                ? appProperties.getAi().getRag().getContext().getSymptomBoost() : 1.5;
    }

    private double zoneMismatchPenalty() {
        return appProperties.getAi().getRag().getContext() != null
                ? appProperties.getAi().getRag().getContext().getZoneMismatchPenalty() : 0.6;
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
            String unavailableReason,
            boolean codeNotFound,
            String unknownFaultCode
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
                    reason,
                    false,
                    null
            );
        }

        public static RetrievalOutcome codeNotFound(
                String unknownFaultCode,
                boolean hybridEnabled,
                long retrievalDurationMs
        ) {
            return new RetrievalOutcome(
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    0,
                    0,
                    0,
                    "OK",
                    hybridEnabled,
                    retrievalDurationMs,
                    false,
                    null,
                    true,
                    unknownFaultCode
            );
        }
    }
}
