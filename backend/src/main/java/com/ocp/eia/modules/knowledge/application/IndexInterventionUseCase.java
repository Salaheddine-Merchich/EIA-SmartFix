package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.modules.knowledge.domain.port.EmbeddingProviderPort;
import com.ocp.eia.modules.knowledge.domain.port.VectorStorePort;
import com.ocp.eia.modules.maintenance.application.event.InterventionKnowledgePayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.knowledge.enabled", havingValue = "true")
@ConditionalOnBean(EmbeddingProviderPort.class)
@RequiredArgsConstructor
@Slf4j
public class IndexInterventionUseCase {

    private final EmbeddingProviderPort embeddingProvider;
    private final VectorStorePort vectorStore;

    public IndexOutcome index(InterventionKnowledgePayload payload) {
        try {
            String content = payload.toIndexedContent();
            if (content.isBlank()) {
                log.warn("Contenu vide pour intervention {}, indexation ignorée", payload.interventionId());
                return IndexOutcome.SKIPPED;
            }
            float[] embedding = embeddingProvider.embed(content);
            vectorStore.upsert(payload.interventionId(), embedding, content);
            log.info("Intervention {} indexée dans la base de connaissances", payload.interventionId());
            return IndexOutcome.INDEXED;
        } catch (Exception e) {
            log.error("Erreur indexation intervention {}: {}", payload.interventionId(), e.getMessage());
            return IndexOutcome.FAILED;
        }
    }

    public enum IndexOutcome {
        INDEXED,
        SKIPPED,
        FAILED
    }

    public void remove(java.util.UUID interventionId) {
        try {
            vectorStore.delete(interventionId);
            log.info("Intervention {} retirée de la base de connaissances", interventionId);
        } catch (Exception e) {
            log.warn("Impossible de supprimer l'index {}: {}", interventionId, e.getMessage());
        }
    }
}
