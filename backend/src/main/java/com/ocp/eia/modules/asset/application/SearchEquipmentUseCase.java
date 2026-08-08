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

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchEquipmentUseCase {

    private final EquipmentRepository equipmentRepository;
    private final FailureRepository failureRepository;
    private final EquipmentMapper equipmentMapper;

    public PageResponse<EquipmentResponse> execute(String search, String famille, String zone, Pageable pageable) {
        Page<Equipment> page = equipmentRepository.search(search, famille, zone, pageable);
        List<EquipmentResponse> content = page.getContent().stream()
                .map(this::toResponseWithFailureCount)
                .toList();
        return new PageResponse<>(content, page.getTotalElements(), page.getTotalPages(), page.getNumber(), page.getSize());
    }

    private EquipmentResponse toResponseWithFailureCount(Equipment equipment) {
        long count = failureRepository.findByEquipmentIdOrderByDateHeureDesc(equipment.getId()).size();
        EquipmentResponse base = equipmentMapper.toResponse(equipment);
        return new EquipmentResponse(base.id(), base.code(), base.designation(), base.famille(),
                base.zone(), base.constructeur(), base.miseEnService(), count);
    }
}
