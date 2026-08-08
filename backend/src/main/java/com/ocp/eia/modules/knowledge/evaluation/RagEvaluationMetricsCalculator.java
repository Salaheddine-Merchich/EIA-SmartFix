package com.ocp.eia.modules.knowledge.evaluation;

import java.util.List;

/**
 * Calcul des métriques agrégées IR (Precision@K, MRR, Recall@K).
 * Pour une seule intervention pertinente par question, Hit@K = Precision@K = Recall@K.
 */
final class RagEvaluationMetricsCalculator {

    private RagEvaluationMetricsCalculator() {
    }

    static int rankOfExpected(List<java.util.UUID> retrievedIds, java.util.UUID expectedId) {
        if (expectedId == null) {
            return 0;
        }
        for (int i = 0; i < retrievedIds.size(); i++) {
            if (expectedId.equals(retrievedIds.get(i))) {
                return i + 1;
            }
        }
        return 0;
    }

    static double reciprocalRank(int rank) {
        return rank > 0 ? 1.0 / rank : 0.0;
    }

    static boolean hitAt(List<java.util.UUID> retrievedIds, java.util.UUID expectedId, int k) {
        if (expectedId == null) {
            return false;
        }
        int limit = Math.min(k, retrievedIds.size());
        for (int i = 0; i < limit; i++) {
            if (expectedId.equals(retrievedIds.get(i))) {
                return true;
            }
        }
        return false;
    }

    static double averageSimilarity(List<RagEvaluationResult> results) {
        return results.stream()
                .filter(RagEvaluationResult::success)
                .mapToDouble(RagEvaluationResult::avgSimilarityScore)
                .average()
                .orElse(0.0);
    }

    static double hitRateAt(List<RagEvaluationResult> results, int k) {
        long scorable = results.stream()
                .filter(r -> r.evalCase().expectedInterventionId() != null)
                .count();
        if (scorable == 0) {
            return 0.0;
        }
        long hits = results.stream()
                .filter(r -> r.evalCase().expectedInterventionId() != null)
                .filter(r -> switch (k) {
                    case 1 -> r.hitAt1();
                    case 3 -> r.hitAt3();
                    case 5 -> r.hitAt5();
                    default -> hitAt(r.retrievedIds(), r.evalCase().expectedInterventionId(), k);
                })
                .count();
        return 100.0 * hits / scorable;
    }

    static double meanReciprocalRank(List<RagEvaluationResult> results) {
        return results.stream()
                .filter(r -> r.evalCase().expectedInterventionId() != null)
                .mapToDouble(RagEvaluationResult::reciprocalRank)
                .average()
                .orElse(0.0);
    }

    static double averageTiming(List<RagEvaluationResult> results,
                                java.util.function.ToLongFunction<RagEvaluationTimings> extractor) {
        return results.stream()
                .filter(RagEvaluationResult::success)
                .mapToLong(r -> extractor.applyAsLong(r.timings()))
                .average()
                .orElse(0.0);
    }

    static double successRate(List<RagEvaluationResult> results) {
        if (results.isEmpty()) {
            return 0.0;
        }
        long successes = results.stream().filter(RagEvaluationResult::success).count();
        return 100.0 * successes / results.size();
    }
}
