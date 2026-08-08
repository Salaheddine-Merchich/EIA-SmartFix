package com.ocp.eia.modules.maintenance.domain.port;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface DocumentStoragePort {

    StoredDocument store(UUID interventionId, MultipartFile file);

    Resource load(String storagePath);

    void delete(String storagePath);

    record StoredDocument(
            String storedName,
            String storagePath,
            String contentType,
            long size
    ) {}
}
