package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.application.dto.InterventionDto.InterventionRequest;
import com.ocp.eia.application.dto.InterventionDto.InterventionResponse;
import com.ocp.eia.application.mapper.InterventionMapper;
import com.ocp.eia.domain.model.*;
import com.ocp.eia.domain.repository.FailureRepository;
import com.ocp.eia.domain.repository.InterventionRepository;
import com.ocp.eia.infrastructure.security.SecurityUtils;
import com.ocp.eia.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateInterventionUseCaseTest {

    @Mock
    private InterventionRepository interventionRepository;

    @Mock
    private FailureRepository failureRepository;

    @Mock
    private InterventionMapper interventionMapper;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CreateInterventionUseCase useCase;

    @Test
    void execute_createsInterventionInBrouillon() {
        UUID failureId = UUID.randomUUID();
        Failure failure = Failure.builder().id(failureId).build();
        User technicien = User.builder().id(UUID.randomUUID()).role(Role.TECHNICIEN).nomPrenom("Tech").build();
        InterventionRequest request = new InterventionRequest(
                failureId, "Desc", "Sympt", "Cause", "Analyse", "Actions", "Pieces", 30, 60
        );
        InterventionResponse response = mock(InterventionResponse.class);

        when(failureRepository.findById(failureId)).thenReturn(Optional.of(failure));
        when(securityUtils.getCurrentUser()).thenReturn(technicien);
        when(interventionRepository.save(any(Intervention.class))).thenAnswer(inv -> inv.getArgument(0));
        when(interventionMapper.toResponse(any(Intervention.class))).thenReturn(response);

        assertSame(response, useCase.execute(request));

        ArgumentCaptor<Intervention> captor = ArgumentCaptor.forClass(Intervention.class);
        verify(interventionRepository).save(captor.capture());
        Intervention saved = captor.getValue();
        assertEquals(failure, saved.getFailure());
        assertEquals(technicien, saved.getTechnicien());
        assertEquals(StatutValidation.BROUILLON, saved.getStatutValidation());
        assertEquals("Desc", saved.getDescription());
        assertEquals("Sympt", saved.getSymptomes());
        assertEquals("Cause", saved.getCauseRacine());
        assertEquals("Analyse", saved.getAnalyseTechnique());
        assertEquals("Actions", saved.getActionsCorrectives());
        assertEquals("Pieces", saved.getPiecesRemplacees());
        assertEquals(30, saved.getDureeArretMinutes());
        assertEquals(60, saved.getTempsInterventionMinutes());
    }

    @Test
    void execute_failureNotFound_throws() {
        UUID failureId = UUID.randomUUID();
        when(failureRepository.findById(failureId)).thenReturn(Optional.empty());

        InterventionRequest request = new InterventionRequest(
                failureId, null, null, null, null, null, null, null, null
        );

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(request));
        verifyNoInteractions(interventionRepository, securityUtils);
    }
}
