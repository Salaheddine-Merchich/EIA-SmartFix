package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.modules.knowledge.domain.model.SimilarIntervention;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class HybridRetrievalMergerTest {

    @Test
    void merge_vectorOnly_returnsVectorResults() {
        UUID id = UUID.randomUUID();
        SimilarIntervention vector = row(id, "EQ-V", 0.92);

        List<SimilarIntervention> merged = HybridRetrievalMerger.merge(List.of(vector), List.of(), 5);

        assertEquals(1, merged.size());
        assertEquals(id, merged.get(0).interventionId());
        assertEquals(0.92, merged.get(0).similarity());
    }

    @Test
    void merge_textOnly_returnsTextResults() {
        UUID id = UUID.randomUUID();
        SimilarIntervention text = row(id, "EQ-T", 0.80);

        List<SimilarIntervention> merged = HybridRetrievalMerger.merge(List.of(), List.of(text), 5);

        assertEquals(1, merged.size());
        assertEquals("EQ-T", merged.get(0).equipmentCode());
    }

    @Test
    void merge_duplicate_keepsHighestRawSimilarityForThreshold() {
        UUID id = UUID.randomUUID();
        SimilarIntervention vector = row(id, "EQ-001", 0.85);
        SimilarIntervention text = row(id, "EQ-001", 0.95);

        List<SimilarIntervention> merged = HybridRetrievalMerger.merge(List.of(vector), List.of(text), 5);

        assertEquals(1, merged.size());
        assertEquals(0.95, merged.get(0).similarity());
    }

    @Test
    void merge_distinctResults_tieBreaksByRawSimilarity() {
        UUID idVector = UUID.randomUUID();
        UUID idText = UUID.randomUUID();
        SimilarIntervention vector = row(idVector, "EQ-V", 0.88);
        SimilarIntervention text = row(idText, "EQ-T", 0.76);

        List<SimilarIntervention> merged = HybridRetrievalMerger.merge(List.of(vector), List.of(text), 5);

        assertEquals(2, merged.size());
        assertEquals(idVector, merged.get(0).interventionId());
        assertEquals(idText, merged.get(1).interventionId());
    }

    @Test
    void merge_respectsTopKLimit() {
        List<SimilarIntervention> vector = List.of(
                row(UUID.randomUUID(), "EQ-1", 0.90),
                row(UUID.randomUUID(), "EQ-2", 0.80),
                row(UUID.randomUUID(), "EQ-3", 0.70)
        );

        List<SimilarIntervention> merged = HybridRetrievalMerger.merge(vector, List.of(), 2);

        assertEquals(2, merged.size());
        assertEquals(0.90, merged.get(0).similarity());
        assertEquals(0.80, merged.get(1).similarity());
    }

    @Test
    void merge_rrf_boostsDocumentPresentInBothLists() {
        UUID idBoth = UUID.randomUUID();
        UUID idVectorOnly = UUID.randomUUID();
        SimilarIntervention vectorOnly = row(idVectorOnly, "EQ-V", 0.95);
        SimilarIntervention bothVector = row(idBoth, "EQ-B", 0.82);
        SimilarIntervention bothText = row(idBoth, "EQ-B", 0.78);

        List<SimilarIntervention> merged = HybridRetrievalMerger.merge(
                List.of(vectorOnly, bothVector),
                List.of(bothText),
                5);

        assertEquals(idBoth, merged.get(0).interventionId(),
                "Un document présent dans les deux listes doit être boosté par RRF");
        assertEquals(0.82, merged.get(0).similarity(),
                "Le score brut conservé pour le seuil est le max des deux sources");
    }

    @Test
    void merge_rrfFormula_usesConfiguredK() {
        UUID id = UUID.randomUUID();
        SimilarIntervention vector = row(id, "EQ-1", 0.90);

        List<SimilarIntervention> merged = HybridRetrievalMerger.merge(List.of(vector), List.of(), 5, 60);

        assertEquals(1, merged.size());
        assertEquals(id, merged.get(0).interventionId());
    }

    private static SimilarIntervention row(UUID id, String equipmentCode, double similarity) {
        return new SimilarIntervention(id, equipmentCode, "S", "C", "A", null, similarity);
    }
}
