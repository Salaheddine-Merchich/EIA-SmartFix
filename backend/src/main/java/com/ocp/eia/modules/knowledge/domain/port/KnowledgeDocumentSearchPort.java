package com.ocp.eia.modules.knowledge.domain.port;

import com.ocp.eia.modules.knowledge.domain.model.SimilarKnowledgeDocument;

import java.util.List;

/**
 * Port for searching knowledge documents using various methods
 */
public interface KnowledgeDocumentSearchPort {

    /**
     * Search knowledge documents using full-text search
     * 
     * @param query Search query text
     * @param limit Maximum number of results to return
     * @return List of similar knowledge documents with relevance scores
     */
    List<SimilarKnowledgeDocument> searchDocuments(String query, int limit);

    /**
     * Search knowledge documents by equipment family with full-text search
     * 
     * @param query Search query text
     * @param equipmentFamily Equipment family to filter by
     * @param limit Maximum number of results to return
     * @return List of similar knowledge documents filtered by equipment family
     */
    List<SimilarKnowledgeDocument> searchByEquipmentFamily(String query, String equipmentFamily, int limit);

    /**
     * Search knowledge documents using vector similarity (if embeddings are available)
     * This method will be implemented later when knowledge document embeddings are indexed
     * 
     * @param queryEmbedding Query embedding vector
     * @param limit Maximum number of results to return
     * @return List of similar knowledge documents based on vector similarity
     */
    List<SimilarKnowledgeDocument> searchByEmbedding(float[] queryEmbedding, int limit);
}