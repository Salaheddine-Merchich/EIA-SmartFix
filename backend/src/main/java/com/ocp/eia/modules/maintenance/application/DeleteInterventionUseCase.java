package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.domain.model.Intervention;
import com.ocp.eia.domain.model.User;
import com.ocp.eia.domain.repository.InterventionRepository;
import com.ocp.eia.infrastructure.security.SecurityUtils;
import com.ocp.eia.modules.maintenance.application.event.InterventionKnowledgeRemovedEvent;
import com.ocp.eia.modules.maintenance.domain.service.InterventionWorkflow;
import com.ocp.eia.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DeleteInterventionUseCase {

    private final InterventionRepository interventionRepository;
    private final SecurityUtils securityUtils;
    private final ApplicationEventPublisher eventPublisher;

    public void execute(UUID id) {
        Intervention intervention = interventionRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Intervention introuvable: " + id));
        User current = securityUtils.getCurrentUser();
        InterventionWorkflow.ensureEditable(intervention, current.getId(), current.getRole());
        eventPublisher.publishEvent(new InterventionKnowledgeRemovedEvent(id));
        interventionRepository.delete(intervention);
    }
}
