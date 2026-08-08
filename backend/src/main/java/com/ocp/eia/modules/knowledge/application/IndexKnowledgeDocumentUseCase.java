package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.modules.knowledge.domain.model.KnowledgeDocument;
import com.ocp.eia.modules.knowledge.domain.port.ChunkingProviderPort;
import com.ocp.eia.modules.knowledge.domain.port.EmbeddingProviderPort;
import com.ocp.eia.modules.knowledge.domain.repository.KnowledgeDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.knowledge.enabled", havingValue = "true")
@ConditionalOnBean(EmbeddingProviderPort.class)
@RequiredArgsConstructor
@Slf4j
public class IndexKnowledgeDocumentUseCase {

    private final EmbeddingProviderPort embeddingProvider;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ChunkingProviderPort chunkingProvider;

    /**
     * Indexe un document de connaissance en calculant ses embeddings par chunks
     */
    @Transactional
    public IndexOutcome indexDocument(UUID documentId) {
        try {
            KnowledgeDocument document = knowledgeDocumentRepository.findById(documentId).orElse(null);
            if (document == null || !Boolean.TRUE.equals(document.getActive())) {
                log.warn("Document {} introuvable ou inactif, indexation ignorée", documentId);
                return IndexOutcome.SKIPPED;
            }

            // Construire le contenu indexable complet
            String fullContent = buildIndexableContent(document);
            if (fullContent.isBlank()) {
                log.warn("Contenu vide pour document {}, indexation ignorée", documentId);
                return IndexOutcome.SKIPPED;
            }

            // Chunking intelligent du document
            List<ChunkingProviderPort.TextChunk> chunks = chunkingProvider.chunkContent(
                fullContent, ChunkingProviderPort.ContentType.KNOWLEDGE_DOCUMENT);
            
            if (chunks.isEmpty()) {
                log.warn("Aucun chunk valide pour document {}, indexation ignorée", documentId);
                return IndexOutcome.SKIPPED;
            }
            
            // Supprimer les anciens embeddings du document
            removeDocumentEmbeddings(documentId);
            
            // Indexer chaque chunk
            int indexedChunks = 0;
            for (ChunkingProviderPort.TextChunk chunk : chunks) {
                if (!chunk.isEmpty()) {
                    try {
                        float[] embedding = embeddingProvider.embed(chunk.content());
                        upsertDocumentEmbedding(documentId, chunk.content(), embedding, chunk.index());
                        indexedChunks++;
                    } catch (Exception e) {
                        log.warn("Erreur indexation chunk {} du document {}: {}", 
                                chunk.index(), documentId, e.getMessage());
                    }
                }
            }
            
            if (indexedChunks == 0) {
                log.warn("Aucun chunk indexé pour document {}", documentId);
                return IndexOutcome.FAILED;
            }
            
            log.info("Document {} indexé avec succès: {} chunks dans la base de connaissances", 
                    documentId, indexedChunks);
            return IndexOutcome.INDEXED;
            
        } catch (Exception e) {
            log.error("Erreur lors de l'indexation du document {}: {}", documentId, e.getMessage());
            return IndexOutcome.FAILED;
        }
    }

    /**
     * Indexe tous les documents actifs
     */
    @Transactional
    public IndexResults indexAllDocuments() {
        List<KnowledgeDocument> activeDocuments = knowledgeDocumentRepository.findAllActive();
        int indexed = 0, skipped = 0, failed = 0;
        
        log.info("Démarrage de l'indexation de {} documents de connaissance", activeDocuments.size());
        
        for (KnowledgeDocument document : activeDocuments) {
            IndexOutcome outcome = indexDocument(document.getId());
            switch (outcome) {
                case INDEXED -> indexed++;
                case SKIPPED -> skipped++;
                case FAILED -> failed++;
            }
        }
        
        log.info("Indexation terminée: {} indexés, {} ignorés, {} échecs", indexed, skipped, failed);
        return new IndexResults(indexed, skipped, failed);
    }

    /**
     * Supprime les embeddings d'un document
     */
    @Transactional
    public void removeDocument(UUID documentId) {
        try {
            jdbcTemplate.update("DELETE FROM knowledge_document_embeddings WHERE document_id = ?", documentId);
            log.info("Embeddings du document {} supprimés", documentId);
        } catch (Exception e) {
            log.warn("Impossible de supprimer les embeddings du document {}: {}", documentId, e.getMessage());
        }
    }

    /**
     * Construit le contenu indexable à partir du document
     */
    private String buildIndexableContent(KnowledgeDocument document) {
        StringBuilder content = new StringBuilder();
        
        // Titre avec poids
        if (document.getTitle() != null && !document.getTitle().isBlank()) {
            content.append("Titre: ").append(document.getTitle()).append("\n");
        }
        
        // Source
        if (document.getSource() != null && !document.getSource().isBlank()) {
            content.append("Source: ").append(document.getSource()).append("\n");
        }
        
        // Famille d'équipement si spécifiée
        if (document.getEquipmentFamily() != null && !document.getEquipmentFamily().isBlank()) {
            content.append("Famille équipement: ").append(document.getEquipmentFamily()).append("\n");
        }
        
        // Type de document
        content.append("Type: ").append(document.getDocumentType().getValue()).append("\n");
        
        // Contenu principal
        if (document.getContent() != null && !document.getContent().isBlank()) {
            content.append("\nContenu:\n").append(document.getContent());
        }
        
        return content.toString().trim();
    }

    /**
     * Supprime tous les embeddings existants d'un document
     */
    private void removeDocumentEmbeddings(UUID documentId) {
        try {
            int deleted = jdbcTemplate.update("DELETE FROM knowledge_document_embeddings WHERE document_id = ?", documentId);
            if (deleted > 0) {
                log.debug("Supprimés {} anciens embeddings du document {}", deleted, documentId);
            }
        } catch (Exception e) {
            log.warn("Erreur suppression anciens embeddings du document {}: {}", documentId, e.getMessage());
        }
    }

    /**
     * Insert/update embedding dans la base
     */
    private void upsertDocumentEmbedding(UUID documentId, String contentChunk, float[] embedding, int chunkIndex) {
        String vectorLiteral = toVectorLiteral(embedding);
        
        jdbcTemplate.update("""
                INSERT INTO knowledge_document_embeddings (document_id, content_chunk, embedding, chunk_index, created_at)
                VALUES (?, ?, ?::vector, ?, ?)
                ON CONFLICT (document_id, chunk_index) DO UPDATE SET
                    content_chunk = EXCLUDED.content_chunk,
                    embedding = EXCLUDED.embedding,
                    created_at = EXCLUDED.created_at
                """, documentId, contentChunk, vectorLiteral, chunkIndex, Timestamp.from(Instant.now()));
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

    public enum IndexOutcome {
        INDEXED,
        SKIPPED,
        FAILED
    }

    public record IndexResults(int indexed, int skipped, int failed) {
        public int total() {
            return indexed + skipped + failed;
        }
    }
}