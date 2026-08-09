package com.ocp.eia.modules.monitoring.application;

import com.ocp.eia.modules.maintenance.application.event.*;
import com.ocp.eia.modules.monitoring.application.event.AiServiceUnavailableEvent;
import com.ocp.eia.modules.monitoring.application.event.RagIndexCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Live feed bridge.
 * <p>
 * Maintenance domain events are published inside transactions → {@link TransactionalEventListener}
 * AFTER_COMMIT. RAG/AI monitoring events may be published outside a TX (async index listener,
 * assist use case) → plain {@link EventListener} with no AFTER_COMMIT requirement.
 */
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
    @EventListener
    public void onRagIndexCompleted(RagIndexCompletedEvent event) {
        liveMonitoringService.publishRagIndexCompleted(event);
    }

    @Async
    @EventListener
    public void onAiUnavailable(AiServiceUnavailableEvent event) {
        liveMonitoringService.publishAiUnavailable(event);
    }
}
