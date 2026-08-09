package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.application.dto.InterventionDto.InterventionResponse;
import com.ocp.eia.application.mapper.InterventionMapper;
import com.ocp.eia.domain.model.Intervention;
import com.ocp.eia.domain.model.User;
import com.ocp.eia.domain.repository.InterventionRepository;
import com.ocp.eia.infrastructure.security.SecurityUtils;
import com.ocp.eia.modules.maintenance.application.event.InterventionSubmittedEvent;
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
public class SubmitInterventionUseCase {

    private final InterventionRepository interventionRepository;
    private final InterventionMapper interventionMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final SecurityUtils securityUtils;

    public InterventionResponse execute(UUID id) {
        Intervention intervention = interventionRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Intervention introuvable: " + id));
        User current = securityUtils.getCurrentUser();
        InterventionWorkflow.ensureEditable(intervention, current.getId(), current.getRole());
        InterventionWorkflow.submit(intervention);
        Intervention saved = interventionRepository.save(intervention);
        String equipmentCode = saved.getFailure() != null && saved.getFailure().getEquipment() != null
                ? saved.getFailure().getEquipment().getCode()
                : "";
        String technicienNom = saved.getTechnicien() != null ? saved.getTechnicien().getNomPrenom() : "";
        eventPublisher.publishEvent(new InterventionSubmittedEvent(saved.getId(), equipmentCode, technicienNom));
        return interventionMapper.toResponse(saved);
    }
}
