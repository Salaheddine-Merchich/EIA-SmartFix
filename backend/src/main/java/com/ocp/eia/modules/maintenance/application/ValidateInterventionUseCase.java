package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.application.dto.InterventionDto.InterventionResponse;
import com.ocp.eia.application.dto.InterventionDto.ValidationRequest;
import com.ocp.eia.application.mapper.InterventionMapper;
import com.ocp.eia.domain.model.Intervention;
import com.ocp.eia.domain.model.User;
import com.ocp.eia.domain.repository.InterventionRepository;
import com.ocp.eia.infrastructure.security.SecurityUtils;
import com.ocp.eia.modules.maintenance.application.event.InterventionKnowledgePayload;
import com.ocp.eia.modules.maintenance.application.event.InterventionKnowledgeRemovedEvent;
import com.ocp.eia.modules.maintenance.application.event.InterventionValidatedEvent;
import com.ocp.eia.modules.maintenance.domain.service.InterventionWorkflow;
import com.ocp.eia.presentation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ValidateInterventionUseCase {

    private final InterventionRepository interventionRepository;
    private final InterventionMapper interventionMapper;
    private final SecurityUtils securityUtils;
    private final ApplicationEventPublisher eventPublisher;

    public InterventionResponse execute(UUID id, ValidationRequest request) {
        Intervention intervention = interventionRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Intervention introuvable: " + id));
        User validateur = securityUtils.getCurrentUser();

        InterventionWorkflow.ValidationResult result = InterventionWorkflow.validate(
                intervention, request.approved(), request.commentaire(), validateur
        );

        Intervention saved = interventionRepository.save(intervention);

        if (result == InterventionWorkflow.ValidationResult.APPROVED) {
            eventPublisher.publishEvent(new InterventionValidatedEvent(
                    InterventionKnowledgePayload.fromIntervention(saved)
            ));
        } else {
            eventPublisher.publishEvent(new InterventionKnowledgeRemovedEvent(saved.getId()));
        }

        return interventionMapper.toResponse(saved);
    }
}
