package com.ocp.eia.modules.knowledge.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Short transactional writer for knowledge document embeddings.
 * Kept separate so embedding HTTP I/O stays outside DB transactions.
 */
@Service
@ConditionalOnProperty(name = "app.knowledge.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class KnowledgeDocumentEmbeddingStore {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void replaceEmbeddings(UUID documentId, List<DocumentChunkEmbedding> chunks) {
        removeDocumentEmbeddings(documentId);
        Instant now = Instant.now();
        for (DocumentChunkEmbedding chunk : chunks) {
            upsertDocumentEmbedding(documentId, chunk.content(), chunk.embedding(), chunk.chunkIndex(), now);
        }
    }

    @Transactional
    public void removeDocument(UUID documentId) {
        try {
            jdbcTemplate.update("DELETE FROM knowledge_document_embeddings WHERE document_id = ?", documentId);
            log.info("Embeddings du document {} supprimés", documentId);
        } catch (Exception e) {
            log.warn("Impossible de supprimer les embeddings du document {}: {}", documentId, e.getMessage());
        }
    }

    private void removeDocumentEmbeddings(UUID documentId) {
        try {
            int deleted = jdbcTemplate.update(
                    "DELETE FROM knowledge_document_embeddings WHERE document_id = ?", documentId);
            if (deleted > 0) {
                log.debug("Supprimés {} anciens embeddings du document {}", deleted, documentId);
            }
        } catch (Exception e) {
            log.warn("Erreur suppression anciens embeddings du document {}: {}", documentId, e.getMessage());
        }
    }

    private void upsertDocumentEmbedding(
            UUID documentId, String contentChunk, float[] embedding, int chunkIndex, Instant createdAt) {
        String vectorLiteral = toVectorLiteral(embedding);

        jdbcTemplate.update("""
                INSERT INTO knowledge_document_embeddings (document_id, content_chunk, embedding, chunk_index, created_at)
                VALUES (?, ?, ?::vector, ?, ?)
                ON CONFLICT (document_id, chunk_index) DO UPDATE SET
                    content_chunk = EXCLUDED.content_chunk,
                    embedding = EXCLUDED.embedding,
                    created_at = EXCLUDED.created_at
                """, documentId, contentChunk, vectorLiteral, chunkIndex, Timestamp.from(createdAt));
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

    public record DocumentChunkEmbedding(String content, float[] embedding, int chunkIndex) {}
}
