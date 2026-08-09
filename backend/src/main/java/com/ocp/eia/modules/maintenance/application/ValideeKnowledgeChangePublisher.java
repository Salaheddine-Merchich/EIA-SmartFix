package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.domain.model.Intervention;
import com.ocp.eia.domain.repository.InterventionRepository;
import com.ocp.eia.modules.maintenance.application.event.InterventionKnowledgeChangedEvent;
import com.ocp.eia.modules.maintenance.application.event.InterventionKnowledgePayload;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Publishes reindex events for VALIDEE interventions whose indexed payload may have drifted
 * after a failure or equipment update. Listeners run AFTER_COMMIT (no Ollama inside this TX).
 */
@Component
@RequiredArgsConstructor
public class ValideeKnowledgeChangePublisher {

    private final InterventionRepository interventionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public void publishForFailure(UUID failureId) {
        publish(interventionRepository.findValideeByFailureIdWithDetails(failureId));
    }

    public void publishForEquipment(UUID equipmentId) {
        publish(interventionRepository.findValideeByEquipmentIdWithDetails(equipmentId));
    }

    private void publish(List<Intervention> interventions) {
        for (Intervention intervention : interventions) {
            eventPublisher.publishEvent(new InterventionKnowledgeChangedEvent(
                    InterventionKnowledgePayload.fromIntervention(intervention)
            ));
        }
    }
}
