package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.application.dto.KnowledgeDto.ReindexResponse;
import com.ocp.eia.domain.model.StatutValidation;
import com.ocp.eia.domain.repository.InterventionRepository;
import com.ocp.eia.modules.knowledge.application.IndexInterventionUseCase.IndexOutcome;
import com.ocp.eia.modules.maintenance.application.event.InterventionKnowledgePayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.knowledge.enabled", havingValue = "true")
@ConditionalOnBean(IndexInterventionUseCase.class)
@RequiredArgsConstructor
@Slf4j
public class ReindexKnowledgeUseCase {

    private final InterventionRepository interventionRepository;
    private final IndexInterventionUseCase indexInterventionUseCase;

    public ReindexResponse execute() {
        var validated = interventionRepository.findByStatutValidationWithDetails(StatutValidation.VALIDEE);

        int processed = 0;
        int indexed = 0;
        int skipped = 0;
        int errors = 0;

        for (var intervention : validated) {
            processed++;
            IndexOutcome outcome = indexInterventionUseCase.index(
                    InterventionKnowledgePayload.fromIntervention(intervention)
            );
            switch (outcome) {
                case INDEXED -> indexed++;
                case SKIPPED -> skipped++;
                case FAILED -> errors++;
            }
        }

        log.info("Réindexation RAG terminée : {} traitée(s), {} indexée(s), {} ignorée(s), {} erreur(s)",
                processed, indexed, skipped, errors);

        return new ReindexResponse(processed, indexed, skipped, errors);
    }
}
