package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.application.dto.FailureDto.FailureRequest;
import com.ocp.eia.application.dto.FailureDto.FailureResponse;
import com.ocp.eia.application.mapper.FailureMapper;
import com.ocp.eia.domain.model.*;
import com.ocp.eia.domain.repository.EquipmentRepository;
import com.ocp.eia.domain.repository.FailureRepository;
import com.ocp.eia.domain.repository.UserRepository;
import com.ocp.eia.infrastructure.security.SecurityUtils;
import com.ocp.eia.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateFailureUseCaseTest {

    @Mock private FailureRepository failureRepository;
    @Mock private EquipmentRepository equipmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private FailureMapper failureMapper;
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;
    @Mock private SecurityUtils securityUtils;

    @InjectMocks private CreateFailureUseCase useCase;

    @Test
    void execute_createsFailureWithDefaultStatutAndDeclarant() {
        UUID equipmentId = UUID.randomUUID();
        Equipment equipment = Equipment.builder().id(equipmentId).code("EQ-001").build();
        User declarant = User.builder().id(UUID.randomUUID()).nomPrenom("Youssef Alami").role(Role.TECHNICIEN).build();
        FailureRequest request = new FailureRequest(
                equipmentId, Instant.now(), Criticite.HAUTE, "Zone A", null, null, "Desc", "CODE-01"
        );
        FailureResponse response = mock(FailureResponse.class);

        when(securityUtils.getCurrentUser()).thenReturn(declarant);
        when(equipmentRepository.findById(equipmentId)).thenReturn(Optional.of(equipment));
        when(failureRepository.save(any(Failure.class))).thenAnswer(inv -> inv.getArgument(0));
        when(failureMapper.toResponse(any(Failure.class))).thenReturn(response);

        assertSame(response, useCase.execute(request));

        ArgumentCaptor<Failure> captor = ArgumentCaptor.forClass(Failure.class);
        verify(failureRepository).save(captor.capture());
        assertEquals(StatutPanne.OUVERTE, captor.getValue().getStatut());
        assertEquals(equipment, captor.getValue().getEquipment());
        assertEquals(declarant, captor.getValue().getDeclarant());
    }

    @Test
    void execute_equipmentNotFound_throws() {
        UUID equipmentId = UUID.randomUUID();
        User declarant = User.builder().id(UUID.randomUUID()).nomPrenom("Test User").role(Role.TECHNICIEN).build();
        when(securityUtils.getCurrentUser()).thenReturn(declarant);
        when(equipmentRepository.findById(equipmentId)).thenReturn(Optional.empty());

        FailureRequest request = new FailureRequest(
                equipmentId, Instant.now(), Criticite.HAUTE, null, null, null, null, null
        );

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(request));
        verify(failureRepository, never()).save(any());
        verify(securityUtils).getCurrentUser();
    }
}
