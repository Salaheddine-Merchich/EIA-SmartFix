package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.application.dto.AiDto.AiAssistRequest;
import com.ocp.eia.config.AppProperties;
import com.ocp.eia.domain.model.Equipment;
import com.ocp.eia.domain.model.Failure;
import com.ocp.eia.domain.repository.EquipmentRepository;
import com.ocp.eia.domain.repository.FailureRepository;
import com.ocp.eia.modules.knowledge.domain.model.QuerySignals;
import com.ocp.eia.modules.knowledge.domain.model.SearchContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.knowledge.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class SearchContextFactory {

    private final AppProperties appProperties;
    private final EquipmentRepository equipmentRepository;
    private final FailureRepository failureRepository;

    public SearchContext from(AiAssistRequest request) {
        return from(request, QuerySignals.empty());
    }

    public SearchContext from(AiAssistRequest request, QuerySignals signals) {
        try {
            if (request.equipmentId() != null) {
                Equipment equipment = equipmentRepository.findById(request.equipmentId()).orElse(null);
                if (equipment != null) {
                    return buildContext(
                            equipment.getId(),
                            request.failureId(),
                            equipment.getFamille(),
                            equipment.getZone(),
                            signals
                    );
                }
            }

            if (request.failureId() != null) {
                Failure failure = failureRepository.findByIdWithDetails(request.failureId()).orElse(null);
                if (failure != null && failure.getEquipment() != null) {
                    Equipment equipment = failure.getEquipment();
                    return buildContext(
                            equipment.getId(),
                            failure.getId(),
                            equipment.getFamille(),
                            equipment.getZone(),
                            signals
                    );
                }
            }

            return buildContextFromSignals(signals);
        } catch (Exception e) {
            log.warn("Erreur lors de la construction du contexte de recherche: {}", e.getMessage());
            return SearchContext.none();
        }
    }

    private SearchContext buildContextFromSignals(QuerySignals signals) {
        String family = signals.equipmentFamily().orElse(null);
        String zone = signals.equipmentZone().orElse(null);
        return buildContext(null, null, family, zone, signals);
    }

    private SearchContext buildContext(
            java.util.UUID equipmentId,
            java.util.UUID failureId,
            String family,
            String zone,
            QuerySignals signals
    ) {
        if (family == null || family.isBlank()) {
            family = signals.equipmentFamily().orElse(null);
        }
        if (zone == null || zone.isBlank()) {
            zone = signals.equipmentZone().orElse(null);
        }

        String manufacturer = signals.manufacturer().orElse(null);
        return SearchContext.withSignals(
                equipmentId,
                failureId,
                family,
                zone,
                manufacturer,
                signals.faultCodes(),
                equipmentBoost(),
                familyBoost(),
                zoneBoost(),
                manufacturerBoost()
        );
    }

    private double equipmentBoost() {
        return appProperties.getAi().getRag().getContext() != null
                ? appProperties.getAi().getRag().getContext().getEquipmentBoost() : 2.0;
    }

    private double familyBoost() {
        return appProperties.getAi().getRag().getContext() != null
                ? appProperties.getAi().getRag().getContext().getFamilyBoost() : 1.6;
    }

    private double zoneBoost() {
        return appProperties.getAi().getRag().getContext() != null
                ? appProperties.getAi().getRag().getContext().getZoneBoost() : 1.8;
    }

    private double manufacturerBoost() {
        return appProperties.getAi().getRag().getContext() != null
                ? appProperties.getAi().getRag().getContext().getManufacturerBoost() : 1.8;
    }
}
