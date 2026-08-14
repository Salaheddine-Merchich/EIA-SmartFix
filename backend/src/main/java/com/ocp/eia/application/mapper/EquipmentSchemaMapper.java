package com.ocp.eia.application.mapper;

import com.ocp.eia.application.dto.EquipmentDto.EquipmentSchemaResponse;
import com.ocp.eia.domain.model.EquipmentSchema;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Arrays;
import java.util.List;

@Mapper(componentModel = "spring")
public interface EquipmentSchemaMapper {

    @Mapping(target = "equipmentId", source = "equipment.id")
    @Mapping(target = "equipmentCode", source = "equipment.code")
    @Mapping(target = "triggerKeywords", expression = "java(toKeywordList(schema.getTriggerKeywords()))")
    EquipmentSchemaResponse toResponse(EquipmentSchema schema);

    default List<String> toKeywordList(String[] keywords) {
        if (keywords == null) {
            return List.of();
        }
        return Arrays.asList(keywords);
    }
}
