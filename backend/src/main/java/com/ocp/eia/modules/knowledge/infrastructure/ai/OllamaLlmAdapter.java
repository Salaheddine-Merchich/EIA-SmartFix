package com.ocp.eia.modules.knowledge.infrastructure.ai;

import com.ocp.eia.modules.knowledge.domain.port.LlmProviderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Do not use {@code @ConditionalOnBean(ChatClient)} on this {@code @Component}:
 * scan-time conditions can miss beans defined by later {@code @Configuration}/auto-config.
 */
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

        long start = System.nanoTime();
        String content = chatClient
                .prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();

        long durationMs = (System.nanoTime() - start) / 1_000_000L;
        log.info("Ollama LLM complete done: responseChars={}, durationMs={}", length(content), durationMs);
        return content;
    }

    @Override
    public Flux<String> stream(String systemPrompt, String userPrompt) {
        try {
            log.info(
                    "Ollama LLM stream start: systemPromptChars={}, userPromptChars={}",
                    length(systemPrompt),
                    length(userPrompt)
            );

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
                            error.getClass().getSimpleName()
                    ));

        } catch (Exception e) {
            log.error("Erreur lors de l'initialisation du streaming LLM: {}", e.getClass().getSimpleName());
            return Flux.error(e);
        }
    }

    private static int length(String value) {
        return value != null ? value.length() : 0;
    }
}
