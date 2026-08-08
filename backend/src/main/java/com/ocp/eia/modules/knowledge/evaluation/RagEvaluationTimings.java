package com.ocp.eia.modules.knowledge.evaluation;

/**
 * Latences mesurées par étape du pipeline RAG (millisecondes).
 */
public record RagEvaluationTimings(
        long embeddingMs,
        long vectorSearchMs,
        long textSearchMs,
        long mergeMs,
        long llmMs,
        long totalMs
) {
    public static RagEvaluationTimings zero() {
        return new RagEvaluationTimings(0, 0, 0, 0, 0, 0);
    }
}
