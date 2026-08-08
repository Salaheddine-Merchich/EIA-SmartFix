package com.ocp.eia.modules.knowledge.domain.port;

import com.ocp.eia.modules.knowledge.domain.model.SearchContext;
import com.ocp.eia.modules.knowledge.domain.model.SimilarIntervention;

import java.util.List;

/**
 * Port pour la recherche full-text PostgreSQL sur les interventions validées (hybrid RAG).
 */
public interface InterventionTextSearchPort {

    List<SimilarIntervention> searchValidated(String query, int topK);
    
    /**
     * Recherche full-text avec contexte pour filtrage et pondération
     */
    List<SimilarIntervention> searchValidated(String query, int topK, SearchContext context);
}
