package com.ocp.eia.modules.knowledge.infrastructure.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression("${app.knowledge.enabled:false} == true and '${app.knowledge.provider:ollama}' == 'ollama'")
@RequiredArgsConstructor
@Slf4j
public class OllamaEmbeddingAdapter {

    private final OllamaEmbeddingModel embeddingModel;

    public float[] embed(String text) {
        int textLength = text != null ? text.length() : 0;
        long start = System.nanoTime();
        float[] embedding = embeddingModel.embed(text);
        long durationMs = (System.nanoTime() - start) / 1_000_000L;
        int dims = embedding != null ? embedding.length : 0;
        log.info("Ollama embedding done: textLength={}, dims={}, durationMs={}", textLength, dims, durationMs);
        if (log.isDebugEnabled() && text != null) {
            String snippet = text.length() <= 200 ? text : text.substring(0, 200) + "...";
            log.debug("Ollama embedding text snippet: {}", snippet);
        }
        return embedding;
    }
}
