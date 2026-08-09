package com.ocp.eia.modules.asset.application;

import com.ocp.eia.application.dto.EquipmentDto.EquipmentRequest;
import com.ocp.eia.application.dto.EquipmentDto.EquipmentResponse;
import com.ocp.eia.application.mapper.EquipmentMapper;
import com.ocp.eia.domain.model.Equipment;
import com.ocp.eia.domain.repository.EquipmentRepository;
import com.ocp.eia.shared.exception.ConflictException;
import com.ocp.eia.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateEquipmentUseCase {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentMapper equipmentMapper;

    public EquipmentResponse execute(UUID id, EquipmentRequest request) {
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Équipement introuvable: " + id));
        if (!equipment.getCode().equals(request.code()) && equipmentRepository.existsByCode(request.code())) {
            throw new ConflictException("Un équipement avec ce code existe déjà");
        }
        equipment.setCode(request.code());
        equipment.setDesignation(request.designation());
        equipment.setFamille(request.famille());
        equipment.setZone(request.zone());
        equipment.setConstructeur(request.constructeur());
        equipment.setMiseEnService(request.miseEnService());
        return equipmentMapper.toResponse(equipmentRepository.save(equipment));
    }
}
