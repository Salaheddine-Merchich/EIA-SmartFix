package com.ocp.eia.modules.knowledge.infrastructure.ai;

import com.ocp.eia.modules.knowledge.domain.port.LlmProviderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicInteger;

@Component
@ConditionalOnExpression("${app.knowledge.enabled:false} == true and '${app.knowledge.provider:ollama}' == 'ollama'")
@RequiredArgsConstructor
@Slf4j
public class OllamaLlmAdapter implements LlmProviderPort {

    private final ChatClient chatClient;

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        int systemChars = length(systemPrompt);
        int userChars = length(userPrompt);
        log.info("Ollama LLM complete start: systemPromptChars={}, userPromptChars={}", systemChars, userChars);
        if (log.isDebugEnabled()) {
            log.debug("Ollama LLM userPrompt snippet: {}", truncateForLog(userPrompt, 200));
        }

        long start = System.nanoTime();
        String content = chatClient
                .prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();

        long durationMs = (System.nanoTime() - start) / 1_000_000L;
        int responseChars = length(content);
        log.info("Ollama LLM complete done: responseChars={}, durationMs={}", responseChars, durationMs);
        if (log.isDebugEnabled() && content != null) {
            log.debug("Ollama LLM response snippet: {}", truncateForLog(content, 200));
        }
        return content;
    }

    @Override
    public Flux<String> stream(String systemPrompt, String userPrompt) {
        try {
            int systemChars = length(systemPrompt);
            int userChars = length(userPrompt);
            log.info("Ollama LLM stream start: systemPromptChars={}, userPromptChars={}", systemChars, userChars);
            if (log.isDebugEnabled()) {
                log.debug("Ollama LLM stream userPrompt snippet: {}", truncateForLog(userPrompt, 200));
            }

            AtomicInteger tokenCount = new AtomicInteger();
            long start = System.nanoTime();

            return chatClient
                    .prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .stream()
                    .content()
                    .doOnNext(token -> tokenCount.incrementAndGet())
                    .doOnComplete(() -> log.info(
                            "Ollama LLM stream done: tokenCount={}, durationMs={}",
                            tokenCount.get(),
                            (System.nanoTime() - start) / 1_000_000L
                    ))
                    .doOnError(error -> log.error(
                            "Erreur streaming LLM after {} tokens: {}",
                            tokenCount.get(),
                            error.getMessage()
                    ));

        } catch (Exception e) {
            log.error("Erreur lors de l'initialisation du streaming LLM: {}", e.getMessage());
            return Flux.error(e);
        }
    }

    private static int length(String value) {
        return value != null ? value.length() : 0;
    }

    private static String truncateForLog(String value, int maxChars) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, maxChars) + "...";
    }
}
