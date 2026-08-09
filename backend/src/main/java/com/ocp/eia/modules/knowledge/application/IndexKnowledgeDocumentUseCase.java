package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.modules.knowledge.application.KnowledgeDocumentEmbeddingStore.DocumentChunkEmbedding;
import com.ocp.eia.modules.knowledge.domain.model.KnowledgeDocument;
import com.ocp.eia.modules.knowledge.domain.port.ChunkingProviderPort;
import com.ocp.eia.modules.knowledge.domain.port.EmbeddingProviderPort;
import com.ocp.eia.modules.knowledge.domain.repository.KnowledgeDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
    private final ChunkingProviderPort chunkingProvider;
    private final KnowledgeDocumentEmbeddingStore embeddingStore;

    /**
     * Indexe un document de connaissance en calculant ses embeddings par chunks.
     * Load/chunk/embed stay outside a long DB transaction; persistence uses a short write TX.
     */
    public IndexOutcome indexDocument(UUID documentId) {
        try {
            // Short read TX via Spring Data repository (no outer transaction)
            KnowledgeDocument document = knowledgeDocumentRepository.findById(documentId).orElse(null);
            if (document == null || !Boolean.TRUE.equals(document.getActive())) {
                log.warn("Document {} introuvable ou inactif, indexation ignorée", documentId);
                return IndexOutcome.SKIPPED;
            }

            String fullContent = buildIndexableContent(document);
            if (fullContent.isBlank()) {
                log.warn("Contenu vide pour document {}, indexation ignorée", documentId);
                return IndexOutcome.SKIPPED;
            }

            List<ChunkingProviderPort.TextChunk> chunks = chunkingProvider.chunkContent(
                    fullContent, ChunkingProviderPort.ContentType.KNOWLEDGE_DOCUMENT);

            if (chunks.isEmpty()) {
                log.warn("Aucun chunk valide pour document {}, indexation ignorée", documentId);
                return IndexOutcome.SKIPPED;
            }

            // Embed outside any DB transaction (Ollama/HTTP I/O)
            List<DocumentChunkEmbedding> prepared = new ArrayList<>();
            for (ChunkingProviderPort.TextChunk chunk : chunks) {
                if (!chunk.isEmpty()) {
                    try {
                        float[] embedding = embeddingProvider.embed(chunk.content());
                        prepared.add(new DocumentChunkEmbedding(chunk.content(), embedding, chunk.index()));
                    } catch (Exception e) {
                        log.warn("Erreur indexation chunk {} du document {}: {}",
                                chunk.index(), documentId, e.getMessage());
                    }
                }
            }

            if (prepared.isEmpty()) {
                log.warn("Aucun chunk indexé pour document {}", documentId);
                return IndexOutcome.FAILED;
            }

            // Short write TX: replace embeddings atomically
            embeddingStore.replaceEmbeddings(documentId, prepared);

            log.info("Document {} indexé avec succès: {} chunks dans la base de connaissances",
                    documentId, prepared.size());
            return IndexOutcome.INDEXED;

        } catch (Exception e) {
            log.error("Erreur lors de l'indexation du document {}: {}", documentId, e.getMessage());
            return IndexOutcome.FAILED;
        }
    }

    /**
     * Indexe tous les documents actifs.
     * Does not hold a DB transaction across embed HTTP calls.
     */
    public IndexResults indexAllDocuments() {
        // Short read TX via Spring Data repository
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
    public void removeDocument(UUID documentId) {
        embeddingStore.removeDocument(documentId);
    }

    private String buildIndexableContent(KnowledgeDocument document) {
        StringBuilder content = new StringBuilder();

        if (document.getTitle() != null && !document.getTitle().isBlank()) {
            content.append("Titre: ").append(document.getTitle()).append("\n");
        }

        if (document.getSource() != null && !document.getSource().isBlank()) {
            content.append("Source: ").append(document.getSource()).append("\n");
        }

        if (document.getEquipmentFamily() != null && !document.getEquipmentFamily().isBlank()) {
            content.append("Famille équipement: ").append(document.getEquipmentFamily()).append("\n");
        }

        content.append("Type: ").append(document.getDocumentType().getValue()).append("\n");

        if (document.getContent() != null && !document.getContent().isBlank()) {
            content.append("\nContenu:\n").append(document.getContent());
        }

        return content.toString().trim();
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
