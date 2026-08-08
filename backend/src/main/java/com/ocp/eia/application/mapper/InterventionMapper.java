package com.ocp.eia.application.mapper;

import com.ocp.eia.application.dto.InterventionDto.DocumentResponse;
import com.ocp.eia.application.dto.InterventionDto.InterventionResponse;
import com.ocp.eia.domain.model.Intervention;
import com.ocp.eia.domain.model.InterventionDocument;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InterventionMapper {

    @Mapping(target = "failureId", source = "failure.id")
    @Mapping(target = "equipmentCode", source = "failure.equipment.code")
    @Mapping(target = "technicienId", source = "technicien.id")
    @Mapping(target = "technicienNom", source = "technicien.nomPrenom")
    @Mapping(target = "validateurId", source = "validateur.id")
    @Mapping(target = "validateurNom", source = "validateur.nomPrenom")
    @Mapping(target = "documents", source = "documents")
    InterventionResponse toResponse(Intervention intervention);

    List<InterventionResponse> toResponseList(List<Intervention> interventions);

    DocumentResponse toDocumentResponse(InterventionDocument document);
}
