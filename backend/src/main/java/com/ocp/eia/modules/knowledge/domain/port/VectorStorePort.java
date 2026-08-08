package com.ocp.eia.modules.knowledge.domain.port;

import com.ocp.eia.modules.knowledge.domain.model.SearchContext;
import com.ocp.eia.modules.knowledge.domain.model.SimilarIntervention;

import java.util.List;
import java.util.UUID;

/**
 * Port pour le stockage et la recherche vectorielle.
 * Implémentation : PostgreSQL pgvector.
 */
public interface VectorStorePort {

    void upsert(UUID interventionId, float[] embedding, String indexedContent);

    void delete(UUID interventionId);

    List<SimilarIntervention> findSimilar(float[] queryEmbedding, int topK);
    
    /**
     * Recherche vectorielle avec contexte pour filtrage et pondération
     */
    List<SimilarIntervention> findSimilar(float[] queryEmbedding, int topK, SearchContext context);
}
