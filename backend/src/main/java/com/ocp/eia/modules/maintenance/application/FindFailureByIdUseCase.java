package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.application.dto.FailureDto.FailureResponse;
import com.ocp.eia.application.mapper.FailureMapper;
import com.ocp.eia.domain.repository.FailureRepository;
import com.ocp.eia.presentation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindFailureByIdUseCase {

    private final FailureRepository failureRepository;
    private final FailureMapper failureMapper;

    public FailureResponse execute(UUID id) {
        return failureRepository.findByIdWithDetails(id)
                .map(failureMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Panne introuvable: " + id));
    }
}
