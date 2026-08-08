package com.ocp.eia.modules.knowledge.evaluation;

import com.ocp.eia.modules.knowledge.domain.model.SimilarIntervention;

import java.util.List;
import java.util.UUID;

/**
 * Résultat d'évaluation pour un scénario unique.
 */
public record RagEvaluationResult(
        RagEvaluationCase evalCase,
        List<UUID> retrievedIds,
        List<SimilarIntervention> filteredResults,
        int rankOfExpected,
        boolean hitAt1,
        boolean hitAt3,
        boolean hitAt5,
        double reciprocalRank,
        double avgSimilarityScore,
        int mergedCount,
        int filteredCount,
        RagEvaluationTimings timings,
        boolean success,
        String errorMessage
) {
    public static RagEvaluationResult failure(RagEvaluationCase evalCase, String errorMessage) {
        return new RagEvaluationResult(
                evalCase, List.of(), List.of(), 0,
                false, false, false, 0.0, 0.0,
                0, 0, RagEvaluationTimings.zero(),
                false, errorMessage
        );
    }
}
