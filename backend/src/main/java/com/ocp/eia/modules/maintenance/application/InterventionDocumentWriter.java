package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.application.dto.InterventionDto.DocumentResponse;
import com.ocp.eia.application.mapper.InterventionMapper;
import com.ocp.eia.domain.model.Intervention;
import com.ocp.eia.domain.model.InterventionDocument;
import com.ocp.eia.domain.repository.InterventionDocumentRepository;
import com.ocp.eia.domain.repository.InterventionRepository;
import com.ocp.eia.modules.maintenance.domain.port.DocumentStoragePort;
import com.ocp.eia.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Short transactional writer for intervention document metadata.
 * File I/O stays outside this bean's transaction boundary.
 */
@Service
@RequiredArgsConstructor
public class InterventionDocumentWriter {

    private final InterventionRepository interventionRepository;
    private final InterventionDocumentRepository documentRepository;
    private final InterventionMapper interventionMapper;

    @Transactional
    public DocumentResponse saveMetadata(
            UUID interventionId, MultipartFile file, DocumentStoragePort.StoredDocument stored) {
        Intervention intervention = interventionRepository.findById(interventionId)
                .orElseThrow(() -> new ResourceNotFoundException("Intervention introuvable: " + interventionId));

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
