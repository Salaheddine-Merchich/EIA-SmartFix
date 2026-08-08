package com.ocp.eia.presentation.controller;

import com.ocp.eia.application.dto.AnalyticsDto.RecurringDefectsAnalysisResponse;
import com.ocp.eia.application.dto.AnalyticsDto.RecurringDefectsResponse;
import com.ocp.eia.modules.analytics.application.AnalyzeRecurringDefectsUseCase;
import com.ocp.eia.modules.analytics.application.RecurringDefectsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('RESPONSABLE_EIA', 'ADMIN')")
@Tag(name = "Analytics")
public class AnalyticsController {

    private final RecurringDefectsUseCase recurringDefectsUseCase;
    private final AnalyzeRecurringDefectsUseCase analyzeRecurringDefectsUseCase;

    @GetMapping("/recurring-defects")
    @Operation(summary = "Agrégation des codes défaut récurrents")
    public ResponseEntity<RecurringDefectsResponse> getRecurringDefects(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(recurringDefectsUseCase.execute(limit));
    }

    @PostMapping("/recurring-defects/analyze")
    @Operation(summary = "Analyse IA des défauts récurrents")
    public ResponseEntity<RecurringDefectsAnalysisResponse> analyzeRecurringDefects(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(analyzeRecurringDefectsUseCase.execute(limit));
    }
}
