package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.modules.knowledge.domain.port.EmbeddingProviderPort;
import com.ocp.eia.modules.knowledge.domain.port.VectorStorePort;
import com.ocp.eia.modules.maintenance.application.event.InterventionKnowledgePayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IndexInterventionUseCaseTest {

    @Mock private EmbeddingProviderPort embeddingProvider;
    @Mock private VectorStorePort vectorStore;

    @InjectMocks private IndexInterventionUseCase useCase;

    @Test
    void index_embedsAndUpsertsContent() {
        UUID id = UUID.randomUUID();
        InterventionKnowledgePayload payload = new InterventionKnowledgePayload(
                id, "Vibrations", "Roulement usé", null, "Remplacement roulement", null, "Panne convoyeur",
                null, null, null, null, null, null, null, null, null, null, null, null
        );
        float[] embedding = new float[]{0.1f, 0.2f};

        when(embeddingProvider.embed(anyString())).thenReturn(embedding);

        IndexInterventionUseCase.IndexOutcome outcome = useCase.index(payload);

        assertEquals(IndexInterventionUseCase.IndexOutcome.INDEXED, outcome);
        verify(embeddingProvider).embed(contains("Symptômes: Vibrations"));
        verify(vectorStore).upsert(eq(id), same(embedding), contains("Cause racine: Roulement usé"));
    }

    @Test
    void index_blankContent_skipsEmbedding() {
        UUID id = UUID.randomUUID();
        InterventionKnowledgePayload payload = new InterventionKnowledgePayload(
                id, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null
        );

        IndexInterventionUseCase.IndexOutcome outcome = useCase.index(payload);

        assertEquals(IndexInterventionUseCase.IndexOutcome.SKIPPED, outcome);
        verify(embeddingProvider, never()).embed(anyString());
        verify(vectorStore, never()).upsert(any(), any(), anyString());
    }

    @Test
    void index_embeddingFailure_doesNotThrow() {
        UUID id = UUID.randomUUID();
        InterventionKnowledgePayload payload = new InterventionKnowledgePayload(
                id, "Alarme", null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null
        );
        when(embeddingProvider.embed(anyString())).thenThrow(new RuntimeException("Ollama indisponible"));

        IndexInterventionUseCase.IndexOutcome outcome = useCase.index(payload);

        assertEquals(IndexInterventionUseCase.IndexOutcome.FAILED, outcome);
        verify(vectorStore, never()).upsert(any(), any(), anyString());
    }

    @Test
    void remove_deletesFromVectorStore() {
        UUID id = UUID.randomUUID();

        useCase.remove(id);

        verify(vectorStore).delete(id);
    }

    @Test
    void remove_failure_doesNotThrow() {
        UUID id = UUID.randomUUID();
        doThrow(new RuntimeException("DB error")).when(vectorStore).delete(id);

        useCase.remove(id);

        verify(vectorStore).delete(id);
    }
}
