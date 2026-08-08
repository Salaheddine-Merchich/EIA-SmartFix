package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.modules.maintenance.application.event.InterventionKnowledgePayload;
import com.ocp.eia.modules.maintenance.application.event.InterventionKnowledgeRemovedEvent;
import com.ocp.eia.modules.maintenance.application.event.InterventionValidatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterventionKnowledgeListenerTest {

    @Mock private IndexInterventionUseCase indexInterventionUseCase;
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks private InterventionKnowledgeListener listener;

    @Test
    void onInterventionValidated_delegatesToIndex() {
        UUID id = UUID.randomUUID();
        InterventionKnowledgePayload payload = new InterventionKnowledgePayload(
                id, "Symptôme", "Cause", null, "Action", null, "Desc",
                null, null, null, null, null, null, null, null, null, null, null, null
        );
        InterventionValidatedEvent event = new InterventionValidatedEvent(payload);
        when(indexInterventionUseCase.index(payload)).thenReturn(IndexInterventionUseCase.IndexOutcome.INDEXED);

        listener.onInterventionValidated(event);

        verify(indexInterventionUseCase).index(payload);
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    void onInterventionKnowledgeRemoved_delegatesToRemove() {
        UUID id = UUID.randomUUID();
        InterventionKnowledgeRemovedEvent event = new InterventionKnowledgeRemovedEvent(id);

        listener.onInterventionKnowledgeRemoved(event);

        verify(indexInterventionUseCase).remove(id);
        verify(eventPublisher).publishEvent(any(Object.class));
    }
}
