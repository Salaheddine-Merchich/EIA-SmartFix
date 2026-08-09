package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.application.dto.InterventionDto.DocumentResponse;
import com.ocp.eia.domain.repository.InterventionRepository;
import com.ocp.eia.modules.maintenance.domain.port.DocumentStoragePort;
import com.ocp.eia.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadDocumentUseCase {

    private final InterventionRepository interventionRepository;
    private final DocumentStoragePort documentStorage;
    private final InterventionDocumentWriter documentWriter;

    /**
     * Stores the file outside any long DB transaction, then persists metadata in a short write TX.
     * On metadata failure, best-effort deletes the stored file to avoid orphans.
     */
    public DocumentResponse execute(UUID interventionId, MultipartFile file) {
        // Short existence check (Spring Data opens its own brief TX)
        if (!interventionRepository.existsById(interventionId)) {
            throw new ResourceNotFoundException("Intervention introuvable: " + interventionId);
        }

        DocumentStoragePort.StoredDocument stored = documentStorage.store(interventionId, file);
        try {
            return documentWriter.saveMetadata(interventionId, file, stored);
        } catch (RuntimeException ex) {
            try {
                documentStorage.delete(stored.storagePath());
            } catch (Exception cleanupEx) {
                log.warn("Échec nettoyage fichier après erreur DB ({}): {}",
                        stored.storagePath(), cleanupEx.getMessage());
            }
            throw ex;
        }
    }
}
