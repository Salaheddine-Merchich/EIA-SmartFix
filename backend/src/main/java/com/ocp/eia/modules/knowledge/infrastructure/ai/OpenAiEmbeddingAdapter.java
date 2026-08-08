package com.ocp.eia.modules.knowledge.infrastructure.ai;

import com.ocp.eia.modules.knowledge.domain.port.EmbeddingProviderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * Stub OpenAI — implémenter avec spring-ai-openai quand le contrat OCP le permettra.
 */
@Component
@ConditionalOnExpression("${app.knowledge.enabled:false} == true and '${app.knowledge.provider}' == 'openai'")
@Slf4j
public class OpenAiEmbeddingAdapter implements EmbeddingProviderPort {

    public OpenAiEmbeddingAdapter() {
        log.warn("Provider OpenAI sélectionné — adapter embedding à implémenter (spring-ai-openai)");
    }

    @Override
    public float[] embed(String text) {
        throw new UnsupportedOperationException("Provider OpenAI non encore configuré. Utilisez app.knowledge.provider=ollama");
    }
}
