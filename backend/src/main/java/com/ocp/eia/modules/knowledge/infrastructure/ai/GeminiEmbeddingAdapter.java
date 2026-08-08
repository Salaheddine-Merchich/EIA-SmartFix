package com.ocp.eia.modules.knowledge.infrastructure.ai;

import com.ocp.eia.modules.knowledge.domain.port.EmbeddingProviderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * Stub Gemini — implémenter avec spring-ai-vertex-gemini quand disponible.
 */
@Component
@ConditionalOnExpression("${app.knowledge.enabled:false} == true and '${app.knowledge.provider}' == 'gemini'")
@Slf4j
public class GeminiEmbeddingAdapter implements EmbeddingProviderPort {

    public GeminiEmbeddingAdapter() {
        log.warn("Provider Gemini sélectionné — adapter embedding à implémenter");
    }

    @Override
    public float[] embed(String text) {
        throw new UnsupportedOperationException("Provider Gemini non encore configuré. Utilisez app.knowledge.provider=ollama");
    }
}
