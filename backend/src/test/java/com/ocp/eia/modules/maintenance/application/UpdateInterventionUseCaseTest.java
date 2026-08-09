package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.application.dto.InterventionDto.InterventionResponse;
import com.ocp.eia.application.dto.InterventionDto.InterventionUpdateRequest;
import com.ocp.eia.application.mapper.InterventionMapper;
import com.ocp.eia.domain.model.*;
import com.ocp.eia.domain.repository.InterventionRepository;
import com.ocp.eia.infrastructure.security.SecurityUtils;
import com.ocp.eia.shared.exception.ResourceNotFoundException;
import com.ocp.eia.shared.exception.DomainRuleViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateInterventionUseCaseTest {

    @Mock
    private InterventionRepository interventionRepository;

    @Mock
    private InterventionMapper interventionMapper;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private UpdateInterventionUseCase useCase;

    @Test
    void execute_updatesFields() {
        UUID id = UUID.randomUUID();
        UUID technicienId = UUID.randomUUID();
        Intervention intervention = editableIntervention(id, technicienId, StatutValidation.BROUILLON);
        User current = User.builder().id(technicienId).role(Role.TECHNICIEN).build();
        InterventionUpdateRequest request = new InterventionUpdateRequest(
                "Desc", "Sympt", "Cause", "Analyse", "Actions", "Pieces", 45, 90
        );
        InterventionResponse response = mock(InterventionResponse.class);

        when(interventionRepository.findByIdWithDetails(id)).thenReturn(Optional.of(intervention));
        when(securityUtils.getCurrentUser()).thenReturn(current);
        when(interventionRepository.save(intervention)).thenReturn(intervention);
        when(interventionMapper.toResponse(intervention)).thenReturn(response);

        assertSame(response, useCase.execute(id, request));
        assertEquals("Desc", intervention.getDescription());
        assertEquals("Sympt", intervention.getSymptomes());
        assertEquals("Cause", intervention.getCauseRacine());
        assertEquals("Analyse", intervention.getAnalyseTechnique());
        assertEquals("Actions", intervention.getActionsCorrectives());
        assertEquals("Pieces", intervention.getPiecesRemplacees());
        assertEquals(45, intervention.getDureeArretMinutes());
        assertEquals(90, intervention.getTempsInterventionMinutes());
    }

    @Test
    void execute_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(interventionRepository.findByIdWithDetails(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(id, new InterventionUpdateRequest(null, null, null, null, null, null, null, null)));
        verify(interventionRepository, never()).save(any());
    }

    @Test
    void execute_validee_throws() {
        UUID id = UUID.randomUUID();
        UUID technicienId = UUID.randomUUID();
        Intervention intervention = editableIntervention(id, technicienId, StatutValidation.VALIDEE);

        when(interventionRepository.findByIdWithDetails(id)).thenReturn(Optional.of(intervention));
        when(securityUtils.getCurrentUser()).thenReturn(
                User.builder().id(technicienId).role(Role.TECHNICIEN).build()
        );

        assertThrows(DomainRuleViolationException.class,
                () -> useCase.execute(id, new InterventionUpdateRequest(null, null, null, null, null, null, null, null)));
        verify(interventionRepository, never()).save(any());
    }

    @Test
    void execute_otherTechnicien_throws() {
        UUID id = UUID.randomUUID();
        Intervention intervention = editableIntervention(id, UUID.randomUUID(), StatutValidation.BROUILLON);

        when(interventionRepository.findByIdWithDetails(id)).thenReturn(Optional.of(intervention));
        when(securityUtils.getCurrentUser()).thenReturn(
                User.builder().id(UUID.randomUUID()).role(Role.TECHNICIEN).build()
        );

        assertThrows(DomainRuleViolationException.class,
                () -> useCase.execute(id, new InterventionUpdateRequest(null, null, null, null, null, null, null, null)));
        verify(interventionRepository, never()).save(any());
    }

    private Intervention editableIntervention(UUID id, UUID technicienId, StatutValidation statut) {
        User technicien = User.builder().id(technicienId).role(Role.TECHNICIEN).build();
        Failure failure = Failure.builder().id(UUID.randomUUID()).build();
        return Intervention.builder()
                .id(id)
                .statutValidation(statut)
                .technicien(technicien)
                .failure(failure)
                .build();
    }
}
