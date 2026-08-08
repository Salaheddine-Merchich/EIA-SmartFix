package com.ocp.eia.presentation.controller;



import com.ocp.eia.application.dto.CommonDto.PageResponse;

import com.ocp.eia.application.dto.FailureDto.FailureRequest;

import com.ocp.eia.application.dto.FailureDto.FailureResponse;

import com.ocp.eia.domain.model.Criticite;

import com.ocp.eia.domain.model.StatutPanne;

import com.ocp.eia.modules.maintenance.application.CreateFailureUseCase;

import com.ocp.eia.modules.maintenance.application.DeleteFailureUseCase;

import com.ocp.eia.modules.maintenance.application.FindFailureByIdUseCase;

import com.ocp.eia.modules.maintenance.application.ListFailuresUseCase;

import com.ocp.eia.modules.maintenance.application.UpdateFailureUseCase;

import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;

import org.springframework.data.domain.Sort;

import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;



import java.util.UUID;



@RestController

@RequestMapping("/api/v1/failures")

@RequiredArgsConstructor

@Tag(name = "Pannes")

public class FailureController {



    private final ListFailuresUseCase listFailuresUseCase;

    private final FindFailureByIdUseCase findFailureByIdUseCase;

    private final CreateFailureUseCase createFailureUseCase;

    private final UpdateFailureUseCase updateFailureUseCase;

    private final DeleteFailureUseCase deleteFailureUseCase;



    @GetMapping

    @Operation(summary = "Rechercher des pannes")

    public ResponseEntity<PageResponse<FailureResponse>> search(

            @RequestParam(required = false) UUID equipmentId,

            @RequestParam(required = false) StatutPanne statut,

            @RequestParam(required = false) Criticite criticite,

            @RequestParam(required = false) String codeDefaut,

            @RequestParam(required = false) String search,

            @RequestParam(defaultValue = "0") int page,

            @RequestParam(defaultValue = "20") int size

    ) {

        var pageable = PageRequest.of(page, size, Sort.by("dateHeure").descending());

        return ResponseEntity.ok(listFailuresUseCase.execute(equipmentId, statut, criticite, codeDefaut, search, pageable));

    }



    @GetMapping("/{id}")

    public ResponseEntity<FailureResponse> findById(@PathVariable UUID id) {

        return ResponseEntity.ok(findFailureByIdUseCase.execute(id));

    }



    @PostMapping

    @PreAuthorize("hasAnyRole('TECHNICIEN', 'RESPONSABLE_EIA', 'ADMIN')")

    public ResponseEntity<FailureResponse> create(@Valid @RequestBody FailureRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(createFailureUseCase.execute(request));

    }



    @PutMapping("/{id}")

    @PreAuthorize("hasAnyRole('TECHNICIEN', 'RESPONSABLE_EIA', 'ADMIN')")

    public ResponseEntity<FailureResponse> update(@PathVariable UUID id, @Valid @RequestBody FailureRequest request) {

        return ResponseEntity.ok(updateFailureUseCase.execute(id, request));

    }



    @DeleteMapping("/{id}")

    @PreAuthorize("hasAnyRole('RESPONSABLE_EIA', 'ADMIN')")

    public ResponseEntity<Void> delete(@PathVariable UUID id) {

        deleteFailureUseCase.execute(id);

        return ResponseEntity.noContent().build();

    }

}


