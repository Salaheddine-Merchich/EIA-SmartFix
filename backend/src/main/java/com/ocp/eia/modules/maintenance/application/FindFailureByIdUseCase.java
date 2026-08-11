package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.application.dto.FailureDto.FailureResponse;
import com.ocp.eia.application.mapper.FailureMapper;
import com.ocp.eia.domain.model.Failure;
import com.ocp.eia.domain.model.StatutValidation;
import com.ocp.eia.domain.repository.FailureRepository;
import com.ocp.eia.domain.repository.InterventionRepository;
import com.ocp.eia.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindFailureByIdUseCase {

    private final FailureRepository failureRepository;
    private final InterventionRepository interventionRepository;
    private final FailureMapper failureMapper;

    public FailureResponse execute(UUID id) {
        Failure failure = failureRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Panne introuvable: " + id));

        Map<UUID, Integer> interventionCounts = loadInterventionCounts(List.of(failure));
        Map<UUID, StatutValidation> latestStatuts = loadLatestStatuts(List.of(failure));

        FailureResponse base = failureMapper.toResponseWithoutInterventionStats(failure);
        return withInterventionStats(
                base,
                interventionCounts.getOrDefault(failure.getId(), 0),
                latestStatuts.get(failure.getId())
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
