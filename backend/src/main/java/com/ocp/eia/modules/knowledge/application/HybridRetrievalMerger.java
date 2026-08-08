package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.modules.knowledge.domain.model.SimilarIntervention;
import com.ocp.eia.modules.knowledge.domain.model.SimilarKnowledgeDocument;
import com.ocp.eia.modules.knowledge.domain.model.SimilarResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Fusion hybride vectoriel + full-text via Reciprocal Rank Fusion (RRF).
 * <p>
 * Le score RRF (basé sur le rang, échelle-invariant) sert au tri final.
 * Supporte maintenant la fusion unifiée de multiples sources :
 * interventions (vectoriel + texte) et documents techniques (vectoriel + texte).
 */
public final class HybridRetrievalMerger {

    static final int DEFAULT_RRF_K = 60;

    private HybridRetrievalMerger() {
    }

    public static List<SimilarIntervention> merge(
            List<SimilarIntervention> vectorResults,
            List<SimilarIntervention> textResults,
            int topK) {
        return merge(vectorResults, textResults, topK, DEFAULT_RRF_K);
    }

    public static List<SimilarIntervention> merge(
            List<SimilarIntervention> vectorResults,
            List<SimilarIntervention> textResults,
            int topK,
            int rrfK) {
        Map<UUID, Double> rrfScores = new HashMap<>();
        Map<UUID, SimilarIntervention> bestById = new HashMap<>();

        accumulateRrf(vectorResults, rrfK, rrfScores, bestById);
        accumulateRrf(textResults, rrfK, rrfScores, bestById);

        List<UUID> orderedIds = new ArrayList<>(rrfScores.keySet());
        orderedIds.sort((a, b) -> {
            int byRrf = Double.compare(rrfScores.get(b), rrfScores.get(a));
            if (byRrf != 0) {
                return byRrf;
            }
            return Double.compare(bestById.get(b).similarity(), bestById.get(a).similarity());
        });

        return orderedIds.stream()
                .limit(topK)
                .map(bestById::get)
                .toList();
    }

    private static void accumulateRrf(
            List<SimilarIntervention> results,
            int rrfK,
            Map<UUID, Double> rrfScores,
            Map<UUID, SimilarIntervention> bestById) {
        for (int rank = 0; rank < results.size(); rank++) {
            SimilarIntervention candidate = results.get(rank);
            UUID id = candidate.interventionId();
            double rrfContribution = 1.0 / (rrfK + rank + 1);
            rrfScores.merge(id, rrfContribution, Double::sum);
            bestById.merge(id, candidate, HybridRetrievalMerger::keepBestRawScore);
        }
    }

    private static SimilarIntervention keepBestRawScore(SimilarIntervention existing, SimilarIntervention incoming) {
        return existing.similarity() >= incoming.similarity() ? existing : incoming;
    }
    
    /**
     * Fusion unifiée de toutes les sources de résultats avec RRF
     * @param interventionVectorResults résultats recherche vectorielle interventions
     * @param interventionTextResults résultats recherche texte interventions  
     * @param knowledgeResults résultats recherche documents (déjà fusionnés vectoriel+texte)
     * @param topK nombre maximum de résultats à retourner
     * @return résultats fusionnés et triés
     */
    public static UnifiedResults mergeAll(
            List<SimilarIntervention> interventionVectorResults,
            List<SimilarIntervention> interventionTextResults,
            List<SimilarKnowledgeDocument> knowledgeResults,
            int topK) {
        
        return mergeAll(interventionVectorResults, interventionTextResults, knowledgeResults, topK, DEFAULT_RRF_K);
    }
    
    /**
     * Fusion unifiée avec RRF K personnalisé
     */
    public static UnifiedResults mergeAll(
            List<SimilarIntervention> interventionVectorResults,
            List<SimilarIntervention> interventionTextResults,
            List<SimilarKnowledgeDocument> knowledgeResults,
            int topK,
            int rrfK) {
        
        // Maps pour accumulation RRF
        Map<UUID, Double> rrfScores = new HashMap<>();
        Map<UUID, SimilarResult> bestById = new HashMap<>();
        
        // Accumulation des différentes sources
        accumulateRrfGeneric(interventionVectorResults, rrfK, rrfScores, bestById);
        accumulateRrfGeneric(interventionTextResults, rrfK, rrfScores, bestById);
        accumulateRrfGeneric(knowledgeResults, rrfK, rrfScores, bestById);
        
        // Tri par score RRF puis par similarité
        List<UUID> orderedIds = new ArrayList<>(rrfScores.keySet());
        orderedIds.sort((a, b) -> {
            int byRrf = Double.compare(rrfScores.get(b), rrfScores.get(a));
            if (byRrf != 0) {
                return byRrf;
            }
            return Double.compare(bestById.get(b).similarity(), bestById.get(a).similarity());
        });
        
        // Séparation par type dans les résultats finaux
        List<SimilarIntervention> interventions = new ArrayList<>();
        List<SimilarKnowledgeDocument> documents = new ArrayList<>();
        
        orderedIds.stream()
                .limit(topK)
                .map(bestById::get)
                .forEach(result -> {
                    if (result.getType() == SimilarResult.ResultType.INTERVENTION) {
                        interventions.add((SimilarIntervention) result);
                    } else {
                        documents.add((SimilarKnowledgeDocument) result);
                    }
                });
        
        return new UnifiedResults(interventions, documents);
    }
    
    /**
     * Accumulation RRF générique pour n'importe quel type de SimilarResult
     */
    private static <T extends SimilarResult> void accumulateRrfGeneric(
            List<T> results,
            int rrfK,
            Map<UUID, Double> rrfScores,
            Map<UUID, SimilarResult> bestById) {
        
        for (int rank = 0; rank < results.size(); rank++) {
            T candidate = results.get(rank);
            UUID id = candidate.getId();
            double rrfContribution = 1.0 / (rrfK + rank + 1);
            
            rrfScores.merge(id, rrfContribution, Double::sum);
            bestById.merge(id, candidate, HybridRetrievalMerger::keepBestSimilarResult);
        }
    }
    
    /**
     * Garde le résultat avec la meilleure similarité
     */
    private static SimilarResult keepBestSimilarResult(SimilarResult existing, SimilarResult incoming) {
        return existing.similarity() >= incoming.similarity() ? existing : incoming;
    }
    
    /**
     * Résultats unifiés séparés par type
     */
    public record UnifiedResults(
        List<SimilarIntervention> interventions,
        List<SimilarKnowledgeDocument> knowledgeDocuments
    ) {
        public int totalResults() {
            return interventions.size() + knowledgeDocuments.size();
        }
    }
}
