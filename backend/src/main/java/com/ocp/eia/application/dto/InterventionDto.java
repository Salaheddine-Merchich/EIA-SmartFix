package com.ocp.eia.application.dto;

import com.ocp.eia.domain.model.StatutValidation;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class InterventionDto {

    private InterventionDto() {}

    public record InterventionRequest(
            @NotNull UUID failureId,
            String description,
            String symptomes,
            String causeRacine,
            String analyseTechnique,
            String actionsCorrectives,
            String piecesRemplacees,
            Integer dureeArretMinutes,
            Integer tempsInterventionMinutes
    ) {}

    public record InterventionUpdateRequest(
            String description,
            String symptomes,
            String causeRacine,
            String analyseTechnique,
            String actionsCorrectives,
            String piecesRemplacees,
            Integer dureeArretMinutes,
            Integer tempsInterventionMinutes
    ) {}

    public record ValidationRequest(
            @NotNull boolean approved,
            String commentaire
    ) {}

    public record InterventionResponse(
            UUID id,
            UUID failureId,
            String equipmentCode,
            UUID technicienId,
            String technicienNom,
            String description,
            String symptomes,
            String causeRacine,
            String analyseTechnique,
            String actionsCorrectives,
            String piecesRemplacees,
            Integer dureeArretMinutes,
            Integer tempsInterventionMinutes,
            StatutValidation statutValidation,
            UUID validateurId,
            String validateurNom,
            Instant dateValidation,
            String commentaireValidation,
            Instant createdAt,
            List<DocumentResponse> documents
    ) {}

    public record DocumentResponse(
            UUID id,
            String nomFichier,
            String typeMime,
            Long tailleOctets
    ) {}
}
