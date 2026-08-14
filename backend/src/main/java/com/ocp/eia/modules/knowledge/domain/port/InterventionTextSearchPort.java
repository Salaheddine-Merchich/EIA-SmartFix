package com.ocp.eia.modules.knowledge.domain.port;

import com.ocp.eia.modules.knowledge.domain.model.QuerySignals;
import com.ocp.eia.modules.knowledge.domain.model.SearchContext;
import com.ocp.eia.modules.knowledge.domain.model.SimilarIntervention;

import java.util.List;

/**
 * Port pour la recherche full-text PostgreSQL sur les interventions validées (hybrid RAG).
 */
public interface InterventionTextSearchPort {

    List<SimilarIntervention> searchValidated(String query, int topK);

    List<SimilarIntervention> searchValidated(String query, int topK, SearchContext context);

    List<SimilarIntervention> searchValidated(String query, int topK, SearchContext context, QuerySignals signals);

    /**
     * Recherche ciblée zone/famille + mots-clés symptômes (voie sémantique dédiée).
     */
    List<SimilarIntervention> searchBySemanticContext(QuerySignals signals, SearchContext context, int topK);
}
