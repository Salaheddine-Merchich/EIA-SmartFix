package com.ocp.eia.modules.knowledge.config;

import com.ocp.eia.modules.knowledge.infrastructure.observability.RagObservabilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@ConditionalOnProperty(name = "app.knowledge.enabled", havingValue = "true")
@ConditionalOnBean(RagObservabilityService.class)
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class RagObservabilityConfig {

    private final RagObservabilityService ragObservabilityService;

    @Bean
    CommandLineRunner initRagObservability() {
        return args -> {
            log.info("Initialisation des métriques d'observabilité RAG");
            ragObservabilityService.init();
            log.info("Métriques RAG prêtes pour monitoring et alerting");
        };
    }
}