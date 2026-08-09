package com.ocp.eia.modules.asset.application;

import com.ocp.eia.application.dto.EquipmentDto.EquipmentRequest;
import com.ocp.eia.application.dto.EquipmentDto.EquipmentResponse;
import com.ocp.eia.application.mapper.EquipmentMapper;
import com.ocp.eia.domain.model.Equipment;
import com.ocp.eia.domain.repository.EquipmentRepository;
import com.ocp.eia.modules.maintenance.application.ValideeKnowledgeChangePublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateEquipmentUseCaseTest {

    @Mock private EquipmentRepository equipmentRepository;
    @Mock private EquipmentMapper equipmentMapper;
    @Mock private ValideeKnowledgeChangePublisher valideeKnowledgeChangePublisher;
    @InjectMocks private UpdateEquipmentUseCase useCase;

    @Test
    void execute_publishesKnowledgeChangeWhenIndexedFieldsChange() {
        UUID id = UUID.randomUUID();
        Equipment equipment = Equipment.builder()
                .id(id)
                .code("EQ-1")
                .designation("Old")
                .famille("F")
                .zone("Z")
                .constructeur("OEM")
                .build();
        EquipmentRequest request = new EquipmentRequest("EQ-1", "New", "F", "Z", "OEM", null);
        EquipmentResponse response = mock(EquipmentResponse.class);

        when(equipmentRepository.findById(id)).thenReturn(Optional.of(equipment));
        when(equipmentRepository.save(equipment)).thenReturn(equipment);
        when(equipmentMapper.toResponse(equipment)).thenReturn(response);

        assertSame(response, useCase.execute(id, request));
        verify(valideeKnowledgeChangePublisher).publishForEquipment(id);
    }
}
