package com.ocp.eia.modules.knowledge.infrastructure.ai;

import com.ocp.eia.modules.knowledge.domain.port.LlmProviderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression("${app.knowledge.enabled:false} == true and '${app.knowledge.provider}' == 'openai'")
@Slf4j
public class OpenAiLlmAdapter implements LlmProviderPort {

    public OpenAiLlmAdapter() {
        log.warn("Provider OpenAI sélectionné — adapter LLM à implémenter (spring-ai-openai)");
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        throw new UnsupportedOperationException("Provider OpenAI non encore configuré. Utilisez app.knowledge.provider=ollama");
    }
}
