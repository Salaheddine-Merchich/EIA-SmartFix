package com.ocp.eia.modules.asset.application;

import com.ocp.eia.domain.model.EquipmentSchema;
import com.ocp.eia.domain.repository.EquipmentSchemaRepository;
import com.ocp.eia.infrastructure.storage.FileStorageService;
import com.ocp.eia.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DownloadEquipmentSchemaUseCase {

    private final EquipmentSchemaRepository schemaRepository;
    private final FileStorageService fileStorageService;

    public DownloadResult execute(UUID equipmentId, UUID schemaId) {
        EquipmentSchema schema = schemaRepository.findByIdAndEquipmentId(schemaId, equipmentId)
                .filter(EquipmentSchema::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Schéma introuvable: " + schemaId));

        Resource resource = fileStorageService.loadAsResource(schema.getFilePath());
        String filename = schema.getFilePath().substring(schema.getFilePath().lastIndexOf('/') + 1);
        return new DownloadResult(resource, filename, schema.getMimeType());
    }

    public record DownloadResult(Resource resource, String filename, String contentType) {}
}
