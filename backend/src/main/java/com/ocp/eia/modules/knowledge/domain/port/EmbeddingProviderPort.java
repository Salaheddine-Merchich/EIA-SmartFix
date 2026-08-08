package com.ocp.eia.modules.knowledge.domain.port;

/**
 * Port d'inversion de dépendance pour la vectorisation de texte.
 * Implémentations : Ollama, OpenAI, Gemini...
 */
public interface EmbeddingProviderPort {
    float[] embed(String text);
}
