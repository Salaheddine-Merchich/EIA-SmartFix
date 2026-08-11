package com.ocp.eia.presentation.controller;

import com.ocp.eia.application.dto.CommonDto.PageResponse;
import com.ocp.eia.application.dto.InterventionDto.*;
import com.ocp.eia.modules.maintenance.application.CreateInterventionUseCase;
import com.ocp.eia.modules.maintenance.application.DeleteDocumentUseCase;
import com.ocp.eia.modules.maintenance.application.DeleteInterventionUseCase;
import com.ocp.eia.modules.maintenance.application.DownloadDocumentUseCase;
import com.ocp.eia.modules.maintenance.application.ExportInterventionPdfUseCase;
import com.ocp.eia.modules.maintenance.application.FindInterventionByIdUseCase;
import com.ocp.eia.modules.maintenance.application.FindInterventionsByFailureUseCase;
import com.ocp.eia.modules.maintenance.application.ListDocumentsUseCase;
import com.ocp.eia.modules.maintenance.application.SubmitInterventionUseCase;
import com.ocp.eia.modules.maintenance.application.UpdateInterventionUseCase;
import com.ocp.eia.modules.maintenance.application.UploadDocumentUseCase;
import com.ocp.eia.modules.maintenance.application.ValidateInterventionUseCase;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/interventions")
@RequiredArgsConstructor
@Tag(name = "Interventions")
public class InterventionController {

    private final FindInterventionsByFailureUseCase findInterventionsByFailureUseCase;
    private final FindInterventionByIdUseCase findInterventionByIdUseCase;
    private final CreateInterventionUseCase createInterventionUseCase;
    private final SubmitInterventionUseCase submitInterventionUseCase;
    private final UpdateInterventionUseCase updateInterventionUseCase;
    private final DeleteInterventionUseCase deleteInterventionUseCase;
    private final ValidateInterventionUseCase validateInterventionUseCase;
    private final UploadDocumentUseCase uploadDocumentUseCase;
    private final ListDocumentsUseCase listDocumentsUseCase;
    private final DownloadDocumentUseCase downloadDocumentUseCase;
    private final DeleteDocumentUseCase deleteDocumentUseCase;
    private final ExportInterventionPdfUseCase exportInterventionPdfUseCase;

    @GetMapping
    public ResponseEntity<PageResponse<InterventionResponse>> findByFailure(
            @RequestParam UUID failureId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(findInterventionsByFailureUseCase.execute(failureId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InterventionResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(findInterventionByIdUseCase.execute(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TECHNICIEN', 'RESPONSABLE_EIA', 'ADMIN')")
    public ResponseEntity<InterventionResponse> create(@Valid @RequestBody InterventionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(createInterventionUseCase.execute(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TECHNICIEN', 'RESPONSABLE_EIA', 'ADMIN')")
    public ResponseEntity<InterventionResponse> update(@PathVariable UUID id, @Valid @RequestBody InterventionUpdateRequest request) {
        return ResponseEntity.ok(updateInterventionUseCase.execute(id, request));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('TECHNICIEN', 'RESPONSABLE_EIA', 'ADMIN')")
    @Operation(summary = "Soumettre une intervention pour validation")
    public ResponseEntity<InterventionResponse> submit(@PathVariable UUID id) {
        return ResponseEntity.ok(submitInterventionUseCase.execute(id));
    }

    @PostMapping("/{id}/validate")
    @PreAuthorize("hasAnyRole('RESPONSABLE_EIA', 'ADMIN')")
    @Operation(summary = "Valider ou rejeter une intervention")
    public ResponseEntity<InterventionResponse> validate(@PathVariable UUID id, @Valid @RequestBody ValidationRequest request) {
        return ResponseEntity.ok(validateInterventionUseCase.execute(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TECHNICIEN', 'RESPONSABLE_EIA', 'ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteInterventionUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('TECHNICIEN', 'RESPONSABLE_EIA', 'ADMIN')")
    public ResponseEntity<DocumentResponse> uploadDocument(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(uploadDocumentUseCase.execute(id, file));
    }

    @GetMapping("/{id}/documents")
    @PreAuthorize("hasAnyRole('TECHNICIEN', 'RESPONSABLE_EIA', 'ADMIN')")
    @Operation(summary = "Lister les documents (lecture plant-wide authentifiée)")
    public ResponseEntity<List<DocumentResponse>> listDocuments(@PathVariable UUID id) {
        return ResponseEntity.ok(listDocumentsUseCase.execute(id));
    }

    @GetMapping(value = "/{interventionId}/documents/{documentId}/download", 
                produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('TECHNICIEN', 'RESPONSABLE_EIA', 'ADMIN')")
    @Operation(summary = "Télécharger un document (lecture plant-wide authentifiée)")
    public ResponseEntity<Resource> downloadDocument(@PathVariable UUID interventionId, @PathVariable UUID documentId) {
        var result = downloadDocumentUseCase.execute(interventionId, documentId);
        
        MediaType contentType;
        try {
            contentType = MediaType.parseMediaType(result.contentType());
        } catch (Exception e) {
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }

        String safeName = com.ocp.eia.infrastructure.storage.FileStorageService.contentDispositionFilename(result.filename());
        
        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeName + "\"")
                .body(result.resource());
    }

    @DeleteMapping("/{interventionId}/documents/{documentId}")
    @PreAuthorize("hasAnyRole('TECHNICIEN', 'RESPONSABLE_EIA', 'ADMIN')")
    public ResponseEntity<Void> deleteDocument(@PathVariable UUID interventionId, @PathVariable UUID documentId) {
        deleteDocumentUseCase.execute(interventionId, documentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/{id}/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('TECHNICIEN', 'RESPONSABLE_EIA', 'ADMIN')")
    @Operation(summary = "Exporter l'intervention en PDF")
    public ResponseEntity<byte[]> exportToPdf(@PathVariable UUID id) {
        var result = exportInterventionPdfUseCase.execute(id);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.filename() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(result.content());
    }
}
