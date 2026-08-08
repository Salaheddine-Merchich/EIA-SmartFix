package com.ocp.eia.modules.knowledge.infrastructure.ai;

import com.ocp.eia.modules.knowledge.domain.port.LlmProviderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@ConditionalOnExpression("${app.knowledge.enabled:false} == true and '${app.knowledge.provider:ollama}' == 'ollama'")
@RequiredArgsConstructor
@Slf4j
public class OllamaLlmAdapter implements LlmProviderPort {

    private final ChatClient chatClient;

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        return chatClient
                .prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
    }

    @Override
    public Flux<String> stream(String systemPrompt, String userPrompt) {
        try {
            log.debug("Démarrage streaming LLM pour prompt utilisateur: {}", 
                     userPrompt.length() > 100 ? userPrompt.substring(0, 100) + "..." : userPrompt);
            
            return chatClient
                    .prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .stream()
                    .content()
                    .doOnNext(token -> log.trace("Token reçu: {}", token))
                    .doOnComplete(() -> log.debug("Streaming LLM terminé"))
                    .doOnError(error -> log.error("Erreur streaming LLM: {}", error.getMessage()));
                    
        } catch (Exception e) {
            log.error("Erreur lors de l'initialisation du streaming LLM: {}", e.getMessage());
            return Flux.error(e);
        }
    }
}
