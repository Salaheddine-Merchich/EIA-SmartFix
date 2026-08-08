package com.ocp.eia.modules.asset.application;

import com.ocp.eia.application.dto.EquipmentDto.EquipmentRequest;
import com.ocp.eia.application.dto.EquipmentDto.EquipmentResponse;
import com.ocp.eia.application.mapper.EquipmentMapper;
import com.ocp.eia.domain.model.Equipment;
import com.ocp.eia.domain.repository.EquipmentRepository;
import com.ocp.eia.presentation.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateEquipmentUseCaseTest {

    @Mock private EquipmentRepository equipmentRepository;
    @Mock private EquipmentMapper equipmentMapper;

    @InjectMocks private CreateEquipmentUseCase useCase;

    @Test
    void execute_createsEquipment() {
        EquipmentRequest request = new EquipmentRequest("EQ-001", "Convoyeur", "Convoyage", "Zone A", "Siemens", LocalDate.now());
        EquipmentResponse response = mock(EquipmentResponse.class);

        when(equipmentRepository.existsByCode("EQ-001")).thenReturn(false);
        when(equipmentRepository.save(any(Equipment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(equipmentMapper.toResponse(any(Equipment.class))).thenReturn(response);

        assertSame(response, useCase.execute(request));
        verify(equipmentRepository).save(any(Equipment.class));
    }

    @Test
    void execute_duplicateCode_throws() {
        EquipmentRequest request = new EquipmentRequest("EQ-001", "Convoyeur", null, null, null, null);
        when(equipmentRepository.existsByCode("EQ-001")).thenReturn(true);

        assertThrows(ConflictException.class, () -> useCase.execute(request));
        verify(equipmentRepository, never()).save(any());
    }
}
