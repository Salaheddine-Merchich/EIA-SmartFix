package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.application.dto.FailureDto.FailureRequest;
import com.ocp.eia.application.dto.FailureDto.FailureResponse;
import com.ocp.eia.application.mapper.FailureMapper;
import com.ocp.eia.domain.model.*;
import com.ocp.eia.domain.repository.EquipmentRepository;
import com.ocp.eia.domain.repository.FailureRepository;
import com.ocp.eia.domain.repository.UserRepository;
import com.ocp.eia.infrastructure.security.SecurityUtils;
import com.ocp.eia.modules.maintenance.application.event.FailureCreatedEvent;
import com.ocp.eia.presentation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateFailureUseCase {

    private final FailureRepository failureRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final FailureMapper failureMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final SecurityUtils securityUtils;

    public FailureResponse execute(FailureRequest request) {
        User declarant = securityUtils.getCurrentUser();
        Equipment equipment = equipmentRepository.findById(request.equipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Équipement introuvable: " + request.equipmentId()));

        User responsable = null;
        if (request.responsableId() != null) {
            responsable = userRepository.findById(request.responsableId())
                    .orElseThrow(() -> new ResourceNotFoundException("Responsable introuvable: " + request.responsableId()));
        }

        Failure failure = Failure.builder()
                .equipment(equipment)
                .dateHeure(request.dateHeure())
                .criticite(request.criticite())
                .zoneService(request.zoneService())
                .declarant(declarant)
                .responsable(responsable)
                .statut(request.statut() != null ? request.statut() : StatutPanne.OUVERTE)
                .descriptionInitiale(request.descriptionInitiale())
                .codeDefaut(request.codeDefaut())
                .build();

        Failure saved = failureRepository.save(failure);
        eventPublisher.publishEvent(new FailureCreatedEvent(
                saved.getId(),
                equipment.getCode(),
                saved.getCriticite() != null ? saved.getCriticite().name() : null,
                saved.getDescriptionInitiale()
        ));
        return failureMapper.toResponse(saved);
    }
}
