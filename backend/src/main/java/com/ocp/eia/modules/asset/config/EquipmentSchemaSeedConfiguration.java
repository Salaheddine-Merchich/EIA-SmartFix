package com.ocp.eia.modules.asset.config;

import com.ocp.eia.domain.repository.EquipmentSchemaRepository;
import com.ocp.eia.infrastructure.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Copies seed schema PNGs from classpath into the configured storage root on startup.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class EquipmentSchemaSeedConfiguration {

    private final EquipmentSchemaRepository schemaRepository;
    private final FileStorageService fileStorageService;

    @Bean
    ApplicationRunner seedEquipmentSchemaFiles() {
        return args -> {
            var schemas = schemaRepository.findAll();
            if (schemas.isEmpty()) {
                return;
            }
            int copied = 0;
            for (var schema : schemas) {
                String filePath = schema.getFilePath();
                if (filePath == null || filePath.isBlank()) {
                    continue;
                }
                String resourceName = "seed/equipment-schemas/" + filePath.substring(filePath.lastIndexOf('/') + 1);
                try {
                    fileStorageService.copySeedResource(resourceName, filePath);
                    copied++;
                } catch (Exception e) {
                    log.warn("Unable to copy seed schema {}: {}", resourceName, e.getMessage());
                }
            }
            if (copied > 0) {
                log.info("Equipment schemas: {} seed file(s) ensured in storage", copied);
            }
        };
    }
}
