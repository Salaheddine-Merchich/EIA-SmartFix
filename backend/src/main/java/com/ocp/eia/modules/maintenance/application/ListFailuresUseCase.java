package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.application.dto.CommonDto.PageResponse;
import com.ocp.eia.application.dto.FailureDto.FailureResponse;
import com.ocp.eia.application.mapper.FailureMapper;
import com.ocp.eia.domain.model.Criticite;
import com.ocp.eia.domain.model.Failure;
import com.ocp.eia.domain.model.StatutPanne;
import com.ocp.eia.domain.model.StatutValidation;
import com.ocp.eia.domain.repository.FailureRepository;
import com.ocp.eia.domain.repository.InterventionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListFailuresUseCase {

    private final FailureRepository failureRepository;
    private final InterventionRepository interventionRepository;
    private final FailureMapper failureMapper;

    public PageResponse<FailureResponse> execute(UUID equipmentId, StatutPanne statut, Criticite criticite,
                                                 String codeDefaut, String search, Pageable pageable) {
        var page = failureRepository.search(equipmentId, statut, criticite, codeDefaut, search, pageable);
        List<Failure> failures = page.getContent();
        Map<UUID, Integer> interventionCounts = loadInterventionCounts(failures);
        Map<UUID, StatutValidation> latestStatuts = loadLatestStatuts(failures);

        List<FailureResponse> content = failures.stream()
                .map(failure -> withInterventionStats(
                        failureMapper.toResponseWithoutInterventionStats(failure),
                        interventionCounts.getOrDefault(failure.getId(), 0),
                        latestStatuts.get(failure.getId())
                ))
                .toList();

        return new PageResponse<>(
                content,
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }

    private Map<UUID, Integer> loadInterventionCounts(List<Failure> failures) {
        if (failures.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = failures.stream().map(Failure::getId).toList();
        Map<UUID, Integer> counts = new HashMap<>();
        for (Object[] row : interventionRepository.countByFailureIds(ids)) {
            counts.put((UUID) row[0], ((Number) row[1]).intValue());
        }
        return counts;
    }

    private Map<UUID, StatutValidation> loadLatestStatuts(List<Failure> failures) {
        if (failures.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = failures.stream().map(Failure::getId).toList();
        Map<UUID, StatutValidation> latest = new HashMap<>();
        for (Object[] row : interventionRepository.findLatestStatutByFailureIds(ids)) {
            latest.putIfAbsent((UUID) row[0], (StatutValidation) row[1]);
        }
        return latest;
    }

    private FailureResponse withInterventionStats(FailureResponse base, int interventionCount,
                                                  StatutValidation latestInterventionStatut) {
        return new FailureResponse(
                base.id(),
                base.equipmentId(),
                base.equipmentCode(),
                base.equipmentDesignation(),
                base.dateHeure(),
                base.criticite(),
                base.zoneService(),
                base.declarantId(),
                base.declarantNom(),
                base.responsableId(),
                base.responsableNom(),
                base.statut(),
                base.descriptionInitiale(),
                base.codeDefaut(),
                interventionCount,
                latestInterventionStatut
        );
    }
}
