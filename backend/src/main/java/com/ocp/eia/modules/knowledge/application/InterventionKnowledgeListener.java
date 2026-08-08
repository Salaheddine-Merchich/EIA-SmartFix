package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.modules.maintenance.application.event.InterventionKnowledgeRemovedEvent;
import com.ocp.eia.modules.maintenance.application.event.InterventionValidatedEvent;
import com.ocp.eia.modules.monitoring.application.event.RagIndexCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnProperty(name = "app.knowledge.enabled", havingValue = "true")
@ConditionalOnBean(IndexInterventionUseCase.class)
@RequiredArgsConstructor
public class InterventionKnowledgeListener {

    private final IndexInterventionUseCase indexInterventionUseCase;
    private final ApplicationEventPublisher eventPublisher;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInterventionValidated(InterventionValidatedEvent event) {
        IndexInterventionUseCase.IndexOutcome outcome = indexInterventionUseCase.index(event.payload());
        eventPublisher.publishEvent(new RagIndexCompletedEvent(
                event.payload().interventionId(),
                outcome.name(),
                event.payload().equipmentCode()
        ));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInterventionKnowledgeRemoved(InterventionKnowledgeRemovedEvent event) {
        indexInterventionUseCase.remove(event.interventionId());
        eventPublisher.publishEvent(new RagIndexCompletedEvent(
                event.interventionId(),
                "REMOVED",
                ""
        ));
    }
}
