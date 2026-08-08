package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.domain.model.*;
import com.ocp.eia.domain.repository.InterventionRepository;
import com.ocp.eia.infrastructure.security.SecurityUtils;
import com.ocp.eia.modules.maintenance.application.event.InterventionKnowledgeRemovedEvent;
import com.ocp.eia.presentation.exception.ResourceNotFoundException;
import com.ocp.eia.shared.exception.DomainRuleViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteInterventionUseCaseTest {

    @Mock
    private InterventionRepository interventionRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DeleteInterventionUseCase useCase;

    @Test
    void execute_publishesEventBeforeDelete() {
        UUID id = UUID.randomUUID();
        UUID technicienId = UUID.randomUUID();
        Intervention intervention = editableIntervention(id, technicienId, StatutValidation.BROUILLON);
        User current = User.builder().id(technicienId).role(Role.TECHNICIEN).build();

        when(interventionRepository.findByIdWithDetails(id)).thenReturn(Optional.of(intervention));
        when(securityUtils.getCurrentUser()).thenReturn(current);

        useCase.execute(id);

        InOrder inOrder = inOrder(eventPublisher, interventionRepository);
        ArgumentCaptor<InterventionKnowledgeRemovedEvent> eventCaptor =
                ArgumentCaptor.forClass(InterventionKnowledgeRemovedEvent.class);
        inOrder.verify(eventPublisher).publishEvent(eventCaptor.capture());
        inOrder.verify(interventionRepository).delete(intervention);
        assertEquals(id, eventCaptor.getValue().interventionId());
    }

    @Test
    void execute_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(interventionRepository.findByIdWithDetails(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(id));
        verifyNoInteractions(eventPublisher);
        verify(interventionRepository, never()).delete(any());
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

        assertThrows(DomainRuleViolationException.class, () -> useCase.execute(id));
        verifyNoInteractions(eventPublisher);
        verify(interventionRepository, never()).delete(any());
    }

    @Test
    void execute_otherTechnicien_throws() {
        UUID id = UUID.randomUUID();
        Intervention intervention = editableIntervention(id, UUID.randomUUID(), StatutValidation.BROUILLON);

        when(interventionRepository.findByIdWithDetails(id)).thenReturn(Optional.of(intervention));
        when(securityUtils.getCurrentUser()).thenReturn(
                User.builder().id(UUID.randomUUID()).role(Role.TECHNICIEN).build()
        );

        assertThrows(DomainRuleViolationException.class, () -> useCase.execute(id));
        verifyNoInteractions(eventPublisher);
        verify(interventionRepository, never()).delete(any());
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
