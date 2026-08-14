package com.ocp.eia.modules.asset.application;

import com.ocp.eia.application.dto.EquipmentDto.EquipmentSchemaResponse;
import com.ocp.eia.application.mapper.EquipmentSchemaMapper;
import com.ocp.eia.domain.repository.EquipmentSchemaRepository;
import com.ocp.eia.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListEquipmentSchemasUseCase {

    private final EquipmentSchemaRepository schemaRepository;
    private final EquipmentSchemaMapper schemaMapper;

    public List<EquipmentSchemaResponse> execute(UUID equipmentId) {
        return schemaRepository.findByEquipmentIdAndActiveTrueOrderByLabelAsc(equipmentId).stream()
                .map(schemaMapper::toResponse)
                .toList();
    }

    public EquipmentSchemaResponse findById(UUID equipmentId, UUID schemaId) {
        return schemaRepository.findByIdAndEquipmentId(schemaId, equipmentId)
                .filter(s -> s.isActive())
                .map(schemaMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Schéma introuvable: " + schemaId));
    }
}
