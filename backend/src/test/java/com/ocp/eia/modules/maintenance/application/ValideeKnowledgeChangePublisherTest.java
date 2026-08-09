package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.domain.model.Equipment;
import com.ocp.eia.domain.model.Failure;
import com.ocp.eia.domain.model.Intervention;
import com.ocp.eia.domain.repository.InterventionRepository;
import com.ocp.eia.modules.maintenance.application.event.InterventionKnowledgeChangedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValideeKnowledgeChangePublisherTest {

    @Mock private InterventionRepository interventionRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private ValideeKnowledgeChangePublisher publisher;

    @Test
    void publishForFailure_emitsChangedEventPerValideeIntervention() {
        UUID failureId = UUID.randomUUID();
        UUID interventionId = UUID.randomUUID();
        Equipment equipment = Equipment.builder().id(UUID.randomUUID()).code("EQ-1").build();
        Failure failure = Failure.builder().id(failureId).equipment(equipment).build();
        Intervention intervention = Intervention.builder().id(interventionId).failure(failure).build();

        when(interventionRepository.findValideeByFailureIdWithDetails(failureId))
                .thenReturn(List.of(intervention));

        publisher.publishForFailure(failureId);

        ArgumentCaptor<InterventionKnowledgeChangedEvent> captor =
                ArgumentCaptor.forClass(InterventionKnowledgeChangedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals(interventionId, captor.getValue().payload().interventionId());
    }
}
