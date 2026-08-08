package com.ocp.eia.presentation.controller;

import com.ocp.eia.modules.knowledge.infrastructure.observability.RagObservabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/rag")
@RequiredArgsConstructor
@ConditionalOnBean(RagObservabilityService.class)
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administration RAG")
public class RagObservabilityController {

    private final RagObservabilityService ragObservabilityService;

    @GetMapping("/health")
    @Operation(summary = "Rapport de santé complet du système RAG")
    public ResponseEntity<RagObservabilityService.RagHealthReport> getHealthReport() {
        return ResponseEntity.ok(ragObservabilityService.getHealthReport());
    }
    
    @GetMapping("/health/simple")
    @Operation(summary = "Status de santé simple (OK/WARNING/CRITICAL)")
    public ResponseEntity<String> getSimpleHealthStatus() {
        RagObservabilityService.RagHealthReport report = ragObservabilityService.getHealthReport();
        return ResponseEntity.ok(report.getOverallHealth());
    }
}