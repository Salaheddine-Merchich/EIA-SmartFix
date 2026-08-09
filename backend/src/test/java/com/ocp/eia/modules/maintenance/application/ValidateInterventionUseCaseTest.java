package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.application.dto.InterventionDto.InterventionResponse;
import com.ocp.eia.application.dto.InterventionDto.ValidationRequest;
import com.ocp.eia.application.mapper.InterventionMapper;
import com.ocp.eia.domain.model.*;
import com.ocp.eia.domain.repository.InterventionRepository;
import com.ocp.eia.infrastructure.security.SecurityUtils;
import com.ocp.eia.modules.maintenance.application.event.InterventionKnowledgeRemovedEvent;
import com.ocp.eia.modules.maintenance.application.event.InterventionValidatedEvent;
import com.ocp.eia.shared.exception.ResourceNotFoundException;
import com.ocp.eia.shared.exception.DomainRuleViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidateInterventionUseCaseTest {

    @Mock
    private InterventionRepository interventionRepository;

    @Mock
    private InterventionMapper interventionMapper;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ValidateInterventionUseCase useCase;

    @Test
    void execute_approved_publishesValidatedEvent() {
        UUID id = UUID.randomUUID();
        Intervention intervention = submittedIntervention(id);
        User validateur = User.builder().id(UUID.randomUUID()).role(Role.RESPONSABLE_EIA).nomPrenom("Validateur").build();
        InterventionResponse response = mock(InterventionResponse.class);

        when(interventionRepository.findByIdWithDetails(id)).thenReturn(Optional.of(intervention));
        when(securityUtils.getCurrentUser()).thenReturn(validateur);
        when(interventionRepository.save(intervention)).thenReturn(intervention);
        when(interventionMapper.toResponse(intervention)).thenReturn(response);

        ValidationRequest request = new ValidationRequest(true, "Conforme");
        assertSame(response, useCase.execute(id, request));

        assertEquals(StatutValidation.VALIDEE, intervention.getStatutValidation());
        assertEquals(validateur, intervention.getValidateur());
        assertEquals("Conforme", intervention.getCommentaireValidation());

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertInstanceOf(InterventionValidatedEvent.class, eventCaptor.getValue());
        verify(eventPublisher, never()).publishEvent(any(InterventionKnowledgeRemovedEvent.class));
    }

    @Test
    void execute_rejected_publishesRemovedEvent() {
        UUID id = UUID.randomUUID();
        Intervention intervention = submittedIntervention(id);
        User validateur = User.builder().id(UUID.randomUUID()).role(Role.ADMIN).build();
        InterventionResponse response = mock(InterventionResponse.class);

        when(interventionRepository.findByIdWithDetails(id)).thenReturn(Optional.of(intervention));
        when(securityUtils.getCurrentUser()).thenReturn(validateur);
        when(interventionRepository.save(intervention)).thenReturn(intervention);
        when(interventionMapper.toResponse(intervention)).thenReturn(response);

        useCase.execute(id, new ValidationRequest(false, "Incomplet"));

        assertEquals(StatutValidation.REJETEE, intervention.getStatutValidation());

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertInstanceOf(InterventionKnowledgeRemovedEvent.class, eventCaptor.getValue());
        assertEquals(id, ((InterventionKnowledgeRemovedEvent) eventCaptor.getValue()).interventionId());
    }

    @Test
    void execute_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(interventionRepository.findByIdWithDetails(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(id, new ValidationRequest(true, null)));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void execute_wrongStatus_throws() {
        UUID id = UUID.randomUUID();
        Intervention intervention = submittedIntervention(id);
        intervention.setStatutValidation(StatutValidation.BROUILLON);

        when(interventionRepository.findByIdWithDetails(id)).thenReturn(Optional.of(intervention));
        when(securityUtils.getCurrentUser()).thenReturn(
                User.builder().id(UUID.randomUUID()).role(Role.RESPONSABLE_EIA).build()
        );

        assertThrows(DomainRuleViolationException.class,
                () -> useCase.execute(id, new ValidationRequest(true, null)));
        verify(interventionRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    private Intervention submittedIntervention(UUID id) {
        User technicien = User.builder().id(UUID.randomUUID()).role(Role.TECHNICIEN).build();
        Equipment equipment = Equipment.builder().id(UUID.randomUUID()).code("EQ-001").build();
        Failure failure = Failure.builder().id(UUID.randomUUID()).equipment(equipment).build();
        return Intervention.builder()
                .id(id)
                .statutValidation(StatutValidation.SOUMISE)
                .technicien(technicien)
                .failure(failure)
                .build();
    }
}
