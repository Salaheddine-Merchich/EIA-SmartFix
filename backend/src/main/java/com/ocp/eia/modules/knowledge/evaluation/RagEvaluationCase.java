package com.ocp.eia.modules.knowledge.evaluation;

import java.util.UUID;

/**
 * Scénario d'évaluation : une question métier et l'intervention attendue (ground truth).
 */
public record RagEvaluationCase(
        String caseId,
        String question,
        UUID expectedInterventionId,
        String description
) {
    public RagEvaluationCase {
        if (caseId == null || caseId.isBlank()) {
            throw new IllegalArgumentException("caseId requis");
        }
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question requise");
        }
    }
}
