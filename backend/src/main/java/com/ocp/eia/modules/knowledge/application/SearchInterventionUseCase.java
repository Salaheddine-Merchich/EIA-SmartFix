package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.application.dto.DashboardDto.SearchResponse;
import com.ocp.eia.application.mapper.InterventionMapper;
import com.ocp.eia.domain.model.StatutValidation;
import com.ocp.eia.domain.repository.InterventionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchInterventionUseCase {

    private final InterventionRepository interventionRepository;
    private final InterventionMapper interventionMapper;

    public SearchResponse execute(String q, UUID equipmentId, String symptom, String faultCode,
                                  StatutValidation statut, Pageable pageable) {
        String textQuery = buildTextQuery(q, symptom);
        String equipmentParam = equipmentId != null ? equipmentId.toString() : null;
        String statutParam = statut != null ? statut.name() : null;
        String faultCodeParam = normalizeFilter(faultCode);

        var page = interventionRepository.fullTextSearch(
                textQuery,
                equipmentParam,
                faultCodeParam,
                statutParam,
                pageable
        );

        return new SearchResponse(
                interventionMapper.toResponseList(page.getContent()),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber()
        );
    }

    private String buildTextQuery(String q, String symptom) {
        StringBuilder sb = new StringBuilder();
        if (q != null && !q.isBlank()) sb.append(q.trim()).append(' ');
        if (symptom != null && !symptom.isBlank()) sb.append(symptom.trim());
        String result = sb.toString().trim();
        return result.isEmpty() ? null : result;
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
