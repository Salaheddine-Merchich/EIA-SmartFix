package com.ocp.eia.modules.asset.application;

import com.ocp.eia.application.dto.FailureDto.FailureResponse;
import com.ocp.eia.application.dto.InterventionDto.InterventionResponse;
import com.ocp.eia.application.mapper.FailureMapper;
import com.ocp.eia.application.mapper.InterventionMapper;
import com.ocp.eia.domain.model.Failure;
import com.ocp.eia.domain.model.Intervention;
import com.ocp.eia.domain.repository.EquipmentRepository;
import com.ocp.eia.domain.repository.FailureRepository;
import com.ocp.eia.domain.repository.InterventionRepository;
import com.ocp.eia.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetEquipmentHistoryUseCase {

    private final EquipmentRepository equipmentRepository;
    private final FailureRepository failureRepository;
    private final InterventionRepository interventionRepository;
    private final FailureMapper failureMapper;
    private final InterventionMapper interventionMapper;

    public EquipmentHistoryResponse execute(UUID id) {
        if (!equipmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Équipement introuvable: " + id);
        }
        List<Failure> failures = failureRepository.findByEquipmentIdWithDetails(id);
        List<Intervention> interventions = interventionRepository.findByFailureEquipmentIdWithDetails(id);
        return new EquipmentHistoryResponse(
                failureMapper.toResponseList(failures),
                interventionMapper.toResponseList(interventions)
        );
    }

    public record EquipmentHistoryResponse(
            List<FailureResponse> failures,
            List<InterventionResponse> interventions
    ) {}
}
