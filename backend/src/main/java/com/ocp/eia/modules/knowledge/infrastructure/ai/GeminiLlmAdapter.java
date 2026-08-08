package com.ocp.eia.modules.knowledge.infrastructure.ai;

import com.ocp.eia.modules.knowledge.domain.port.LlmProviderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression("${app.knowledge.enabled:false} == true and '${app.knowledge.provider}' == 'gemini'")
@Slf4j
public class GeminiLlmAdapter implements LlmProviderPort {

    public GeminiLlmAdapter() {
        log.warn("Provider Gemini sélectionné — adapter LLM à implémenter");
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        throw new UnsupportedOperationException("Provider Gemini non encore configuré. Utilisez app.knowledge.provider=ollama");
    }
}
