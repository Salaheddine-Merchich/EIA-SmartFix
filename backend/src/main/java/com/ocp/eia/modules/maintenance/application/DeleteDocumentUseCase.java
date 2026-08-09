package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.domain.model.Intervention;
import com.ocp.eia.domain.model.InterventionDocument;
import com.ocp.eia.domain.model.User;
import com.ocp.eia.domain.repository.InterventionDocumentRepository;
import com.ocp.eia.domain.repository.InterventionRepository;
import com.ocp.eia.infrastructure.security.SecurityUtils;
import com.ocp.eia.modules.maintenance.domain.port.DocumentStoragePort;
import com.ocp.eia.modules.maintenance.domain.service.InterventionWorkflow;
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
    private final InterventionRepository interventionRepository;
    private final DocumentStoragePort documentStorage;
    private final SecurityUtils securityUtils;

    public void execute(UUID interventionId, UUID documentId) {
        Intervention intervention = interventionRepository.findByIdWithDetails(interventionId)
                .orElseThrow(() -> new ResourceNotFoundException("Intervention introuvable: " + interventionId));
        User current = securityUtils.getCurrentUser();
        InterventionWorkflow.ensureEditable(intervention, current.getId(), current.getRole());

        InterventionDocument document = documentRepository.findByIdAndInterventionId(documentId, interventionId)
                .orElseThrow(() -> new ResourceNotFoundException("Document introuvable: " + documentId));
        documentStorage.delete(document.getCheminStockage());
        documentRepository.delete(document);
    }
}
