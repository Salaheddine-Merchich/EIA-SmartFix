package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.modules.knowledge.domain.model.QuerySignals;
import com.ocp.eia.modules.knowledge.domain.model.SimilarIntervention;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SemanticContextFilterTest {

    @Test
    void apply_excludesOtherZoneWhenMatchesExist() {
        UUID pvId = UUID.randomUUID();
        UUID convId = UUID.randomUUID();

        SimilarIntervention pv = intervention(pvId, "Station PV", "Variateur en veille 0 Hz",
                "Commande X1", "VEI-VEILLE");
        SimilarIntervention conv = intervention(convId, "Zone Convoyage", "Sens rotation inverse",
                "Phases incorrectes", "HIT-REV");

        QuerySignals signals = new QuerySignals(
                List.of(),
                Optional.empty(),
                Optional.of("pompe"),
                Optional.of("Pompe"),
                Optional.of("Station PV"),
                List.of("veille", "sommeil"),
                List.of("no_start")
        );

        List<SimilarIntervention> filtered = SemanticContextFilter.apply(
                List.of(conv, pv), signals);

        assertEquals(1, filtered.size());
        assertEquals(pvId, filtered.get(0).interventionId());
    }

    @Test
    void apply_withFaultCode_isNoOp() {
        SimilarIntervention item = intervention(UUID.randomUUID(), "Station PV", "veille",
                "Cause", "VEI-VEILLE");
        QuerySignals signals = new QuerySignals(
                List.of("E21"),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                List.of(),
                List.of()
        );

        List<SimilarIntervention> filtered = SemanticContextFilter.apply(List.of(item), signals);

        assertEquals(1, filtered.size());
    }

    @Test
    void apply_emptyFilterResult_fallsBackToOriginal() {
        SimilarIntervention onlyConv = intervention(UUID.randomUUID(), "Zone Convoyage", "rotation",
                "Cause", "HIT-REV");
        QuerySignals signals = new QuerySignals(
                List.of(),
                Optional.empty(),
                Optional.empty(),
                Optional.of("Pompe"),
                Optional.of("Station PV"),
                List.of("veille"),
                List.of("no_start")
        );

        List<SimilarIntervention> filtered = SemanticContextFilter.apply(List.of(onlyConv), signals);

        assertEquals(1, filtered.size());
    }

    @Test
    void apply_excludesConflictingAlarmePleinForNoStart() {
        UUID alarmId = UUID.randomUUID();
        UUID veilleId = UUID.randomUUID();

        SimilarIntervention alarm = intervention(alarmId, "Station PV", "Code A-tF affiche sur Goodrive 100-PV",
                "Reservoir plein", "A-tF");
        SimilarIntervention veille = intervention(veilleId, "Station PV", "Variateur reste en veille 0 Hz",
                "Commande X1 ou cablage commutateur", "VEI-VEILLE");

        QuerySignals signals = new QuerySignals(
                List.of(),
                Optional.empty(),
                Optional.of("pompe"),
                Optional.of("Pompe"),
                Optional.of("Station PV"),
                List.of("veille", "sommeil"),
                List.of("no_start")
        );

        List<SimilarIntervention> filtered = SemanticContextFilter.apply(
                List.of(alarm, veille), signals);

        assertEquals(1, filtered.size());
        assertEquals(veilleId, filtered.get(0).interventionId());
    }

    private static SimilarIntervention intervention(
            UUID id, String zone, String symptomes, String cause, String faultCode
    ) {
        return new SimilarIntervention(
                id, "POM-PV", symptomes, cause, "Actions", "Analyse",
                0.8, faultCode, "VEICHI", "Pompe", zone
        );
    }
}
