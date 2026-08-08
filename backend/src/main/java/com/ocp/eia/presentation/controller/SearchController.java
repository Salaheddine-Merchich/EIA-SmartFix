package com.ocp.eia.presentation.controller;

import com.ocp.eia.application.dto.DashboardDto.SearchResponse;
import com.ocp.eia.modules.knowledge.application.SearchInterventionUseCase;
import com.ocp.eia.domain.model.StatutValidation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Tag(name = "Recherche")
public class SearchController {

    private final SearchInterventionUseCase searchInterventionUseCase;

    @GetMapping
    @Operation(summary = "Recherche intelligente dans les interventions")
    public ResponseEntity<SearchResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID equipmentId,
            @RequestParam(required = false) String symptom,
            @RequestParam(required = false) String faultCode,
            @RequestParam(required = false) StatutValidation statut,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(searchInterventionUseCase.execute(q, equipmentId, symptom, faultCode, statut, pageable));
    }
}
