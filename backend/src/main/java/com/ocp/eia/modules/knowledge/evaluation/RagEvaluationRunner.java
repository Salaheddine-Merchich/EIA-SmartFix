package com.ocp.eia.modules.knowledge.evaluation;

import com.ocp.eia.config.AppProperties;
import com.ocp.eia.modules.knowledge.application.HybridRetrievalMerger;
import com.ocp.eia.modules.knowledge.domain.model.SimilarIntervention;
import com.ocp.eia.modules.knowledge.domain.port.EmbeddingProviderPort;
import com.ocp.eia.modules.knowledge.domain.port.InterventionTextSearchPort;
import com.ocp.eia.modules.knowledge.domain.port.LlmProviderPort;
import com.ocp.eia.modules.knowledge.domain.port.VectorStorePort;

import java.util.List;
import java.util.UUID;

/**
 * Exécute le pipeline RAG existant (ports + fusion RRF) et mesure qualité et latences.
 * Module indépendant : ne modifie pas {@code RagAssistUseCase}.
 */
public class RagEvaluationRunner {

    private static final String EVAL_LLM_SYSTEM = "Réponds uniquement OK.";
    private static final String EVAL_LLM_USER_PREFIX = "Évaluation RAG — contexte:\n";

    private final EmbeddingProviderPort embeddingProvider;
    private final VectorStorePort vectorStore;
    private final InterventionTextSearchPort textSearchPort;
    private final LlmProviderPort llmProvider;
    private final AppProperties appProperties;
    private final boolean invokeLlm;

    public RagEvaluationRunner(
            EmbeddingProviderPort embeddingProvider,
            VectorStorePort vectorStore,
            InterventionTextSearchPort textSearchPort,
            LlmProviderPort llmProvider,
            AppProperties appProperties) {
        this(embeddingProvider, vectorStore, textSearchPort, llmProvider, appProperties, true);
    }

    public RagEvaluationRunner(
            EmbeddingProviderPort embeddingProvider,
            VectorStorePort vectorStore,
            InterventionTextSearchPort textSearchPort,
            LlmProviderPort llmProvider,
            AppProperties appProperties,
            boolean invokeLlm) {
        this.embeddingProvider = embeddingProvider;
        this.vectorStore = vectorStore;
        this.textSearchPort = textSearchPort;
        this.llmProvider = llmProvider;
        this.appProperties = appProperties;
        this.invokeLlm = invokeLlm;
    }

    public RagEvaluationReport run(List<RagEvaluationCase> cases) {
        List<RagEvaluationResult> results = cases.stream().map(this::evaluateCase).toList();
        return RagEvaluationReport.fromResults(results);
    }

    public RagEvaluationResult evaluateCase(RagEvaluationCase evalCase) {
        long totalStart = System.nanoTime();
        int topK = appProperties.getAi().getRag().getTopK();

        try {
            long embeddingStart = System.nanoTime();
            float[] embedding = embeddingProvider.embed(evalCase.question());
            long embeddingMs = toMs(System.nanoTime() - embeddingStart);

            long vectorStart = System.nanoTime();
            List<SimilarIntervention> vectorResults = vectorStore.findSimilar(embedding, topK);
            long vectorMs = toMs(System.nanoTime() - vectorStart);

            long textMs = 0;
            List<SimilarIntervention> textResults = List.of();
            if (appProperties.getAi().getRag().isHybridTextEnabled()) {
                long textStart = System.nanoTime();
                textResults = textSearchPort.searchValidated(evalCase.question(), topK);
                textMs = toMs(System.nanoTime() - textStart);
            }

            long mergeMs = 0;
            long mergeStart = System.nanoTime();
            List<SimilarIntervention> merged = HybridRetrievalMerger.merge(vectorResults, textResults, topK);
            mergeMs = toMs(System.nanoTime() - mergeStart);

            double threshold = appProperties.getAi().getRag().getSimilarityThreshold();
            List<SimilarIntervention> filtered = merged.stream()
                    .filter(s -> s.similarity() >= threshold)
                    .toList();

            long llmMs = 0;
            if (invokeLlm && !filtered.isEmpty()) {
                long llmStart = System.nanoTime();
                llmProvider.complete(EVAL_LLM_SYSTEM, EVAL_LLM_USER_PREFIX + buildMinimalContext(filtered));
                llmMs = toMs(System.nanoTime() - llmStart);
            }

            List<UUID> retrievedIds = filtered.stream().map(SimilarIntervention::interventionId).toList();
            UUID expected = evalCase.expectedInterventionId();
            int rank = RagEvaluationMetricsCalculator.rankOfExpected(retrievedIds, expected);

            double avgSimilarity = filtered.isEmpty() ? 0.0
                    : filtered.stream().mapToDouble(SimilarIntervention::similarity).average().orElse(0.0);

            long totalMs = toMs(System.nanoTime() - totalStart);

            return new RagEvaluationResult(
                    evalCase,
                    retrievedIds,
                    filtered,
                    rank,
                    RagEvaluationMetricsCalculator.hitAt(retrievedIds, expected, 1),
                    RagEvaluationMetricsCalculator.hitAt(retrievedIds, expected, 3),
                    RagEvaluationMetricsCalculator.hitAt(retrievedIds, expected, 5),
                    RagEvaluationMetricsCalculator.reciprocalRank(rank),
                    avgSimilarity,
                    merged.size(),
                    filtered.size(),
                    new RagEvaluationTimings(embeddingMs, vectorMs, textMs, mergeMs, llmMs, totalMs),
                    true,
                    null
            );
        } catch (Exception e) {
            return RagEvaluationResult.failure(evalCase, e.getMessage());
        }
    }

    private long toMs(long nanos) {
        return nanos / 1_000_000L;
    }

    private String buildMinimalContext(List<SimilarIntervention> filtered) {
        StringBuilder sb = new StringBuilder();
        for (SimilarIntervention row : filtered) {
            sb.append("- ").append(row.interventionId())
                    .append(" (").append(row.equipmentCode()).append(")\n");
        }
        return sb.toString();
    }
}
