package com.ocp.eia.modules.knowledge.domain.port;

import java.util.List;

/**
 * Port pour le découpage intelligent de textes en chunks pour les embeddings.
 * Améliore la précision en divisant les textes longs en segments focalisés.
 */
public interface ChunkingProviderPort {
    
    /**
     * Divise un texte en chunks optimisés pour les embeddings
     * @param content le contenu à diviser
     * @param contentType type de contenu pour adaptation de la stratégie
     * @return liste de chunks avec métadonnées
     */
    List<TextChunk> chunkContent(String content, ContentType contentType);
    
    enum ContentType {
        INTERVENTION,        // Interventions techniques (symptômes, causes, actions)
        KNOWLEDGE_DOCUMENT, // Documents de connaissance (guides, manuels)
        GENERIC            // Contenu générique
    }
    
    /**
     * Représente un segment de texte avec métadonnées
     */
    record TextChunk(
        String content,        // Contenu du chunk
        int index,            // Position dans le document original
        int startOffset,      // Offset de début dans le texte original
        int endOffset,        // Offset de fin dans le texte original
        String type           // Type sémantique du chunk (optionnel)
    ) {
        
        /**
         * Crée un chunk simple sans métadonnées avancées
         */
        public static TextChunk simple(String content, int index) {
            return new TextChunk(content, index, 0, content.length(), "default");
        }
        
        /**
         * Taille du chunk en caractères
         */
        public int size() {
            return content.length();
        }
        
        /**
         * Vérifie si le chunk est vide ou insignifiant
         */
        public boolean isEmpty() {
            return content == null || content.trim().length() < 10;
        }
    }
}