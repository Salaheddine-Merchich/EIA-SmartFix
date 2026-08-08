package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.application.dto.CommonDto.PageResponse;
import com.ocp.eia.application.dto.InterventionDto.InterventionResponse;
import com.ocp.eia.application.mapper.InterventionMapper;
import com.ocp.eia.domain.repository.InterventionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindInterventionsByFailureUseCase {

    private final InterventionRepository interventionRepository;
    private final InterventionMapper interventionMapper;

    public PageResponse<InterventionResponse> execute(UUID failureId, Pageable pageable) {
        var page = interventionRepository.findByFailureIdWithDocuments(failureId, pageable);
        return new PageResponse<>(
                interventionMapper.toResponseList(page.getContent()),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }
}
