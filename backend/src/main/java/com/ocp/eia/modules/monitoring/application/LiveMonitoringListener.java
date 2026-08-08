package com.ocp.eia.modules.monitoring.application;

import com.ocp.eia.modules.maintenance.application.event.*;
import com.ocp.eia.modules.monitoring.application.event.AiServiceUnavailableEvent;
import com.ocp.eia.modules.monitoring.application.event.RagIndexCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class LiveMonitoringListener {

    private final LiveMonitoringService liveMonitoringService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFailureCreated(FailureCreatedEvent event) {
        liveMonitoringService.publishFailureCreated(event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInterventionCreated(InterventionCreatedEvent event) {
        liveMonitoringService.publishInterventionCreated(event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInterventionSubmitted(InterventionSubmittedEvent event) {
        liveMonitoringService.publishInterventionSubmitted(event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInterventionValidated(InterventionValidatedEvent event) {
        liveMonitoringService.publishInterventionValidated(event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRagIndexCompleted(RagIndexCompletedEvent event) {
        liveMonitoringService.publishRagIndexCompleted(event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAiUnavailable(AiServiceUnavailableEvent event) {
        liveMonitoringService.publishAiUnavailable(event);
    }
}
