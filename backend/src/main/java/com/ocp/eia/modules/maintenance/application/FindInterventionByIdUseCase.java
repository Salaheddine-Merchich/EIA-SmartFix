package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.application.dto.InterventionDto.InterventionResponse;
import com.ocp.eia.application.mapper.InterventionMapper;
import com.ocp.eia.domain.repository.InterventionRepository;
import com.ocp.eia.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindInterventionByIdUseCase {

    private final InterventionRepository interventionRepository;
    private final InterventionMapper interventionMapper;

    public InterventionResponse execute(UUID id) {
        return interventionRepository.findByIdWithDetails(id)
                .map(interventionMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Intervention introuvable: " + id));
    }
}
