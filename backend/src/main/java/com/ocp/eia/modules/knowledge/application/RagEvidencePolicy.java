package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.modules.knowledge.domain.model.QuerySignals;
import com.ocp.eia.modules.knowledge.domain.model.SimilarIntervention;
import com.ocp.eia.modules.knowledge.domain.model.SimilarKnowledgeDocument;

import java.util.List;

/**
 * Determines whether retrieved data is strong enough to ground an AI diagnostic.
 */
public final class RagEvidencePolicy {

    private RagEvidencePolicy() {}

    public static boolean hasProjectEvidence(
            List<SimilarIntervention> interventions,
            List<SimilarKnowledgeDocument> documents,
            QuerySignals signals,
            double documentSimilarityThreshold
    ) {
        if (interventions != null && !interventions.isEmpty()) {
            return true;
        }
        if (signals != null && signals.hasFaultCodes()) {
            return true;
        }
        if (documents == null || documents.isEmpty()) {
            return false;
        }
        return documents.stream().anyMatch(doc -> doc.similarity() >= documentSimilarityThreshold);
    }
}
