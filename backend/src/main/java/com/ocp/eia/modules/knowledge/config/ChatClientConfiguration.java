package com.ocp.eia.modules.knowledge.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Builds ChatClient when knowledge+ollama are enabled.
 * Class-level {@code @ConditionalOnBean(OllamaChatModel)} is avoided: scanned
 * {@code @Configuration} is evaluated before Spring AI auto-config.
 */
@Configuration
@ConditionalOnExpression("${app.knowledge.enabled:false} == true and '${app.knowledge.provider:ollama}' == 'ollama'")
public class ChatClientConfiguration {

    @Bean
    public ChatClient chatClient(OllamaChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
