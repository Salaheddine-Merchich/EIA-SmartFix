package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.application.dto.CommonDto.PageResponse;
import com.ocp.eia.application.dto.FailureDto.FailureResponse;
import com.ocp.eia.application.mapper.FailureMapper;
import com.ocp.eia.domain.model.Criticite;
import com.ocp.eia.domain.model.StatutPanne;
import com.ocp.eia.domain.repository.FailureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListFailuresUseCase {

    private final FailureRepository failureRepository;
    private final FailureMapper failureMapper;

    public PageResponse<FailureResponse> execute(UUID equipmentId, StatutPanne statut, Criticite criticite,
                                                 String codeDefaut, String search, Pageable pageable) {
        var page = failureRepository.search(equipmentId, statut, criticite, codeDefaut, search, pageable);
        return new PageResponse<>(
                failureMapper.toResponseList(page.getContent()),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }
}
