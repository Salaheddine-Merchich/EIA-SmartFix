package com.ocp.eia.modules.knowledge.evaluation;

import java.util.List;

/**
 * Rapport agrégé d'une campagne d'évaluation RAG.
 */
public record RagEvaluationReport(
        int questionCount,
        int successfulRuns,
        double successRatePercent,
        double precisionAt1Percent,
        double precisionAt3Percent,
        double precisionAt5Percent,
        double recallAt5Percent,
        double meanReciprocalRank,
        double avgSimilarityScore,
        double avgTotalLatencyMs,
        double avgEmbeddingMs,
        double avgVectorSearchMs,
        double avgTextSearchMs,
        double avgMergeMs,
        double avgLlmMs,
        double avgMergedCount,
        double avgFilteredCount,
        List<RagEvaluationResult> caseResults
) {
    public static RagEvaluationReport fromResults(List<RagEvaluationResult> results) {
        int count = results.size();
        long successes = results.stream().filter(RagEvaluationResult::success).count();

        return new RagEvaluationReport(
                count,
                (int) successes,
                RagEvaluationMetricsCalculator.successRate(results),
                RagEvaluationMetricsCalculator.hitRateAt(results, 1),
                RagEvaluationMetricsCalculator.hitRateAt(results, 3),
                RagEvaluationMetricsCalculator.hitRateAt(results, 5),
                RagEvaluationMetricsCalculator.hitRateAt(results, 5),
                RagEvaluationMetricsCalculator.meanReciprocalRank(results),
                RagEvaluationMetricsCalculator.averageSimilarity(results),
                RagEvaluationMetricsCalculator.averageTiming(results, RagEvaluationTimings::totalMs),
                RagEvaluationMetricsCalculator.averageTiming(results, RagEvaluationTimings::embeddingMs),
                RagEvaluationMetricsCalculator.averageTiming(results, RagEvaluationTimings::vectorSearchMs),
                RagEvaluationMetricsCalculator.averageTiming(results, RagEvaluationTimings::textSearchMs),
                RagEvaluationMetricsCalculator.averageTiming(results, RagEvaluationTimings::mergeMs),
                RagEvaluationMetricsCalculator.averageTiming(results, RagEvaluationTimings::llmMs),
                results.stream().filter(RagEvaluationResult::success).mapToInt(RagEvaluationResult::mergedCount).average().orElse(0),
                results.stream().filter(RagEvaluationResult::success).mapToInt(RagEvaluationResult::filteredCount).average().orElse(0),
                List.copyOf(results)
        );
    }

    public String toTextReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("====================\n");
        sb.append("Evaluation RAG\n");
        sb.append("====================\n\n");
        sb.append("Questions              : ").append(questionCount).append('\n');
        sb.append("Exécutions réussies    : ").append(successfulRuns).append('\n');
        sb.append("Taux de succès         : ").append(formatPercent(successRatePercent)).append('\n');
        sb.append("Top1 Accuracy          : ").append(formatPercent(precisionAt1Percent)).append('\n');
        sb.append("Top3 Accuracy          : ").append(formatPercent(precisionAt3Percent)).append('\n');
        sb.append("Top5 Accuracy          : ").append(formatPercent(precisionAt5Percent)).append('\n');
        sb.append("Recall@5               : ").append(formatPercent(recallAt5Percent)).append('\n');
        sb.append("MRR                    : ").append(String.format("%.3f", meanReciprocalRank)).append('\n');
        sb.append("Score similarité moy.  : ").append(String.format("%.2f", avgSimilarityScore)).append('\n');
        sb.append('\n');
        sb.append("Latence moyenne        : ").append(formatMs(avgTotalLatencyMs)).append('\n');
        sb.append("  Embedding            : ").append(formatMs(avgEmbeddingMs)).append('\n');
        sb.append("  Vector Search        : ").append(formatMs(avgVectorSearchMs)).append('\n');
        sb.append("  Text Search          : ").append(formatMs(avgTextSearchMs)).append('\n');
        sb.append("  Fusion               : ").append(formatMs(avgMergeMs)).append('\n');
        sb.append("  LLM                  : ").append(formatMs(avgLlmMs)).append('\n');
        sb.append('\n');
        sb.append("Résultats fusionnés moy.: ").append(String.format("%.1f", avgMergedCount)).append('\n');
        sb.append("Résultats filtrés moy.  : ").append(String.format("%.1f", avgFilteredCount)).append('\n');
        sb.append("====================\n");
        return sb.toString();
    }

    private static String formatPercent(double value) {
        return String.format("%.0f %%", value);
    }

    private static String formatMs(double value) {
        return String.format("%.0f ms", value);
    }
}
