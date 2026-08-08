package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.application.dto.FailureDto.FailureRequest;
import com.ocp.eia.application.dto.FailureDto.FailureResponse;
import com.ocp.eia.application.mapper.FailureMapper;
import com.ocp.eia.domain.model.*;
import com.ocp.eia.domain.repository.EquipmentRepository;
import com.ocp.eia.domain.repository.FailureRepository;
import com.ocp.eia.domain.repository.UserRepository;
import com.ocp.eia.presentation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateFailureUseCase {

    private final FailureRepository failureRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final FailureMapper failureMapper;

    public FailureResponse execute(UUID id, FailureRequest request) {
        Failure failure = failureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Panne introuvable: " + id));

        Equipment equipment = equipmentRepository.findById(request.equipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Équipement introuvable: " + request.equipmentId()));

        User responsable = null;
        if (request.responsableId() != null) {
            responsable = userRepository.findById(request.responsableId())
                    .orElseThrow(() -> new ResourceNotFoundException("Responsable introuvable: " + request.responsableId()));
        }

        failure.setEquipment(equipment);
        failure.setDateHeure(request.dateHeure());
        failure.setCriticite(request.criticite());
        failure.setZoneService(request.zoneService());
        failure.setResponsable(responsable);
        if (request.statut() != null) {
            failure.setStatut(request.statut());
        }
        failure.setDescriptionInitiale(request.descriptionInitiale());
        failure.setCodeDefaut(request.codeDefaut());

        return failureMapper.toResponse(failureRepository.save(failure));
    }
}
