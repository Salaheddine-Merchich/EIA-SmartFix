package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.application.dto.AiDto.AiAssistRequest;
import com.ocp.eia.config.AppProperties;
import com.ocp.eia.domain.model.Equipment;
import com.ocp.eia.domain.model.Failure;
import com.ocp.eia.domain.repository.EquipmentRepository;
import com.ocp.eia.domain.repository.FailureRepository;
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
        try {
            if (request.equipmentId() != null) {
                Equipment equipment = equipmentRepository.findById(request.equipmentId()).orElse(null);
                if (equipment != null) {
                    return SearchContext.withBoosts(
                            equipment.getId(),
                            request.failureId(),
                            equipment.getFamille(),
                            equipment.getZone(),
                            equipmentBoost(),
                            familyBoost(),
                            zoneBoost()
                    );
                }
            }

            if (request.failureId() != null) {
                Failure failure = failureRepository.findByIdWithDetails(request.failureId()).orElse(null);
                if (failure != null && failure.getEquipment() != null) {
                    Equipment equipment = failure.getEquipment();
                    return SearchContext.withBoosts(
                            equipment.getId(),
                            failure.getId(),
                            equipment.getFamille(),
                            equipment.getZone(),
                            equipmentBoost(),
                            familyBoost(),
                            zoneBoost()
                    );
                }
            }

            return SearchContext.none();
        } catch (Exception e) {
            log.warn("Erreur lors de la construction du contexte de recherche: {}", e.getMessage());
            return SearchContext.none();
        }
    }

    private double equipmentBoost() {
        return appProperties.getAi().getRag().getContext() != null
                ? appProperties.getAi().getRag().getContext().getEquipmentBoost() : 2.0;
    }

    private double familyBoost() {
        return appProperties.getAi().getRag().getContext() != null
                ? appProperties.getAi().getRag().getContext().getFamilyBoost() : 1.5;
    }

    private double zoneBoost() {
        return appProperties.getAi().getRag().getContext() != null
                ? appProperties.getAi().getRag().getContext().getZoneBoost() : 1.2;
    }
}
