package com.ocp.eia.modules.knowledge.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocp.eia.application.dto.AiDto.AiAssistRequest;
import com.ocp.eia.application.dto.AiDto.AiAssistResponse;
import com.ocp.eia.config.AppProperties;
import com.ocp.eia.modules.knowledge.domain.model.SimilarIntervention;
import com.ocp.eia.modules.knowledge.domain.port.EmbeddingProviderPort;
import com.ocp.eia.modules.knowledge.domain.port.InterventionTextSearchPort;
import com.ocp.eia.modules.knowledge.domain.port.LlmProviderPort;
import com.ocp.eia.modules.knowledge.domain.port.VectorStorePort;
import com.ocp.eia.modules.knowledge.infrastructure.observability.RagRetrievalMetrics;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RagAssistTraceTest {

    @Mock private EmbeddingProviderPort embeddingProvider;
    @Mock private VectorStorePort vectorStore;
    @Mock private InterventionTextSearchPort interventionTextSearchPort;
    @Mock private LlmProviderPort llmProvider;
    @Mock private RagRetrievalMetrics ragRetrievalMetrics;
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;
    @Mock private AiDiagnosticStatsService diagnosticStatsService;
    @Spy private AppProperties appProperties = new AppProperties();
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks private RagAssistUseCase useCase;

    @BeforeEach
    void setUp() {
        appProperties.getAi().getRag().setTopK(5);
        appProperties.getAi().getRag().setSimilarityThreshold(0.70);
        appProperties.getAi().getRag().setHybridTextEnabled(true);
        Timer.Sample sample = mock(Timer.Sample.class);
        lenient().when(ragRetrievalMetrics.startRetrievalTimer()).thenReturn(sample);
        lenient().doNothing().when(ragRetrievalMetrics).recordRetrievalDuration(any());
        lenient().doNothing().when(ragRetrievalMetrics).recordVectorCount(anyInt());
        lenient().doNothing().when(ragRetrievalMetrics).recordTextCount(anyInt());
        lenient().doNothing().when(ragRetrievalMetrics).recordMergedCount(anyInt());
        lenient().doNothing().when(ragRetrievalMetrics).recordFilteredCount(anyInt());
        lenient().doNothing().when(ragRetrievalMetrics).recordLlmCall();
        lenient().when(interventionTextSearchPort.searchValidated(anyString(), anyInt())).thenReturn(List.of());
    }

    @Test
    void assist_includesDiagnosticTraceWithSources() {
        UUID interventionId = UUID.randomUUID();
        SimilarIntervention similar = new SimilarIntervention(
                interventionId, "CV-101", "Surchauffe convoyeur", "Code E001", "Remplacement", "Analyse", 0.91
        );
        when(embeddingProvider.embed("Panne convoyeur")).thenReturn(new float[]{0.5f});
        when(vectorStore.findSimilar(any(), eq(5))).thenReturn(List.of(similar));
        when(llmProvider.complete(anyString(), anyString())).thenReturn(
                "{\"probableCauses\":[\"Cause\"],\"correctiveActions\":[\"Action\"],\"summary\":\"Résumé\",\"advice\":\"Conseil\"}"
        );

        AiAssistResponse response = useCase.assist(new AiAssistRequest(null, null, "Panne convoyeur", null));

        assertNotNull(response.diagnosticTrace());
        assertEquals("Panne convoyeur", response.diagnosticTrace().query());
        assertEquals(1, response.diagnosticTrace().retrievedDocuments().size());
        assertEquals(interventionId, response.diagnosticTrace().retrievedDocuments().get(0).interventionId());
        assertTrue(response.diagnosticTrace().confidenceScore() > 0);
        assertFalse(response.diagnosticTrace().retrievalSteps().isEmpty());
        verify(diagnosticStatsService).record(any());
    }

    @Test
    void assist_unavailableEmbedding_traceShowsFailedStatus() {
        when(embeddingProvider.embed(anyString())).thenThrow(new RuntimeException("down"));

        AiAssistResponse response = useCase.assist(new AiAssistRequest(null, null, "Test", null));

        assertNotNull(response.diagnosticTrace());
        assertEquals("FAILED", response.diagnosticTrace().retrievalSteps().get(0).status());
        assertEquals(0, response.diagnosticTrace().filteredCount());
    }
}
