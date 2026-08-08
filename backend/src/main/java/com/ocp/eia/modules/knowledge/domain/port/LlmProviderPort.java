package com.ocp.eia.modules.knowledge.domain.port;

import reactor.core.publisher.Flux;

/**
 * Port pour la génération de texte par LLM.
 * Implémentations : Ollama, OpenAI, Gemini...
 */
public interface LlmProviderPort {
    
    /**
     * Génération synchrone de texte (méthode existante)
     */
    String complete(String systemPrompt, String userPrompt);
    
    /**
     * Génération streaming de texte avec flux de tokens
     * @return flux de tokens individuels au fur et à mesure de la génération
     */
    default Flux<String> stream(String systemPrompt, String userPrompt) {
        // Implémentation par défaut pour compatibilité : génération synchrone puis émission
        String result = complete(systemPrompt, userPrompt);
        return Flux.just(result);
    }
}
