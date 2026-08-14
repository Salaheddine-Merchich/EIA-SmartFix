package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.modules.knowledge.domain.model.QuerySignals;
import com.ocp.eia.modules.knowledge.domain.model.SimilarIntervention;
import com.ocp.eia.modules.knowledge.domain.model.SimilarKnowledgeDocument;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagEvidencePolicyTest {

    @Test
    void hasProjectEvidence_withIntervention_isTrue() {
        SimilarIntervention intervention = new SimilarIntervention(
                UUID.randomUUID(), "POM-PV", "Symptomes", "Cause", "Actions", "Analyse", 0.82);

        assertTrue(RagEvidencePolicy.hasProjectEvidence(
                List.of(intervention), List.of(), QuerySignals.empty(), 0.70));
    }

    @Test
    void hasProjectEvidence_weakDocumentsWithoutIntervention_isFalse() {
        SimilarKnowledgeDocument weakDoc = document(0.41);

        assertFalse(RagEvidencePolicy.hasProjectEvidence(
                List.of(), List.of(weakDoc), QuerySignals.empty(), 0.70));
    }

    @Test
    void hasProjectEvidence_strongDocument_isTrue() {
        SimilarKnowledgeDocument strongDoc = document(0.81);

        assertTrue(RagEvidencePolicy.hasProjectEvidence(
                List.of(), List.of(strongDoc), QuerySignals.empty(), 0.70));
    }

    @Test
    void hasProjectEvidence_faultCodeWithoutHits_isTrue() {
        QuerySignals signals = new QuerySignals(
                List.of("E21"),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                List.of(),
                List.of()
        );

        assertTrue(RagEvidencePolicy.hasProjectEvidence(List.of(), List.of(), signals, 0.70));
    }

    private static SimilarKnowledgeDocument document(double similarity) {
        return new SimilarKnowledgeDocument(
                UUID.randomUUID(),
                "Manuel",
                "Extrait",
                "manual",
                "Variateur",
                "seed",
                similarity
        );
    }
}
