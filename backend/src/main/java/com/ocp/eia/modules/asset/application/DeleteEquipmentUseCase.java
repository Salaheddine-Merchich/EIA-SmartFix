package com.ocp.eia.modules.asset.application;

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
public class DeleteEquipmentUseCase {

    private final EquipmentRepository equipmentRepository;

    public void execute(UUID id) {
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Équipement introuvable: " + id));
        if (!equipment.getFailures().isEmpty()) {
            throw new ConflictException("Impossible de supprimer un équipement avec des pannes associées");
        }
        equipmentRepository.delete(equipment);
    }
}
