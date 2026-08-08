package com.ocp.eia.modules.knowledge.domain.model;

import java.util.UUID;

/**
 * Interface commune pour les résultats de recherche similaire.
 * Permet une fusion uniforme dans HybridRetrievalMerger.
 */
public interface SimilarResult {
    
    /**
     * Identifiant unique du résultat (intervention ou document)
     */
    UUID getId();
    
    /**
     * Score de similarité [0.0, 1.0]
     */
    double similarity();
    
    /**
     * Type de résultat pour la classification
     */
    ResultType getType();
    
    enum ResultType {
        INTERVENTION,
        KNOWLEDGE_DOCUMENT
    }
}