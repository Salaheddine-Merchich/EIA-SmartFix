package com.ocp.eia.modules.knowledge.domain.repository;

import com.ocp.eia.modules.knowledge.domain.model.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, UUID> {

    @Query("SELECT k FROM KnowledgeDocument k WHERE k.active = true")
    List<KnowledgeDocument> findAllActive();

    @Query("SELECT k FROM KnowledgeDocument k WHERE k.active = true AND " +
           "(:equipmentFamily IS NULL OR k.equipmentFamily = :equipmentFamily)")
    List<KnowledgeDocument> findByEquipmentFamily(@Param("equipmentFamily") String equipmentFamily);

    @Query("SELECT k FROM KnowledgeDocument k WHERE k.active = true AND k.documentType = :documentType")
    List<KnowledgeDocument> findByDocumentType(@Param("documentType") KnowledgeDocument.DocumentType documentType);

    long countByActiveTrue();

    /**
     * Full-text search in knowledge documents using PostgreSQL FTS
     */
    @Query(value = """
        SELECT k.* FROM knowledge_documents k
        WHERE k.active = true
        AND (
            (to_tsvector('french', coalesce(k.title, '')) || to_tsvector('french', coalesce(k.content, '')))
            @@ plainto_tsquery('french', :searchText)
        )
        ORDER BY ts_rank(
            to_tsvector('french', coalesce(k.title, '')) || to_tsvector('french', coalesce(k.content, '')),
            plainto_tsquery('french', :searchText)
        ) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<KnowledgeDocument> fullTextSearch(@Param("searchText") String searchText, @Param("limit") int limit);
}