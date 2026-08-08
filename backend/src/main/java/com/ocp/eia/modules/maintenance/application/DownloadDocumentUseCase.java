package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.domain.model.InterventionDocument;
import com.ocp.eia.domain.repository.InterventionDocumentRepository;
import com.ocp.eia.modules.maintenance.domain.port.DocumentStoragePort;
import com.ocp.eia.presentation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DownloadDocumentUseCase {

    private final InterventionDocumentRepository documentRepository;
    private final DocumentStoragePort documentStorage;

    public DownloadResult execute(UUID interventionId, UUID documentId) {
        InterventionDocument document = documentRepository.findByIdAndInterventionId(documentId, interventionId)
                .orElseThrow(() -> new ResourceNotFoundException("Document introuvable: " + documentId));
        Resource resource = documentStorage.load(document.getCheminStockage());
        return new DownloadResult(resource, document.getNomFichier(), document.getTypeMime());
    }

    public record DownloadResult(Resource resource, String filename, String contentType) {}
}
