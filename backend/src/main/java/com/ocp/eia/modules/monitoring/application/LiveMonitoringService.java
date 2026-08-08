package com.ocp.eia.modules.monitoring.application;

import com.ocp.eia.application.dto.LiveDto.LiveEventResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LiveMonitoringService {

    private final LiveEventBroadcaster broadcaster;
    private final LiveEventFactory factory;

    public void publishFailureCreated(com.ocp.eia.modules.maintenance.application.event.FailureCreatedEvent event) {
        broadcaster.broadcast(factory.fromFailureCreated(event));
    }

    public void publishInterventionCreated(com.ocp.eia.modules.maintenance.application.event.InterventionCreatedEvent event) {
        broadcaster.broadcast(factory.fromInterventionCreated(event));
    }

    public void publishInterventionSubmitted(com.ocp.eia.modules.maintenance.application.event.InterventionSubmittedEvent event) {
        broadcaster.broadcast(factory.fromInterventionSubmitted(event));
    }

    public void publishInterventionValidated(com.ocp.eia.modules.maintenance.application.event.InterventionValidatedEvent event) {
        broadcaster.broadcast(factory.fromInterventionValidated(event));
    }

    public void publishRagIndexCompleted(com.ocp.eia.modules.monitoring.application.event.RagIndexCompletedEvent event) {
        broadcaster.broadcast(factory.fromRagIndexCompleted(event));
    }

    public void publishAiUnavailable(com.ocp.eia.modules.monitoring.application.event.AiServiceUnavailableEvent event) {
        broadcaster.broadcast(factory.fromAiUnavailable(event));
    }

    public void publish(LiveEventResponse event) {
        broadcaster.broadcast(event);
    }
}
