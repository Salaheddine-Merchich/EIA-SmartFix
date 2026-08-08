package com.ocp.eia.modules.asset.application;

import com.ocp.eia.application.dto.EquipmentDto.EquipmentRequest;
import com.ocp.eia.application.dto.EquipmentDto.EquipmentResponse;
import com.ocp.eia.application.mapper.EquipmentMapper;
import com.ocp.eia.domain.model.Equipment;
import com.ocp.eia.domain.repository.EquipmentRepository;
import com.ocp.eia.presentation.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateEquipmentUseCase {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentMapper equipmentMapper;

    public EquipmentResponse execute(EquipmentRequest request) {
        if (equipmentRepository.existsByCode(request.code())) {
            throw new ConflictException("Un équipement avec ce code existe déjà");
        }
        Equipment equipment = Equipment.builder()
                .code(request.code())
                .designation(request.designation())
                .famille(request.famille())
                .zone(request.zone())
                .constructeur(request.constructeur())
                .miseEnService(request.miseEnService())
                .build();
        return equipmentMapper.toResponse(equipmentRepository.save(equipment));
    }
}
