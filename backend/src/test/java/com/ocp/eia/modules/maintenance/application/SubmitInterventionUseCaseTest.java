package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.application.dto.InterventionDto.InterventionResponse;
import com.ocp.eia.application.mapper.InterventionMapper;
import com.ocp.eia.domain.model.*;
import com.ocp.eia.domain.repository.InterventionRepository;
import com.ocp.eia.infrastructure.security.SecurityUtils;
import com.ocp.eia.shared.exception.DomainRuleViolationException;
import com.ocp.eia.shared.exception.ResourceNotFoundException;
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
class SubmitInterventionUseCaseTest {

    @Mock private InterventionRepository interventionRepository;
    @Mock private InterventionMapper interventionMapper;
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;
    @Mock private SecurityUtils securityUtils;
    @InjectMocks private SubmitInterventionUseCase useCase;

    @Test
    void execute_fromBrouillon_setsSoumise() {
        UUID id = UUID.randomUUID();
        UUID techId = UUID.randomUUID();
        Intervention intervention = intervention(id, techId, StatutValidation.BROUILLON);
        InterventionResponse response = mock(InterventionResponse.class);

        when(interventionRepository.findByIdWithDetails(id)).thenReturn(Optional.of(intervention));
        when(securityUtils.getCurrentUser()).thenReturn(User.builder().id(techId).role(Role.TECHNICIEN).build());
        when(interventionRepository.save(intervention)).thenReturn(intervention);
        when(interventionMapper.toResponse(intervention)).thenReturn(response);

        assertSame(response, useCase.execute(id));
        assertEquals(StatutValidation.SOUMISE, intervention.getStatutValidation());
    }

    @Test
    void execute_fromRejetee_setsSoumise() {
        UUID id = UUID.randomUUID();
        UUID techId = UUID.randomUUID();
        Intervention intervention = intervention(id, techId, StatutValidation.REJETEE);

        when(interventionRepository.findByIdWithDetails(id)).thenReturn(Optional.of(intervention));
        when(securityUtils.getCurrentUser()).thenReturn(User.builder().id(techId).role(Role.TECHNICIEN).build());
        when(interventionRepository.save(intervention)).thenReturn(intervention);
        when(interventionMapper.toResponse(intervention)).thenReturn(mock(InterventionResponse.class));

        useCase.execute(id);
        assertEquals(StatutValidation.SOUMISE, intervention.getStatutValidation());
    }

    @Test
    void execute_otherTechnicien_forbidden() {
        UUID id = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Intervention intervention = intervention(id, ownerId, StatutValidation.BROUILLON);

        when(interventionRepository.findByIdWithDetails(id)).thenReturn(Optional.of(intervention));
        when(securityUtils.getCurrentUser())
                .thenReturn(User.builder().id(UUID.randomUUID()).role(Role.TECHNICIEN).build());

        assertThrows(DomainRuleViolationException.class, () -> useCase.execute(id));
        verify(interventionRepository, never()).save(any());
    }

    @Test
    void execute_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(interventionRepository.findByIdWithDetails(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(id));
        verify(interventionRepository, never()).save(any());
    }

    @Test
    void execute_fromValidee_throws() {
        UUID id = UUID.randomUUID();
        UUID techId = UUID.randomUUID();
        Intervention intervention = intervention(id, techId, StatutValidation.VALIDEE);

        when(interventionRepository.findByIdWithDetails(id)).thenReturn(Optional.of(intervention));
        when(securityUtils.getCurrentUser()).thenReturn(User.builder().id(techId).role(Role.TECHNICIEN).build());

        assertThrows(DomainRuleViolationException.class, () -> useCase.execute(id));
        verify(interventionRepository, never()).save(any());
    }

    private Intervention intervention(UUID id, UUID techId, StatutValidation statut) {
        User technicien = User.builder().id(techId).role(Role.TECHNICIEN).build();
        Failure failure = Failure.builder().id(UUID.randomUUID()).build();
        return Intervention.builder()
                .id(id)
                .statutValidation(statut)
                .technicien(technicien)
                .failure(failure)
                .build();
    }
}
