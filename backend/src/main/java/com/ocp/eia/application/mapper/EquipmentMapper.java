package com.ocp.eia.application.mapper;

import com.ocp.eia.application.dto.EquipmentDto.EquipmentResponse;
import com.ocp.eia.domain.model.Equipment;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EquipmentMapper {

    @Mapping(target = "failureCount", constant = "0L")
    EquipmentResponse toResponse(Equipment equipment);
}
