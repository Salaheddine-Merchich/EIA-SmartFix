package com.ocp.eia.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public final class EquipmentDto {

    private EquipmentDto() {}

    public record EquipmentRequest(
            @NotBlank String code,
            @NotBlank String designation,
            String famille,
            String zone,
            String constructeur,
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
}
