package com.ocp.eia.modules.knowledge.config;

import com.ocp.eia.domain.model.StatutValidation;
import com.ocp.eia.domain.repository.InterventionRepository;
import com.ocp.eia.modules.knowledge.application.IndexInterventionUseCase;
import com.ocp.eia.modules.knowledge.application.IndexKnowledgeDocumentUseCase;
import com.ocp.eia.modules.maintenance.application.event.InterventionKnowledgePayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
@ConditionalOnBean(IndexInterventionUseCase.class)
@RequiredArgsConstructor
@Slf4j
public class KnowledgeDevReindexConfiguration {

    private final InterventionRepository interventionRepository;
    private final IndexInterventionUseCase indexInterventionUseCase;
    private final IndexKnowledgeDocumentUseCase indexKnowledgeDocumentUseCase;

    @Bean
    CommandLineRunner reindexValidatedInterventions() {
        return args -> {
            var validated = interventionRepository.findByStatutValidationWithDetails(StatutValidation.VALIDEE);
            if (validated.isEmpty()) {
                return;
            }
            log.info("Réindexation dev de {} intervention(s) validée(s) pour le RAG", validated.size());
            validated.forEach(i -> indexInterventionUseCase.index(InterventionKnowledgePayload.fromIntervention(i)));
        };
    }
    
    @Bean
    CommandLineRunner indexKnowledgeDocuments() {
        return args -> {
            log.info("Indexation dev des documents de connaissance pour la recherche vectorielle");
            IndexKnowledgeDocumentUseCase.IndexResults results = indexKnowledgeDocumentUseCase.indexAllDocuments();
            log.info("Indexation des documents terminée: {} indexés, {} ignorés, {} échecs", 
                    results.indexed(), results.skipped(), results.failed());
        };
    }
}
