package com.ocp.eia.modules.monitoring.application;

import com.ocp.eia.modules.maintenance.application.event.FailureCreatedEvent;
import com.ocp.eia.modules.maintenance.application.event.InterventionKnowledgePayload;
import com.ocp.eia.modules.maintenance.application.event.InterventionSubmittedEvent;
import com.ocp.eia.modules.maintenance.application.event.InterventionValidatedEvent;
import com.ocp.eia.modules.monitoring.application.event.AiServiceUnavailableEvent;
import com.ocp.eia.modules.monitoring.application.event.RagIndexCompletedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LiveMonitoringListenerTest {

    @Mock private LiveMonitoringService liveMonitoringService;
    @InjectMocks private LiveMonitoringListener listener;

    @Test
    void onFailureCreated_delegatesToService() {
        FailureCreatedEvent event = new FailureCreatedEvent(UUID.randomUUID(), "EQ-1", "HAUTE", "Desc");

        listener.onFailureCreated(event);

        verify(liveMonitoringService).publishFailureCreated(event);
    }

    @Test
    void onInterventionSubmitted_delegatesToService() {
        InterventionSubmittedEvent event = new InterventionSubmittedEvent(
                UUID.randomUUID(), "EQ-1", "Tech");

        listener.onInterventionSubmitted(event);

        verify(liveMonitoringService).publishInterventionSubmitted(event);
    }

    @Test
    void onInterventionValidated_delegatesToService() {
        InterventionKnowledgePayload payload = new InterventionKnowledgePayload(
                UUID.randomUUID(),
                "symptomes",
                "cause",
                "analyse",
                "actions",
                "pieces",
                "description",
                "ok",
                10,
                20,
                "panne",
                "CODE",
                "HAUTE",
                "Z1",
                "EQ-1",
                "Pompe",
                "FAM",
                "ZONE",
                "OEM"
        );
        InterventionValidatedEvent event = new InterventionValidatedEvent(payload);

        listener.onInterventionValidated(event);

        verify(liveMonitoringService).publishInterventionValidated(event);
    }

    @Test
    void onRagIndexCompleted_delegatesWithoutRequiringTx() {
        RagIndexCompletedEvent event = new RagIndexCompletedEvent(
                UUID.randomUUID(), "INDEXED", "EQ-1");

        listener.onRagIndexCompleted(event);

        verify(liveMonitoringService).publishRagIndexCompleted(event);
    }

    @Test
    void onAiUnavailable_delegatesWithoutRequiringTx() {
        AiServiceUnavailableEvent event = new AiServiceUnavailableEvent("ollama down");

        listener.onAiUnavailable(event);

        verify(liveMonitoringService).publishAiUnavailable(event);
    }
}
