package com.ocp.eia.modules.knowledge.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Point d'entrée configuration du module Knowledge.
 * Les adapters concrets sont sélectionnés via app.knowledge.provider :
 * - ollama (défaut) : OllamaEmbeddingAdapter + OllamaLlmAdapter
 * - openai : OpenAiEmbeddingAdapter + OpenAiLlmAdapter (à implémenter)
 * - gemini : GeminiEmbeddingAdapter + GeminiLlmAdapter (à implémenter)
 */
@Configuration
@ConditionalOnProperty(name = "app.knowledge.enabled", havingValue = "true")
public class KnowledgeModuleConfiguration {

    @Bean
    @ConditionalOnMissingBean(MeterRegistry.class)
    MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }
}
