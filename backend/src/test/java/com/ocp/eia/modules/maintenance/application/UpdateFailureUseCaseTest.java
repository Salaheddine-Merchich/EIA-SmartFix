package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.application.dto.FailureDto.FailureRequest;
import com.ocp.eia.application.dto.FailureDto.FailureResponse;
import com.ocp.eia.application.mapper.FailureMapper;
import com.ocp.eia.domain.model.*;
import com.ocp.eia.domain.repository.EquipmentRepository;
import com.ocp.eia.domain.repository.FailureRepository;
import com.ocp.eia.domain.repository.UserRepository;
import com.ocp.eia.presentation.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateFailureUseCaseTest {

    @Mock private FailureRepository failureRepository;
    @Mock private EquipmentRepository equipmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private FailureMapper failureMapper;

    @InjectMocks private UpdateFailureUseCase useCase;

    @Test
    void execute_updatesFailure() {
        UUID id = UUID.randomUUID();
        UUID equipmentId = UUID.randomUUID();
        Failure failure = Failure.builder().id(id).build();
        Equipment equipment = Equipment.builder().id(equipmentId).build();
        FailureRequest request = new FailureRequest(
                equipmentId, Instant.now(), Criticite.MOYENNE, "Zone B", null, StatutPanne.EN_COURS, "Updated", "C-02"
        );
        FailureResponse response = mock(FailureResponse.class);

        when(failureRepository.findById(id)).thenReturn(Optional.of(failure));
        when(equipmentRepository.findById(equipmentId)).thenReturn(Optional.of(equipment));
        when(failureRepository.save(failure)).thenReturn(failure);
        when(failureMapper.toResponse(failure)).thenReturn(response);

        assertSame(response, useCase.execute(id, request));
        assertEquals(StatutPanne.EN_COURS, failure.getStatut());
        assertEquals("Updated", failure.getDescriptionInitiale());
    }

    @Test
    void execute_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(failureRepository.findById(id)).thenReturn(Optional.empty());

        FailureRequest request = new FailureRequest(
                UUID.randomUUID(), Instant.now(), Criticite.MOYENNE, null, null, null, null, null
        );

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(id, request));
    }
}
