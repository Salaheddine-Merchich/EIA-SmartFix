package com.ocp.eia.presentation.controller;

import com.ocp.eia.application.dto.DashboardDto.DashboardResponse;
import com.ocp.eia.modules.analytics.application.DashboardUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('RESPONSABLE_EIA', 'ADMIN')")
@Tag(name = "Dashboard")
public class DashboardController {

    private final DashboardUseCase dashboardUseCase;

    @GetMapping
    @Operation(summary = "Statistiques agrégées")
    public ResponseEntity<DashboardResponse> getStats() {
        return ResponseEntity.ok(dashboardUseCase.execute());
    }
}
