package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.application.dto.CommonDto.PageResponse;
import com.ocp.eia.application.dto.InterventionDto.InterventionResponse;
import com.ocp.eia.application.mapper.InterventionMapper;
import com.ocp.eia.domain.model.Intervention;
import com.ocp.eia.domain.repository.InterventionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindInterventionsByFailureUseCase {

    private final InterventionRepository interventionRepository;
    private final InterventionMapper interventionMapper;

    public PageResponse<InterventionResponse> execute(UUID failureId, Pageable pageable) {
        Page<Intervention> page = interventionRepository.findByFailureId(failureId, pageable);
        List<Intervention> hydrated = hydrateWithDetails(page.getContent());
        return new PageResponse<>(
                interventionMapper.toResponseList(hydrated),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }

    private List<Intervention> hydrateWithDetails(List<Intervention> pageContent) {
        if (pageContent.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = pageContent.stream().map(Intervention::getId).toList();
        Map<UUID, Intervention> byId = interventionRepository.findAllByIdWithDetails(ids).stream()
                .collect(Collectors.toMap(Intervention::getId, Function.identity(), (a, b) -> a));
        return ids.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .toList();
    }
}
