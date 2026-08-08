package com.ocp.eia.modules.asset.application;

import com.ocp.eia.application.dto.EquipmentDto.EquipmentResponse;
import com.ocp.eia.application.mapper.EquipmentMapper;
import com.ocp.eia.domain.model.Equipment;
import com.ocp.eia.domain.repository.EquipmentRepository;
import com.ocp.eia.domain.repository.FailureRepository;
import com.ocp.eia.presentation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindEquipmentByIdUseCase {

    private final EquipmentRepository equipmentRepository;
    private final FailureRepository failureRepository;
    private final EquipmentMapper equipmentMapper;

    public EquipmentResponse execute(UUID id) {
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Équipement introuvable: " + id));
        long count = failureRepository.findByEquipmentIdOrderByDateHeureDesc(id).size();
        EquipmentResponse base = equipmentMapper.toResponse(equipment);
        return new EquipmentResponse(base.id(), base.code(), base.designation(), base.famille(),
                base.zone(), base.constructeur(), base.miseEnService(), count);
    }
}
