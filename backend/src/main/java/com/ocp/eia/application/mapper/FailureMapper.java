package com.ocp.eia.application.mapper;

import com.ocp.eia.application.dto.FailureDto.FailureResponse;
import com.ocp.eia.domain.model.Failure;
import com.ocp.eia.domain.model.Intervention;
import com.ocp.eia.domain.model.StatutValidation;
import org.mapstruct.*;

import java.util.Comparator;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FailureMapper {

    @Named("withInterventionStats")
    @Mapping(target = "equipmentId", source = "equipment.id")
    @Mapping(target = "equipmentCode", source = "equipment.code")
    @Mapping(target = "equipmentDesignation", source = "equipment.designation")
    @Mapping(target = "declarantId", source = "declarant.id")
    @Mapping(target = "declarantNom", source = "declarant.nomPrenom")
    @Mapping(target = "responsableId", source = "responsable.id")
    @Mapping(target = "responsableNom", source = "responsable.nomPrenom")
    @Mapping(target = "interventionCount", expression = "java(interventionCount(failure))")
    @Mapping(target = "latestInterventionStatut", expression = "java(latestInterventionStatut(failure))")
    FailureResponse toResponse(Failure failure);

    @Named("withoutInterventionStats")
    @Mapping(target = "equipmentId", source = "equipment.id")
    @Mapping(target = "equipmentCode", source = "equipment.code")
    @Mapping(target = "equipmentDesignation", source = "equipment.designation")
    @Mapping(target = "declarantId", source = "declarant.id")
    @Mapping(target = "declarantNom", source = "declarant.nomPrenom")
    @Mapping(target = "responsableId", source = "responsable.id")
    @Mapping(target = "responsableNom", source = "responsable.nomPrenom")
    @Mapping(target = "interventionCount", constant = "0")
    @Mapping(target = "latestInterventionStatut", ignore = true)
    FailureResponse toResponseWithoutInterventionStats(Failure failure);

    @IterableMapping(qualifiedByName = "withInterventionStats")
    List<FailureResponse> toResponseList(List<Failure> failures);

    default int interventionCount(Failure failure) {
        return failure.getInterventions() != null ? failure.getInterventions().size() : 0;
    }

    default StatutValidation latestInterventionStatut(Failure failure) {
        if (failure.getInterventions() == null || failure.getInterventions().isEmpty()) {
            return null;
        }
        return failure.getInterventions().stream()
                .max(Comparator.comparing(Intervention::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(Intervention::getStatutValidation)
                .orElse(null);
    }
}
