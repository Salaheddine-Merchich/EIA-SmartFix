package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.application.dto.InterventionDto.InterventionRequest;
import com.ocp.eia.application.dto.InterventionDto.InterventionResponse;
import com.ocp.eia.application.mapper.InterventionMapper;
import com.ocp.eia.domain.model.Failure;
import com.ocp.eia.domain.model.Intervention;
import com.ocp.eia.domain.model.StatutValidation;
import com.ocp.eia.domain.model.User;
import com.ocp.eia.domain.repository.FailureRepository;
import com.ocp.eia.domain.repository.InterventionRepository;
import com.ocp.eia.infrastructure.security.SecurityUtils;
import com.ocp.eia.modules.maintenance.application.event.InterventionCreatedEvent;
import com.ocp.eia.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateInterventionUseCase {

    private final InterventionRepository interventionRepository;
    private final FailureRepository failureRepository;
    private final InterventionMapper interventionMapper;
    private final SecurityUtils securityUtils;
    private final ApplicationEventPublisher eventPublisher;

    public InterventionResponse execute(InterventionRequest request) {
        Failure failure = failureRepository.findById(request.failureId())
                .orElseThrow(() -> new ResourceNotFoundException("Panne introuvable: " + request.failureId()));
        User technicien = securityUtils.getCurrentUser();

        Intervention intervention = Intervention.builder()
                .failure(failure)
                .technicien(technicien)
                .description(request.description())
                .symptomes(request.symptomes())
                .causeRacine(request.causeRacine())
                .analyseTechnique(request.analyseTechnique())
                .actionsCorrectives(request.actionsCorrectives())
                .piecesRemplacees(request.piecesRemplacees())
                .dureeArretMinutes(request.dureeArretMinutes())
                .tempsInterventionMinutes(request.tempsInterventionMinutes())
                .statutValidation(StatutValidation.BROUILLON)
                .build();

        Intervention saved = interventionRepository.save(intervention);
        String equipmentCode = failure.getEquipment() != null ? failure.getEquipment().getCode() : "";
        eventPublisher.publishEvent(new InterventionCreatedEvent(
                saved.getId(),
                failure.getId(),
                equipmentCode,
                technicien.getNomPrenom()
        ));
        return interventionMapper.toResponse(saved);
    }
}
