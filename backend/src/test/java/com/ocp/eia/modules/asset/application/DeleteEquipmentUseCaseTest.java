package com.ocp.eia.modules.asset.application;

import com.ocp.eia.domain.model.Equipment;
import com.ocp.eia.domain.model.Failure;
import com.ocp.eia.domain.repository.EquipmentRepository;
import com.ocp.eia.presentation.exception.ConflictException;
import com.ocp.eia.presentation.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteEquipmentUseCaseTest {

    @Mock private EquipmentRepository equipmentRepository;

    @InjectMocks private DeleteEquipmentUseCase useCase;

    @Test
    void execute_deletesWhenNoFailures() {
        UUID id = UUID.randomUUID();
        Equipment equipment = Equipment.builder().id(id).code("EQ-001").build();
        when(equipmentRepository.findById(id)).thenReturn(Optional.of(equipment));

        useCase.execute(id);

        verify(equipmentRepository).delete(equipment);
    }

    @Test
    void execute_withFailures_throwsConflict() {
        UUID id = UUID.randomUUID();
        Failure failure = mock(Failure.class);
        Equipment equipment = Equipment.builder().id(id).failures(List.of(failure)).build();
        when(equipmentRepository.findById(id)).thenReturn(Optional.of(equipment));

        assertThrows(ConflictException.class, () -> useCase.execute(id));
        verify(equipmentRepository, never()).delete(any());
    }

    @Test
    void execute_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(equipmentRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(id));
    }
}
