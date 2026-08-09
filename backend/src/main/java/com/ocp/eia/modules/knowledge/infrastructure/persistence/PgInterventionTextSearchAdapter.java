package com.ocp.eia.modules.knowledge.infrastructure.persistence;

import com.ocp.eia.modules.knowledge.domain.model.SearchContext;
import com.ocp.eia.modules.knowledge.domain.model.SimilarIntervention;
import com.ocp.eia.modules.knowledge.domain.port.InterventionTextSearchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.knowledge.enabled", havingValue = "true")
@RequiredArgsConstructor
public class PgInterventionTextSearchAdapter implements InterventionTextSearchPort {

    private static final int BOOST_OVERSAMPLE_FACTOR = 5;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<SimilarIntervention> searchValidated(String query, int topK) {
        return searchValidated(query, topK, SearchContext.none());
    }

    @Override
    public List<SimilarIntervention> searchValidated(String query, int topK, SearchContext context) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        
        String likePattern = "%" + query.trim() + "%";
        
        StringBuilder queryBuilder = new StringBuilder("""
                SELECT i.id AS intervention_id,
                       i.symptomes,
                       i.cause_racine,
                       i.actions_correctives,
                       i.analyse_technique,
                       e.code AS equipment_code,
                       e.id AS equipment_id,
                       e.famille AS equipment_family,
                       e.zone AS equipment_zone,
                       GREATEST(
                           COALESCE(ts_rank(
                               i.search_vector,
                               plainto_tsquery('french', ?)
                           ), 0),
                           CASE WHEN f.code_defaut ILIKE ?
                                     OR e.code ILIKE ?
                                     OR e.constructeur ILIKE ?
                                THEN 0.75 ELSE 0 END
                       ) AS base_similarity
                FROM interventions i
                JOIN failures f ON f.id = i.failure_id
                JOIN equipment e ON e.id = f.equipment_id
                WHERE i.statut_validation = 'VALIDEE'
                AND i.search_vector IS NOT NULL
                AND (
                    i.search_vector @@ plainto_tsquery('french', ?)
                    OR f.code_defaut ILIKE ?
                    OR e.code ILIKE ?
                    OR e.constructeur ILIKE ?
                )
                """);
        
        List<Object> params = new ArrayList<>();
        params.add(query);
        params.add(likePattern);
        params.add(likePattern);
        params.add(likePattern);
        params.add(query);
        params.add(likePattern);
        params.add(likePattern);
        params.add(likePattern);

        // Soft filter: no hard equipment/family/zone WHERE — boost matching rows in Java below
        int fetchLimit = context.hasFilters()
                ? Math.max(topK, topK * BOOST_OVERSAMPLE_FACTOR)
                : topK;

        queryBuilder.append(" ORDER BY base_similarity DESC LIMIT ?");
        params.add(fetchLimit);

        List<SimilarIntervention> results = jdbcTemplate.query(queryBuilder.toString(),
                (rs, rowNum) -> {
                    UUID equipmentId = UUID.fromString(rs.getString("equipment_id"));
                    String family = rs.getString("equipment_family");
                    String zone = rs.getString("equipment_zone");
                    double baseSimilarity = rs.getDouble("base_similarity");

                    double boost = context.calculateBoost(equipmentId, family, zone);
                    double finalSimilarity = Math.min(1.0, baseSimilarity * boost);

                    return new SimilarIntervention(
                            UUID.fromString(rs.getString("intervention_id")),
                            rs.getString("equipment_code"),
                            rs.getString("symptomes"),
                            rs.getString("cause_racine"),
                            rs.getString("actions_correctives"),
                            rs.getString("analyse_technique"),
                            finalSimilarity
                    );
                },
                params.toArray());

        if (context.hasFilters()) {
            results.sort((a, b) -> Double.compare(b.similarity(), a.similarity()));
            if (results.size() > topK) {
                return results.subList(0, topK);
            }
        }

        return results;
    }
}
