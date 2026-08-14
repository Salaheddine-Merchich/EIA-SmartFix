package com.ocp.eia.modules.knowledge.config;

import com.ocp.eia.config.AppProperties;
import com.ocp.eia.modules.knowledge.application.ReindexKnowledgeUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Réindexe automatiquement les interventions validées sans embedding (ex. seed Flyway V23).
 */
@Configuration
@ConditionalOnProperty(name = "app.knowledge.enabled", havingValue = "true")
@ConditionalOnBean(ReindexKnowledgeUseCase.class)
@RequiredArgsConstructor
@Slf4j
public class KnowledgeStartupReindexConfiguration {

    private final AppProperties appProperties;
    private final JdbcTemplate jdbcTemplate;
    private final ReindexKnowledgeUseCase reindexKnowledgeUseCase;

    @Bean
    ApplicationRunner reindexMissingInterventionsOnStartup() {
        return args -> {
            if (!appProperties.getKnowledge().isReindexOnStartup()) {
                return;
            }

            Integer validatedCount = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM interventions
                    WHERE statut_validation = 'VALIDEE'
                    """, Integer.class);

            Integer indexedCount = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM intervention_embeddings ie
                    JOIN interventions i ON i.id = ie.intervention_id
                    WHERE i.statut_validation = 'VALIDEE'
                    """, Integer.class);

            int validated = validatedCount != null ? validatedCount : 0;
            int indexed = indexedCount != null ? indexedCount : 0;

            if (validated == 0) {
                return;
            }

            if (indexed >= validated) {
                log.info("RAG startup: {}/{} interventions validées déjà indexées", indexed, validated);
                return;
            }

            log.info("RAG startup: réindexation auto ({}/{} embeddings manquants)", validated - indexed, validated);
            var result = reindexKnowledgeUseCase.execute();
            log.info("RAG startup reindex done: processed={}, indexed={}, skipped={}, errors={}",
                    result.processed(), result.indexed(), result.skipped(), result.errors());
        };
    }
}
