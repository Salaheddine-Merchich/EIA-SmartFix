package com.ocp.eia.presentation.controller;

import com.ocp.eia.application.dto.CommonDto.PageResponse;
import com.ocp.eia.application.dto.EquipmentDto.EquipmentRequest;
import com.ocp.eia.application.dto.EquipmentDto.EquipmentResponse;
import com.ocp.eia.application.dto.EquipmentDto.EquipmentSchemaResponse;
import com.ocp.eia.modules.asset.application.CreateEquipmentUseCase;
import com.ocp.eia.modules.asset.application.DeleteEquipmentUseCase;
import com.ocp.eia.modules.asset.application.DownloadEquipmentSchemaUseCase;
import com.ocp.eia.modules.asset.application.FindEquipmentByIdUseCase;
import com.ocp.eia.modules.asset.application.GetEquipmentHistoryUseCase;
import com.ocp.eia.modules.asset.application.ListEquipmentSchemasUseCase;
import com.ocp.eia.modules.asset.application.SearchEquipmentUseCase;
import com.ocp.eia.modules.asset.application.UpdateEquipmentUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/equipment")
@RequiredArgsConstructor
@Tag(name = "Équipements")
public class EquipmentController {

    private final SearchEquipmentUseCase searchEquipmentUseCase;
    private final FindEquipmentByIdUseCase findEquipmentByIdUseCase;
    private final GetEquipmentHistoryUseCase getEquipmentHistoryUseCase;
    private final CreateEquipmentUseCase createEquipmentUseCase;
    private final UpdateEquipmentUseCase updateEquipmentUseCase;
    private final DeleteEquipmentUseCase deleteEquipmentUseCase;
    private final ListEquipmentSchemasUseCase listEquipmentSchemasUseCase;
    private final DownloadEquipmentSchemaUseCase downloadEquipmentSchemaUseCase;

    @GetMapping
    @Operation(summary = "Rechercher des équipements")
    public ResponseEntity<PageResponse<EquipmentResponse>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String famille,
            @RequestParam(required = false) String zone,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by("code").ascending());
        return ResponseEntity.ok(searchEquipmentUseCase.execute(search, famille, zone, pageable));
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Historique complet d'un équipement")
    public ResponseEntity<GetEquipmentHistoryUseCase.EquipmentHistoryResponse> getHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(getEquipmentHistoryUseCase.execute(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EquipmentResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(findEquipmentByIdUseCase.execute(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EquipmentResponse> create(@Valid @RequestBody EquipmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(createEquipmentUseCase.execute(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EquipmentResponse> update(@PathVariable UUID id, @Valid @RequestBody EquipmentRequest request) {
        return ResponseEntity.ok(updateEquipmentUseCase.execute(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteEquipmentUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/schemas")
    @PreAuthorize("hasAnyRole('TECHNICIEN', 'RESPONSABLE_EIA', 'ADMIN')")
    @Operation(summary = "Lister les schémas techniques seed d'un équipement (lecture seule)")
    public ResponseEntity<List<EquipmentSchemaResponse>> listSchemas(@PathVariable UUID id) {
        return ResponseEntity.ok(listEquipmentSchemasUseCase.execute(id));
    }

    @GetMapping(value = "/{equipmentId}/schemas/{schemaId}/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('TECHNICIEN', 'RESPONSABLE_EIA', 'ADMIN')")
    @Operation(summary = "Télécharger un schéma technique")
    public ResponseEntity<Resource> downloadSchema(
            @PathVariable UUID equipmentId,
            @PathVariable UUID schemaId
    ) {
        var result = downloadEquipmentSchemaUseCase.execute(equipmentId, schemaId);
        String safeName = com.ocp.eia.infrastructure.storage.FileStorageService.contentDispositionFilename(result.filename());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + safeName + "\"")
                .contentType(MediaType.parseMediaType(
                        result.contentType() != null ? result.contentType() : MediaType.IMAGE_PNG_VALUE))
                .body(result.resource());
    }
}
