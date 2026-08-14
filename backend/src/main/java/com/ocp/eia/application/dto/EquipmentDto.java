package com.ocp.eia.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class EquipmentDto {

    private EquipmentDto() {}

    public record EquipmentRequest(
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 255) String designation,
            @Size(max = 120) String famille,
            @Size(max = 120) String zone,
            @Size(max = 120) String constructeur,
            LocalDate miseEnService
    ) {}

    public record EquipmentResponse(
            UUID id,
            String code,
            String designation,
            String famille,
            String zone,
            String constructeur,
            LocalDate miseEnService,
            long failureCount
    ) {}

    public record EquipmentSchemaResponse(
            UUID id,
            UUID equipmentId,
            String equipmentCode,
            String label,
            String schemaType,
            String sourcePdf,
            Integer sourcePage,
            String mimeType,
            String caption,
            List<String> triggerKeywords,
            Instant createdAt
    ) {}
}
