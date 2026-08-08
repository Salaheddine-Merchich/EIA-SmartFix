package com.ocp.eia.application.dto;

import com.ocp.eia.domain.model.StatutValidation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class InterventionDto {

    private InterventionDto() {}

    public record InterventionRequest(
            @NotNull UUID failureId,
            @Size(max = 4000) String description,
            @Size(max = 4000) String symptomes,
            @Size(max = 4000) String causeRacine,
            @Size(max = 8000) String analyseTechnique,
            @Size(max = 4000) String actionsCorrectives,
            @Size(max = 2000) String piecesRemplacees,
            @Min(0) @Max(525600) Integer dureeArretMinutes,
            @Min(0) @Max(525600) Integer tempsInterventionMinutes
    ) {}

    public record InterventionUpdateRequest(
            @Size(max = 4000) String description,
            @Size(max = 4000) String symptomes,
            @Size(max = 4000) String causeRacine,
            @Size(max = 8000) String analyseTechnique,
            @Size(max = 4000) String actionsCorrectives,
            @Size(max = 2000) String piecesRemplacees,
            @Min(0) @Max(525600) Integer dureeArretMinutes,
            @Min(0) @Max(525600) Integer tempsInterventionMinutes
    ) {}

    public record ValidationRequest(
            @NotNull boolean approved,
            @Size(max = 2000) String commentaire
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
