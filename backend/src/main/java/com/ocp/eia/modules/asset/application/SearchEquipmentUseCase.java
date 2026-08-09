package com.ocp.eia.modules.asset.application;

import com.ocp.eia.application.dto.CommonDto.PageResponse;
import com.ocp.eia.application.dto.EquipmentDto.EquipmentResponse;
import com.ocp.eia.application.mapper.EquipmentMapper;
import com.ocp.eia.domain.model.Equipment;
import com.ocp.eia.domain.repository.EquipmentRepository;
import com.ocp.eia.domain.repository.FailureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
public class SearchEquipmentUseCase {

    private final EquipmentRepository equipmentRepository;
    private final FailureRepository failureRepository;
    private final EquipmentMapper equipmentMapper;

    public PageResponse<EquipmentResponse> execute(String search, String famille, String zone, Pageable pageable) {
        Page<Equipment> page = equipmentRepository.search(search, famille, zone, pageable);
        List<Equipment> equipmentList = page.getContent();
        Map<UUID, Long> failureCounts = loadFailureCounts(equipmentList);

        List<EquipmentResponse> content = equipmentList.stream()
                .map(equipment -> toResponseWithFailureCount(
                        equipment,
                        failureCounts.getOrDefault(equipment.getId(), 0L)
                ))
                .toList();
        return new PageResponse<>(content, page.getTotalElements(), page.getTotalPages(), page.getNumber(), page.getSize());
    }

    private Map<UUID, Long> loadFailureCounts(List<Equipment> equipmentList) {
        if (equipmentList.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = equipmentList.stream().map(Equipment::getId).toList();
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : failureRepository.countByEquipmentIds(ids)) {
            counts.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    private EquipmentResponse toResponseWithFailureCount(Equipment equipment, long count) {
        EquipmentResponse base = equipmentMapper.toResponse(equipment);
        return new EquipmentResponse(base.id(), base.code(), base.designation(), base.famille(),
                base.zone(), base.constructeur(), base.miseEnService(), count);
    }
}
