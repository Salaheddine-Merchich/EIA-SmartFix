package com.ocp.eia.modules.knowledge.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocp.eia.modules.knowledge.domain.model.SimilarIntervention;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RagSuggestionParserTest {

    private RagSuggestionParser parser;

    @BeforeEach
    void setUp() {
        parser = new RagSuggestionParser(new ObjectMapper());
    }

    @Test
    void fallbackFromHistory_out1Actions_includesShortTechnicalSteps() {
        SimilarIntervention out1 = new SimilarIntervention(
                UUID.randomUUID(),
                "VAR-GD-100PV",
                "Code OUt1 affiché",
                "Acceleration trop rapide ou IGBT endommagé",
                "Augmenter ACC; vérifier cables CEM",
                "Analyse",
                0.95,
                "OUt1",
                "Goodrive"
        );

        var suggestions = parser.fallbackFromHistory(List.of(out1), List.of());

        String actions = String.join(" ", suggestions.correctiveActions()).toLowerCase();
        assertTrue(actions.contains("augmenter acc"), "Doit conserver 'Augmenter ACC'");
        assertTrue(actions.contains("cables cem") || actions.contains("câbles cem"),
                "Doit conserver la vérification CEM");
    }

    @Test
    void fallbackFromHistory_f14ParamRefs_preservesFullParameterCodes() {
        SimilarIntervention pvSleep = new SimilarIntervention(
                UUID.randomUUID(),
                "VAR-GD-100PV",
                "Alarme A.LPn mode sommeil",
                "Tension PV inferieure F14.11",
                "Attendre remontee > F14.12; ajuster F14.11/F14.13",
                "Analyse",
                0.92,
                "A.LPn",
                "Goodrive",
                "Variateur",
                "Station PV"
        );

        var suggestions = parser.fallbackFromHistory(List.of(pvSleep), List.of());

        assertTrue(suggestions.probableCauses().stream()
                        .anyMatch(c -> c.contains("F14.11")),
                "La cause doit conserver F14.11");
        assertTrue(suggestions.correctiveActions().stream()
                        .anyMatch(a -> a.contains("F14.11") || a.contains("F14.12")),
                "Les actions doivent conserver les références F14 complètes");
    }

    @Test
    void fallbackFromHistory_withSpecificCause_doesNotAddGenericManualCause() {
        SimilarIntervention pvSleep = new SimilarIntervention(
                UUID.randomUUID(),
                "VAR-GD-100PV",
                "Alarme A.LPn mode sommeil",
                "Tension PV inferieure F14.11",
                "Attendre remontee > F14.12",
                "Analyse",
                0.92,
                "A.LPn",
                "Goodrive",
                "Variateur",
                "Station PV"
        );
        var manualDoc = new com.ocp.eia.modules.knowledge.domain.model.SimilarKnowledgeDocument(
                UUID.randomUUID(),
                "Manuel Goodrive",
                "Contenu manuel",
                "manual",
                "Variateur",
                "Goodrive",
                0.8
        );

        var suggestions = parser.fallbackFromHistory(List.of(pvSleep), List.of(manualDoc));

        assertTrue(suggestions.probableCauses().stream()
                        .noneMatch(c -> c.toLowerCase().contains("consulter manuel constructeur")),
                "Ne doit pas ajouter une cause générique si une cause historique existe");
    }

    @Test
    void fallbackFromHistory_withoutInterventions_returnsInsufficientEvidence() {
        var suggestions = parser.fallbackFromHistory(List.of(), List.of());

        assertEquals("Cette description ne correspond à aucune donnée fiable du projet.", suggestions.summary());
        assertTrue(suggestions.correctiveActions().stream()
                .noneMatch(a -> a.toLowerCase().contains("inspection visuelle")));
    }
}
