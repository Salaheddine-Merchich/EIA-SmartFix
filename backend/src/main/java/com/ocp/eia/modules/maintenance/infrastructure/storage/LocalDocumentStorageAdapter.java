package com.ocp.eia.modules.maintenance.infrastructure.storage;

import com.ocp.eia.modules.maintenance.domain.port.DocumentStoragePort;
import com.ocp.eia.infrastructure.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LocalDocumentStorageAdapter implements DocumentStoragePort {

    private final FileStorageService fileStorageService;

    @Override
    public StoredDocument store(UUID interventionId, MultipartFile file) {
        FileStorageService.StoredFile stored = fileStorageService.store(interventionId, file);
        return new StoredDocument(
                stored.storedName(),
                stored.storagePath(),
                stored.contentType(),
                stored.size()
        );
    }

    @Override
    public Resource load(String storagePath) {
        return fileStorageService.loadAsResource(storagePath);
    }

    @Override
    public void delete(String storagePath) {
        fileStorageService.delete(storagePath);
    }
}
