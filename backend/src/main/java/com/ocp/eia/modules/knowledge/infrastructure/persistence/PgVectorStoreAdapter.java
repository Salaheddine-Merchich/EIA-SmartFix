package com.ocp.eia.modules.knowledge.infrastructure.persistence;

import com.ocp.eia.modules.knowledge.domain.model.SearchContext;
import com.ocp.eia.modules.knowledge.domain.model.SimilarIntervention;
import com.ocp.eia.modules.knowledge.domain.port.VectorStorePort;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.knowledge.enabled", havingValue = "true")
@RequiredArgsConstructor
public class PgVectorStoreAdapter implements VectorStorePort {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void upsert(UUID interventionId, float[] embedding, String indexedContent) {
        String vectorLiteral = toVectorLiteral(embedding);
        jdbcTemplate.update("""
                INSERT INTO intervention_embeddings (intervention_id, embedding, contenu_indexe, indexe_le)
                VALUES (?, ?::vector, ?, ?)
                ON CONFLICT (intervention_id) DO UPDATE SET
                    embedding = EXCLUDED.embedding,
                    contenu_indexe = EXCLUDED.contenu_indexe,
                    indexe_le = EXCLUDED.indexe_le
                """, interventionId, vectorLiteral, indexedContent, Timestamp.from(Instant.now()));
    }

    @Override
    public void delete(UUID interventionId) {
        jdbcTemplate.update("DELETE FROM intervention_embeddings WHERE intervention_id = ?", interventionId);
    }

    @Override
    public List<SimilarIntervention> findSimilar(float[] embedding, int topK) {
        return findSimilar(embedding, topK, SearchContext.none());
    }

    @Override
    public List<SimilarIntervention> findSimilar(float[] embedding, int topK, SearchContext context) {
        String vectorLiteral = toVectorLiteral(embedding);
        
        // Construire la requête avec filtrage contextuel
        StringBuilder queryBuilder = new StringBuilder("""
                SELECT ie.intervention_id,
                       i.symptomes,
                       i.cause_racine,
                       i.actions_correctives,
                       i.analyse_technique,
                       e.code AS equipment_code,
                       e.id AS equipment_id,
                       e.famille AS equipment_family,
                       e.zone AS equipment_zone,
                       (1 - (ie.embedding <=> ?::vector)) AS base_similarity
                FROM intervention_embeddings ie
                JOIN interventions i ON i.id = ie.intervention_id
                JOIN failures f ON f.id = i.failure_id
                JOIN equipment e ON e.id = f.equipment_id
                WHERE i.statut_validation = 'VALIDEE'
                """);
        
        List<Object> params = new ArrayList<>();
        params.add(vectorLiteral);
        
        // Ajouter filtres contextuels si présents
        if (context.equipmentId() != null) {
            queryBuilder.append(" AND e.id = ?");
            params.add(context.equipmentId());
        } else if (context.equipmentFamily() != null) {
            queryBuilder.append(" AND LOWER(e.famille) = LOWER(?)");
            params.add(context.equipmentFamily());
        } else if (context.equipmentZone() != null) {
            queryBuilder.append(" AND LOWER(e.zone) = LOWER(?)");
            params.add(context.equipmentZone());
        }
        
        queryBuilder.append(" ORDER BY ie.embedding <=> ?::vector LIMIT ?");
        params.add(vectorLiteral);
        params.add(topK);
        
        List<SimilarIntervention> results = jdbcTemplate.query(queryBuilder.toString(),
                (rs, rowNum) -> {
                    UUID equipmentId = UUID.fromString(rs.getString("equipment_id"));
                    String family = rs.getString("equipment_family");
                    String zone = rs.getString("equipment_zone");
                    double baseSimilarity = rs.getDouble("base_similarity");
                    
                    // Appliquer le boost contextuel
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
        
        // Re-trier par similarité finale si des boosts ont été appliqués
        if (context.hasFilters()) {
            results.sort((a, b) -> Double.compare(b.similarity(), a.similarity()));
        }
        
        return results;
    }

    private String toVectorLiteral(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
