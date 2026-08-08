package com.ocp.eia.application.dto;

import com.ocp.eia.domain.model.Criticite;
import com.ocp.eia.domain.model.StatutPanne;
import com.ocp.eia.domain.model.StatutValidation;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public final class FailureDto {

    private FailureDto() {}

    public record FailureRequest(
            @NotNull UUID equipmentId,
            @NotNull Instant dateHeure,
            @NotNull Criticite criticite,
            String zoneService,
            UUID responsableId,
            StatutPanne statut,
            String descriptionInitiale,
            String codeDefaut
    ) {}

    public record FailureResponse(
            UUID id,
            UUID equipmentId,
            String equipmentCode,
            String equipmentDesignation,
            Instant dateHeure,
            Criticite criticite,
            String zoneService,
            UUID declarantId,
            String declarantNom,
            UUID responsableId,
            String responsableNom,
            StatutPanne statut,
            String descriptionInitiale,
            String codeDefaut,
            int interventionCount,
            StatutValidation latestInterventionStatut
    ) {}
}
