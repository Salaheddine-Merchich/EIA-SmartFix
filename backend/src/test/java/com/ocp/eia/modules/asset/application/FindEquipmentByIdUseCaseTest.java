package com.ocp.eia.modules.asset.application;

import com.ocp.eia.application.mapper.EquipmentMapper;
import com.ocp.eia.domain.model.Equipment;
import com.ocp.eia.domain.repository.EquipmentRepository;
import com.ocp.eia.domain.repository.FailureRepository;
import com.ocp.eia.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindEquipmentByIdUseCaseTest {

    @Mock private EquipmentRepository equipmentRepository;
    @Mock private FailureRepository failureRepository;
    @Mock private EquipmentMapper equipmentMapper;

    @InjectMocks private FindEquipmentByIdUseCase useCase;

    @Test
    void execute_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(equipmentRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(id));
        verify(failureRepository, never()).findByEquipmentIdOrderByDateHeureDesc(id);
    }
}
