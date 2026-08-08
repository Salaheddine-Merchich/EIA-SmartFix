package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.application.dto.InterventionDto.InterventionResponse;
import com.ocp.eia.application.dto.InterventionDto.InterventionUpdateRequest;
import com.ocp.eia.application.mapper.InterventionMapper;
import com.ocp.eia.domain.model.Intervention;
import com.ocp.eia.domain.model.User;
import com.ocp.eia.domain.repository.InterventionRepository;
import com.ocp.eia.infrastructure.security.SecurityUtils;
import com.ocp.eia.modules.maintenance.domain.service.InterventionWorkflow;
import com.ocp.eia.presentation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateInterventionUseCase {

    private final InterventionRepository interventionRepository;
    private final InterventionMapper interventionMapper;
    private final SecurityUtils securityUtils;

    public InterventionResponse execute(UUID id, InterventionUpdateRequest request) {
        Intervention intervention = interventionRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Intervention introuvable: " + id));
        User current = securityUtils.getCurrentUser();
        InterventionWorkflow.ensureEditable(intervention, current.getId(), current.getRole());

        intervention.setDescription(request.description());
        intervention.setSymptomes(request.symptomes());
        intervention.setCauseRacine(request.causeRacine());
        intervention.setAnalyseTechnique(request.analyseTechnique());
        intervention.setActionsCorrectives(request.actionsCorrectives());
        intervention.setPiecesRemplacees(request.piecesRemplacees());
        intervention.setDureeArretMinutes(request.dureeArretMinutes());
        intervention.setTempsInterventionMinutes(request.tempsInterventionMinutes());

        return interventionMapper.toResponse(interventionRepository.save(intervention));
    }
}
