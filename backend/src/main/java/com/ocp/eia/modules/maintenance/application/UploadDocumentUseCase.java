package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.application.dto.InterventionDto.DocumentResponse;
import com.ocp.eia.application.mapper.InterventionMapper;
import com.ocp.eia.domain.model.Intervention;
import com.ocp.eia.domain.model.InterventionDocument;
import com.ocp.eia.domain.repository.InterventionDocumentRepository;
import com.ocp.eia.domain.repository.InterventionRepository;
import com.ocp.eia.modules.maintenance.domain.port.DocumentStoragePort;
import com.ocp.eia.presentation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UploadDocumentUseCase {

    private final InterventionRepository interventionRepository;
    private final InterventionDocumentRepository documentRepository;
    private final DocumentStoragePort documentStorage;
    private final InterventionMapper interventionMapper;

    public DocumentResponse execute(UUID interventionId, MultipartFile file) {
        Intervention intervention = interventionRepository.findById(interventionId)
                .orElseThrow(() -> new ResourceNotFoundException("Intervention introuvable: " + interventionId));

        DocumentStoragePort.StoredDocument stored = documentStorage.store(interventionId, file);
        InterventionDocument document = InterventionDocument.builder()
                .intervention(intervention)
                .nomFichier(file.getOriginalFilename())
                .cheminStockage(stored.storagePath())
                .typeMime(stored.contentType())
                .tailleOctets(stored.size())
                .build();

        return interventionMapper.toDocumentResponse(documentRepository.save(document));
    }
}
