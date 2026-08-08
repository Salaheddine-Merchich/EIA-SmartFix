package com.ocp.eia.modules.maintenance.application.event;

import com.ocp.eia.domain.model.Equipment;
import com.ocp.eia.domain.model.Failure;
import com.ocp.eia.domain.model.Intervention;

import java.util.UUID;

public record InterventionKnowledgePayload(
        UUID interventionId,
        String symptomes,
        String causeRacine,
        String analyseTechnique,
        String actionsCorrectives,
        String piecesRemplacees,
        String description,
        String commentaireValidation,
        Integer dureeArretMinutes,
        Integer tempsInterventionMinutes,
        String failureDescriptionInitiale,
        String failureCodeDefaut,
        String failureCriticite,
        String failureZoneService,
        String equipmentCode,
        String equipmentDesignation,
        String equipmentFamille,
        String equipmentZone,
        String equipmentConstructeur
) {
    public String toIndexedContent() {
        return java.util.stream.Stream.of(
                        field("Équipement", equipmentCode),
                        field("Désignation équipement", equipmentDesignation),
                        field("Famille", equipmentFamille),
                        field("Zone équipement", equipmentZone),
                        field("Constructeur", equipmentConstructeur),
                        field("Description panne", failureDescriptionInitiale),
                        field("Code défaut", failureCodeDefaut),
                        field("Criticité", failureCriticite),
                        field("Zone service", failureZoneService),
                        field("Symptômes", symptomes),
                        field("Cause racine", causeRacine),
                        field("Analyse", analyseTechnique),
                        field("Actions correctives", actionsCorrectives),
                        field("Pièces", piecesRemplacees),
                        field("Description", description),
                        field("Commentaire validation", commentaireValidation),
                        field("Durée arrêt (min)", dureeArretMinutes),
                        field("Temps intervention (min)", tempsInterventionMinutes)
                )
                .filter(s -> !s.isBlank())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }

    private static String field(String label, Object value) {
        if (value == null) return "";
        String text = value.toString().trim();
        if (text.isBlank()) return "";
        return label + ": " + text;
    }

    public static InterventionKnowledgePayload fromIntervention(Intervention intervention) {
        String failureDescriptionInitiale = null;
        String failureCodeDefaut = null;
        String failureCriticite = null;
        String failureZoneService = null;
        String equipmentCode = null;
        String equipmentDesignation = null;
        String equipmentFamille = null;
        String equipmentZone = null;
        String equipmentConstructeur = null;

        try {
            Failure failure = intervention.getFailure();
            if (failure != null) {
                failureDescriptionInitiale = failure.getDescriptionInitiale();
                failureCodeDefaut = failure.getCodeDefaut();
                if (failure.getCriticite() != null) {
                    failureCriticite = failure.getCriticite().name();
                }
                failureZoneService = failure.getZoneService();
                Equipment equipment = failure.getEquipment();
                if (equipment != null) {
                    equipmentCode = equipment.getCode();
                    equipmentDesignation = equipment.getDesignation();
                    equipmentFamille = equipment.getFamille();
                    equipmentZone = equipment.getZone();
                    equipmentConstructeur = equipment.getConstructeur();
                }
            }
        } catch (org.hibernate.LazyInitializationException ignored) {
            // Relations non chargées : l'appelant doit utiliser findByIdWithDetails ou
            // findByStatutValidationWithDetails (JOIN FETCH failure + equipment).
        }

        return new InterventionKnowledgePayload(
                intervention.getId(),
                intervention.getSymptomes(),
                intervention.getCauseRacine(),
                intervention.getAnalyseTechnique(),
                intervention.getActionsCorrectives(),
                intervention.getPiecesRemplacees(),
                intervention.getDescription(),
                intervention.getCommentaireValidation(),
                intervention.getDureeArretMinutes(),
                intervention.getTempsInterventionMinutes(),
                failureDescriptionInitiale,
                failureCodeDefaut,
                failureCriticite,
                failureZoneService,
                equipmentCode,
                equipmentDesignation,
                equipmentFamille,
                equipmentZone,
                equipmentConstructeur
        );
    }
}
