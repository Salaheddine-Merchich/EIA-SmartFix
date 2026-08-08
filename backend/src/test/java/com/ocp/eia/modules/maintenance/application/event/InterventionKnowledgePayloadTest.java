package com.ocp.eia.modules.maintenance.application.event;

import com.ocp.eia.domain.model.Criticite;
import com.ocp.eia.domain.model.Equipment;
import com.ocp.eia.domain.model.Failure;
import com.ocp.eia.domain.model.Intervention;
import com.ocp.eia.domain.model.StatutPanne;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InterventionKnowledgePayloadTest {

    @Test
    void toIndexedContent_joinsNonBlankFields() {
        UUID id = UUID.randomUUID();
        InterventionKnowledgePayload payload = new InterventionKnowledgePayload(
                id,
                "Vibrations anormales",
                "Roulement usé",
                "Analyse vibratoire",
                "Remplacement roulement",
                "Roulement SKF",
                "Panne sur convoyeur",
                null, null, null,
                null, null, null, null,
                null, null, null, null, null
        );

        String content = payload.toIndexedContent();

        assertTrue(content.contains("Symptômes: Vibrations anormales"));
        assertTrue(content.contains("Cause racine: Roulement usé"));
        assertTrue(content.contains("Actions correctives: Remplacement roulement"));
        assertFalse(content.isBlank());
    }

    @Test
    void toIndexedContent_allBlank_returnsEmpty() {
        InterventionKnowledgePayload payload = new InterventionKnowledgePayload(
                UUID.randomUUID(),
                null, "", "  ", null, null, null,
                null, null, null,
                null, null, null, null,
                null, null, null, null, null
        );

        assertTrue(payload.toIndexedContent().isBlank());
    }

    @Test
    void toIndexedContent_includesEnrichedEquipmentAndFailureFields() {
        InterventionKnowledgePayload payload = new InterventionKnowledgePayload(
                UUID.randomUUID(),
                "Surchauffe moteur",
                "Ventilation obstruée",
                null,
                "Nettoyage filtre",
                null,
                "Variateur en alarme",
                "Conforme aux procédures",
                90,
                30,
                "Arrêt en urgence du convoyeur",
                "E001",
                "HAUTE",
                "Ligne phosphates",
                "CV-101",
                "Convoyeur principal",
                "Convoyage",
                "Atelier A",
                "Siemens"
        );

        String content = payload.toIndexedContent();

        assertTrue(content.contains("Équipement: CV-101"));
        assertTrue(content.contains("Désignation équipement: Convoyeur principal"));
        assertTrue(content.contains("Famille: Convoyage"));
        assertTrue(content.contains("Zone équipement: Atelier A"));
        assertTrue(content.contains("Constructeur: Siemens"));
        assertTrue(content.contains("Description panne: Arrêt en urgence du convoyeur"));
        assertTrue(content.contains("Code défaut: E001"));
        assertTrue(content.contains("Criticité: HAUTE"));
        assertTrue(content.contains("Zone service: Ligne phosphates"));
        assertTrue(content.contains("Commentaire validation: Conforme aux procédures"));
        assertTrue(content.contains("Durée arrêt (min): 90"));
        assertTrue(content.contains("Temps intervention (min): 30"));
        // Champs historiques toujours présents
        assertTrue(content.contains("Symptômes: Surchauffe moteur"));
        assertTrue(content.contains("Cause racine: Ventilation obstruée"));
    }

    @Test
    void fromIntervention_mapsRelatedEntitiesIntoPayload() {
        Equipment equipment = Equipment.builder()
                .code("PMP-42")
                .designation("Pompe doseuse")
                .famille("Pompage")
                .zone("Zone sud")
                .constructeur("Grundfos")
                .build();
        Failure failure = Failure.builder()
                .equipment(equipment)
                .dateHeure(Instant.parse("2026-01-15T10:00:00Z"))
                .criticite(Criticite.CRITIQUE)
                .zoneService("Unité enrichissement")
                .statut(StatutPanne.OUVERTE)
                .descriptionInitiale("Perte de débit")
                .codeDefaut("PMP-LOSS")
                .build();
        Intervention intervention = Intervention.builder()
                .id(UUID.randomUUID())
                .failure(failure)
                .symptomes("Débit nul")
                .causeRacine("Clapet bloqué")
                .commentaireValidation("Validé par responsable")
                .dureeArretMinutes(180)
                .tempsInterventionMinutes(60)
                .build();

        InterventionKnowledgePayload payload = InterventionKnowledgePayload.fromIntervention(intervention);
        String content = payload.toIndexedContent();

        assertEquals("PMP-42", payload.equipmentCode());
        assertEquals("PMP-LOSS", payload.failureCodeDefaut());
        assertEquals("CRITIQUE", payload.failureCriticite());
        assertEquals(180, payload.dureeArretMinutes());
        assertTrue(content.contains("Équipement: PMP-42"));
        assertTrue(content.contains("Code défaut: PMP-LOSS"));
        assertTrue(content.contains("Symptômes: Débit nul"));
    }
}
