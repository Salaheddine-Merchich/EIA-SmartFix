package com.ocp.eia.modules.knowledge.infrastructure.search;

import com.ocp.eia.modules.knowledge.domain.model.KnowledgeDocument;
import com.ocp.eia.modules.knowledge.domain.model.SimilarKnowledgeDocument;
import com.ocp.eia.modules.knowledge.domain.port.KnowledgeDocumentSearchPort;
import com.ocp.eia.modules.knowledge.domain.repository.KnowledgeDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.knowledge.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class KnowledgeDocumentSearchAdapter implements KnowledgeDocumentSearchPort {

    private final KnowledgeDocumentRepository repository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<SimilarKnowledgeDocument> searchDocuments(String query, int limit) {
        try {
            List<KnowledgeDocument> documents = repository.fullTextSearch(query, limit);
            return documents.stream()
                    .map(doc -> mapToSimilarDocument(doc, calculateRelevanceScore(query, doc)))
                    .toList();
        } catch (Exception e) {
            log.warn("Erreur lors de la recherche dans les documents de connaissance: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<SimilarKnowledgeDocument> searchByEquipmentFamily(String query, String equipmentFamily, int limit) {
        try {
            // For now, use the same full-text search but could be enhanced to filter by family
            List<KnowledgeDocument> documents = repository.fullTextSearch(query, limit * 2);
            return documents.stream()
                    .filter(doc -> equipmentFamily == null || 
                                   doc.getEquipmentFamily() == null || 
                                   doc.getEquipmentFamily().equalsIgnoreCase(equipmentFamily))
                    .limit(limit)
                    .map(doc -> mapToSimilarDocument(doc, calculateRelevanceScore(query, doc)))
                    .toList();
        } catch (Exception e) {
            log.warn("Erreur lors de la recherche par famille d'équipement: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<SimilarKnowledgeDocument> searchByEmbedding(float[] queryEmbedding, int limit) {
        try {
            String vectorLiteral = toVectorLiteral(queryEmbedding);
            
            List<SimilarKnowledgeDocument> results = jdbcTemplate.query("""
                    SELECT kd.id, kd.title, kd.document_type, kd.equipment_family, kd.source,
                           kde.content_chunk,
                           1 - (kde.embedding <=> ?::vector) AS similarity
                    FROM knowledge_document_embeddings kde
                    JOIN knowledge_documents kd ON kd.id = kde.document_id
                    WHERE kd.active = true
                    ORDER BY kde.embedding <=> ?::vector
                    LIMIT ?
                    """,
                    (rs, rowNum) -> {
                        return new SimilarKnowledgeDocument(
                                UUID.fromString(rs.getString("id")),
                                rs.getString("title"),
                                rs.getString("content_chunk"), // Utilise le chunk comme excerpt
                                rs.getString("document_type"),
                                rs.getString("equipment_family"),
                                rs.getString("source"),
                                rs.getDouble("similarity")
                        );
                    },
                    vectorLiteral, vectorLiteral, limit);
            
            log.debug("Recherche vectorielle documents: {} résultats trouvés", results.size());
            return results;
            
        } catch (Exception e) {
            log.warn("Erreur lors de la recherche vectorielle dans les documents: {}", e.getMessage());
            return List.of();
        }
    }

    private SimilarKnowledgeDocument mapToSimilarDocument(KnowledgeDocument document, double relevanceScore) {
        String excerpt = SimilarKnowledgeDocument.createExcerpt(document.getContent(), 500);
        
        return new SimilarKnowledgeDocument(
                document.getId(),
                document.getTitle(),
                excerpt,
                document.getDocumentType().getValue(),
                document.getEquipmentFamily(),
                document.getSource(),
                relevanceScore
        );
    }

    /**
     * Calculate a simple relevance score based on query terms appearing in title and content
     * This is a basic implementation - could be enhanced with TF-IDF or other algorithms
     */
    private double calculateRelevanceScore(String query, KnowledgeDocument document) {
        if (query == null || query.trim().isEmpty()) {
            return 0.5; // Default relevance for empty queries
        }

        String[] queryTerms = query.toLowerCase().split("\\s+");
        String titleLower = document.getTitle().toLowerCase();
        String contentLower = document.getContent().toLowerCase();

        int titleMatches = 0;
        int contentMatches = 0;
        
        for (String term : queryTerms) {
            if (term.length() > 2) { // Ignore very short terms
                if (titleLower.contains(term)) {
                    titleMatches++;
                }
                if (contentLower.contains(term)) {
                    contentMatches++;
                }
            }
        }

        // Weight title matches higher than content matches
        double score = (titleMatches * 0.4 + contentMatches * 0.2) / queryTerms.length;
        return Math.min(0.95, Math.max(0.1, score)); // Clamp between 0.1 and 0.95
    }

    /**
     * Convertit un embedding en format vector PostgreSQL
     */
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