package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.domain.model.InterventionDocument;
import com.ocp.eia.domain.repository.InterventionDocumentRepository;
import com.ocp.eia.modules.maintenance.domain.port.DocumentStoragePort;
import com.ocp.eia.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DeleteDocumentUseCase {

    private final InterventionDocumentRepository documentRepository;
    private final DocumentStoragePort documentStorage;

    public void execute(UUID interventionId, UUID documentId) {
        InterventionDocument document = documentRepository.findByIdAndInterventionId(documentId, interventionId)
                .orElseThrow(() -> new ResourceNotFoundException("Document introuvable: " + documentId));
        documentStorage.delete(document.getCheminStockage());
        documentRepository.delete(document);
    }
}
