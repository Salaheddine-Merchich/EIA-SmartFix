package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.modules.knowledge.domain.model.QuerySignals;
import com.ocp.eia.modules.knowledge.domain.model.SimilarIntervention;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FaultCodeInterventionFilterTest {

    @Test
    void apply_withExtractedCode_keepsOnlyMatchingInterventions() {
        UUID e21Id = UUID.randomUUID();
        UUID e35Id = UUID.randomUUID();
        SimilarIntervention e21 = row(e21Id, "E21", "Nettoyer radiateur");
        SimilarIntervention e35 = row(e35Id, "E35", "Verifier sonde 6/L");
        QuerySignals signals = new QuerySignals(
                List.of("E21"), Optional.of("Hitachi"), Optional.empty(),
                Optional.empty(), Optional.empty(), List.of(), List.of());

        List<SimilarIntervention> filtered = FaultCodeInterventionFilter.apply(
                List.of(e35, e21), signals, List.of(e21));

        assertEquals(1, filtered.size());
        assertEquals(e21Id, filtered.get(0).interventionId());
    }

    @Test
    void apply_withoutExtractedCode_returnsAll() {
        SimilarIntervention a = row(UUID.randomUUID(), "E05", "cause");
        List<SimilarIntervention> filtered = FaultCodeInterventionFilter.apply(
                List.of(a), QuerySignals.empty(), List.of());

        assertEquals(1, filtered.size());
    }

    @Test
    void apply_noMatchInList_fallsBackToExactMatches() {
        UUID e21Id = UUID.randomUUID();
        SimilarIntervention e21 = row(e21Id, "E21", "Nettoyer radiateur");
        SimilarIntervention e35 = row(UUID.randomUUID(), "E35", "Sonde");
        QuerySignals signals = new QuerySignals(
                List.of("E21"), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), List.of(), List.of());

        List<SimilarIntervention> filtered = FaultCodeInterventionFilter.apply(
                List.of(e35), signals, List.of(e21));

        assertEquals(1, filtered.size());
        assertEquals(e21Id, filtered.get(0).interventionId());
    }

    private static SimilarIntervention row(UUID id, String faultCode, String cause) {
        return new SimilarIntervention(
                id, "EQ-1", "Symptômes", cause, "Actions", "Analyse",
                0.9, faultCode, "Hitachi"
        );
    }
}
