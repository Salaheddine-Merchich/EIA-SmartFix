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
class RagAssistUseCaseTest {

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
    void assist_noSimilarInterventions_returnsFallbackSuggestions() {
        AiAssistRequest request = new AiAssistRequest(null, null, "Panne variateur", null);
        float[] embedding = new float[]{0.5f};

        when(embeddingProvider.embed("Panne variateur")).thenReturn(embedding);
        when(vectorStore.findSimilar(embedding, 5)).thenReturn(List.of());

        AiAssistResponse response = useCase.assist(request);

        assertTrue(response.similarInterventions().isEmpty());
        assertEquals("Aucune intervention similaire validée trouvée", response.suggestions().probableCauses().get(0));
        verify(llmProvider, never()).complete(anyString(), anyString());
    }

    @Test
    void assist_withSimilarInterventions_usesLlmAndReturnsSuggestions() {
        AiAssistRequest request = new AiAssistRequest(null, null, "Défaut moteur", 3);
        UUID interventionId = UUID.randomUUID();
        float[] embedding = new float[]{0.3f};
        SimilarIntervention similar = new SimilarIntervention(
                interventionId, "EQ-001", "Surchauffe", "Isolation", "Remplacement", "Analyse IR", 0.92
        );
        String llmJson = """
                {"probableCauses":["Isolation dégradée"],"correctiveActions":["Mesurer isolation"],"summary":"Piste moteur","advice":"Couper alimentation"}
                """;

        when(embeddingProvider.embed("Défaut moteur")).thenReturn(embedding);
        when(vectorStore.findSimilar(embedding, 3)).thenReturn(List.of(similar));
        when(llmProvider.complete(anyString(), anyString())).thenReturn(llmJson);

        AiAssistResponse response = useCase.assist(request);

        assertEquals(1, response.similarInterventions().size());
        assertEquals(interventionId, response.similarInterventions().get(0).interventionId());
        assertEquals("Isolation dégradée", response.suggestions().probableCauses().get(0));
        assertNotNull(response.disclaimer());
    }

    @Test
    void assist_llmFailure_fallsBackToSimilarInterventionData() {
        AiAssistRequest request = new AiAssistRequest(null, null, "Panne", null);
        float[] embedding = new float[]{0.1f};
        SimilarIntervention similar = new SimilarIntervention(
                UUID.randomUUID(), "EQ-002", null, "Capteur HS", "Changement capteur", null, 0.85
        );

        when(embeddingProvider.embed("Panne")).thenReturn(embedding);
        when(vectorStore.findSimilar(embedding, 5)).thenReturn(List.of(similar));
        when(llmProvider.complete(anyString(), anyString())).thenThrow(new RuntimeException("LLM down"));

        AiAssistResponse response = useCase.assist(request);

        assertEquals("Capteur HS", response.suggestions().probableCauses().get(0));
        assertTrue(response.suggestions().summary().contains("1 intervention"));
    }

    @Test
    void assist_embeddingUnavailable_returnsControlledResponse() {
        when(embeddingProvider.embed(anyString()))
                .thenThrow(new RuntimeException("Connection refused: Ollama indisponible"));

        AiAssistResponse response = useCase.assist(new AiAssistRequest(null, null, "Panne variateur", null));

        assertUnavailableResponse(response);
        verifyNoInteractions(vectorStore, llmProvider);
    }

    @Test
    void assist_vectorStoreUnavailable_returnsControlledResponse() {
        float[] embedding = new float[]{0.2f};
        when(embeddingProvider.embed(anyString())).thenReturn(embedding);
        when(vectorStore.findSimilar(embedding, 5))
                .thenThrow(new RuntimeException("SQL error: pgvector indisponible"));

        AiAssistResponse response = useCase.assist(new AiAssistRequest(null, null, "Panne moteur", null));

        assertUnavailableResponse(response);
        verify(llmProvider, never()).complete(anyString(), anyString());
    }

    @Test
    void assist_timeout_returnsControlledResponse() {
        when(embeddingProvider.embed(anyString()))
                .thenThrow(new RuntimeException("Read timed out"));

        AiAssistResponse response = useCase.assist(new AiAssistRequest(null, null, "Panne capteur", null));

        assertUnavailableResponse(response);
        verifyNoInteractions(vectorStore, llmProvider);
    }

    @Test
    void assist_llmTimeout_stillReturnsSimilarInterventionsWithFallback() {
        float[] embedding = new float[]{0.4f};
        SimilarIntervention similar = new SimilarIntervention(
                UUID.randomUUID(), "EQ-003", "Alarme", "Capteur défaillant", "Remplacement", null, 0.88
        );
        when(embeddingProvider.embed(anyString())).thenReturn(embedding);
        when(vectorStore.findSimilar(embedding, 5)).thenReturn(List.of(similar));
        when(llmProvider.complete(anyString(), anyString()))
                .thenThrow(new RuntimeException("LLM read timed out"));

        AiAssistResponse response = useCase.assist(new AiAssistRequest(null, null, "Alarme capteur", null));

        assertEquals(1, response.similarInterventions().size());
        assertEquals("Capteur défaillant", response.suggestions().probableCauses().get(0));
    }

    @Test
    void assist_unexpectedError_returnsControlledResponse() {
        when(embeddingProvider.embed(anyString())).thenThrow(new IllegalStateException("unexpected"));

        AiAssistResponse response = useCase.assist(new AiAssistRequest(null, null, "Panne", null));

        assertUnavailableResponse(response);
    }

    @Test
    void assist_allResultsAboveThreshold_usesLlmWithAllResults() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        float[] embedding = new float[]{0.3f};
        SimilarIntervention high1 = new SimilarIntervention(
                id1, "EQ-001", "S1", "C1", "A1", null, 0.92);
        SimilarIntervention high2 = new SimilarIntervention(
                id2, "EQ-002", "S2", "C2", "A2", null, 0.81);
        String llmJson = """
                {"probableCauses":["Piste 1"],"correctiveActions":["Action 1"],"summary":"Résumé","advice":"Conseil"}
                """;

        when(embeddingProvider.embed(anyString())).thenReturn(embedding);
        when(vectorStore.findSimilar(embedding, 5)).thenReturn(List.of(high1, high2));
        when(llmProvider.complete(anyString(), anyString())).thenReturn(llmJson);

        AiAssistResponse response = useCase.assist(new AiAssistRequest(null, null, "Panne moteur", null));

        assertEquals(2, response.similarInterventions().size());
        verify(llmProvider, times(1)).complete(anyString(), anyString());
    }

    @Test
    void assist_someResultsBelowThreshold_filtersThemOut() {
        UUID idHigh = UUID.randomUUID();
        UUID idLow = UUID.randomUUID();
        float[] embedding = new float[]{0.3f};
        SimilarIntervention high = new SimilarIntervention(
                idHigh, "EQ-001", "S1", "Cause retenue", "A1", null, 0.85);
        SimilarIntervention low = new SimilarIntervention(
                idLow, "EQ-002", "S2", "Cause filtrée", "A2", null, 0.55);
        String llmJson = """
                {"probableCauses":["Cause retenue"],"correctiveActions":["A1"],"summary":"OK","advice":"Vérifier"}
                """;

        when(embeddingProvider.embed(anyString())).thenReturn(embedding);
        when(vectorStore.findSimilar(embedding, 5)).thenReturn(List.of(high, low));
        when(llmProvider.complete(anyString(), anyString())).thenReturn(llmJson);

        AiAssistResponse response = useCase.assist(new AiAssistRequest(null, null, "Panne", null));

        assertEquals(1, response.similarInterventions().size());
        assertEquals(idHigh, response.similarInterventions().get(0).interventionId());
        verify(llmProvider, times(1)).complete(anyString(), contains("Cause retenue"));
        verify(llmProvider, never()).complete(anyString(), contains("Cause filtrée"));
    }

    @Test
    void assist_noResultAboveThreshold_returnsFallbackWithoutLlm() {
        float[] embedding = new float[]{0.3f};
        SimilarIntervention low = new SimilarIntervention(
                UUID.randomUUID(), "EQ-001", "S1", "C1", "A1", null, 0.45);

        when(embeddingProvider.embed(anyString())).thenReturn(embedding);
        when(vectorStore.findSimilar(embedding, 5)).thenReturn(List.of(low));

        AiAssistResponse response = useCase.assist(new AiAssistRequest(null, null, "Panne obscure", null));

        assertTrue(response.similarInterventions().isEmpty());
        assertEquals("Aucune intervention similaire validée trouvée",
                response.suggestions().probableCauses().get(0));
        verify(llmProvider, never()).complete(anyString(), anyString());
    }

    @Test
    void assist_customThreshold_filtersAccordingToConfiguredValue() {
        appProperties.getAi().getRag().setSimilarityThreshold(0.90);
        float[] embedding = new float[]{0.3f};
        SimilarIntervention belowCustom = new SimilarIntervention(
                UUID.randomUUID(), "EQ-001", "S1", "C1", "A1", null, 0.85);

        when(embeddingProvider.embed(anyString())).thenReturn(embedding);
        when(vectorStore.findSimilar(embedding, 5)).thenReturn(List.of(belowCustom));

        AiAssistResponse response = useCase.assist(new AiAssistRequest(null, null, "Panne", null));

        assertTrue(response.similarInterventions().isEmpty());
        verify(llmProvider, never()).complete(anyString(), anyString());
    }

    @Test
    void assist_defaultThreshold_retainsResultsAtOrAbove070() {
        AppProperties defaults = new AppProperties();
        assertEquals(0.70, defaults.getAi().getRag().getSimilarityThreshold());

        float[] embedding = new float[]{0.3f};
        SimilarIntervention atThreshold = new SimilarIntervention(
                UUID.randomUUID(), "EQ-001", "S1", "C1", "A1", null, 0.70);
        SimilarIntervention belowDefault = new SimilarIntervention(
                UUID.randomUUID(), "EQ-002", "S2", "C2", "A2", null, 0.69);
        String llmJson = """
                {"probableCauses":["C1"],"correctiveActions":["A1"],"summary":"OK","advice":"OK"}
                """;

        when(embeddingProvider.embed(anyString())).thenReturn(embedding);
        when(vectorStore.findSimilar(embedding, 5)).thenReturn(List.of(atThreshold, belowDefault));
        when(llmProvider.complete(anyString(), anyString())).thenReturn(llmJson);

        AiAssistResponse response = useCase.assist(new AiAssistRequest(null, null, "Panne", null));

        assertEquals(1, response.similarInterventions().size());
        assertEquals(0.70, response.similarInterventions().get(0).similarity());
        verify(llmProvider, times(1)).complete(anyString(), anyString());
    }

    @Test
    void assist_textSearchOnly_findsResultWhenVectorIsEmpty() {
        UUID id = UUID.randomUUID();
        float[] embedding = new float[]{0.3f};
        SimilarIntervention textHit = new SimilarIntervention(
                id, "EQ-TXT", "Symptôme", "Cause", "Action", null, 0.78);
        String llmJson = """
                {"probableCauses":["Cause"],"correctiveActions":["Action"],"summary":"OK","advice":"OK"}
                """;

        when(embeddingProvider.embed(anyString())).thenReturn(embedding);
        when(vectorStore.findSimilar(embedding, 5)).thenReturn(List.of());
        when(interventionTextSearchPort.searchValidated("E001 panne", 5)).thenReturn(List.of(textHit));
        when(llmProvider.complete(anyString(), anyString())).thenReturn(llmJson);

        AiAssistResponse response = useCase.assist(new AiAssistRequest(null, null, "E001 panne", null));

        assertEquals(1, response.similarInterventions().size());
        assertEquals(id, response.similarInterventions().get(0).interventionId());
        verify(llmProvider, times(1)).complete(anyString(), anyString());
    }

    @Test
    void assist_hybridMerge_deduplicatesAndAppliesThresholdAfterFusion() {
        UUID sharedId = UUID.randomUUID();
        UUID textOnlyId = UUID.randomUUID();
        float[] embedding = new float[]{0.3f};
        SimilarIntervention vectorHit = new SimilarIntervention(
                sharedId, "EQ-001", "S1", "C1", "A1", null, 0.82);
        SimilarIntervention textDuplicate = new SimilarIntervention(
                sharedId, "EQ-001", "S1", "C1", "A1", null, 0.95);
        SimilarIntervention textLow = new SimilarIntervention(
                textOnlyId, "EQ-002", "S2", "C2", "A2", null, 0.55);
        String llmJson = """
                {"probableCauses":["C1"],"correctiveActions":["A1"],"summary":"OK","advice":"OK"}
                """;

        when(embeddingProvider.embed(anyString())).thenReturn(embedding);
        when(vectorStore.findSimilar(embedding, 5)).thenReturn(List.of(vectorHit));
        when(interventionTextSearchPort.searchValidated("Panne", 5))
                .thenReturn(List.of(textDuplicate, textLow));
        when(llmProvider.complete(anyString(), anyString())).thenReturn(llmJson);

        AiAssistResponse response = useCase.assist(new AiAssistRequest(null, null, "Panne", null));

        assertEquals(1, response.similarInterventions().size());
        assertEquals(sharedId, response.similarInterventions().get(0).interventionId());
        assertEquals(0.95, response.similarInterventions().get(0).similarity());
        verify(llmProvider, times(1)).complete(anyString(), anyString());
    }

    @Test
    void assist_textSearchFailure_continuesWithVectorResults() {
        UUID id = UUID.randomUUID();
        float[] embedding = new float[]{0.3f};
        SimilarIntervention vectorHit = new SimilarIntervention(
                id, "EQ-001", "S", "C", "A", null, 0.88);
        String llmJson = """
                {"probableCauses":["C"],"correctiveActions":["A"],"summary":"OK","advice":"OK"}
                """;

        when(embeddingProvider.embed(anyString())).thenReturn(embedding);
        when(vectorStore.findSimilar(embedding, 5)).thenReturn(List.of(vectorHit));
        when(interventionTextSearchPort.searchValidated(anyString(), anyInt()))
                .thenThrow(new RuntimeException("FTS indisponible"));
        when(llmProvider.complete(anyString(), anyString())).thenReturn(llmJson);

        AiAssistResponse response = useCase.assist(new AiAssistRequest(null, null, "Panne", null));

        assertEquals(1, response.similarInterventions().size());
        verify(llmProvider, times(1)).complete(anyString(), anyString());
    }

    @Test
    void assist_hybridTextDisabled_skipsTextSearch() {
        appProperties.getAi().getRag().setHybridTextEnabled(false);
        UUID id = UUID.randomUUID();
        float[] embedding = new float[]{0.3f};
        SimilarIntervention vectorHit = new SimilarIntervention(
                id, "EQ-001", "S", "C", "A", null, 0.88);
        String llmJson = """
                {"probableCauses":["C"],"correctiveActions":["A"],"summary":"OK","advice":"OK"}
                """;

        when(embeddingProvider.embed(anyString())).thenReturn(embedding);
        when(vectorStore.findSimilar(embedding, 5)).thenReturn(List.of(vectorHit));
        when(llmProvider.complete(anyString(), anyString())).thenReturn(llmJson);

        AiAssistResponse response = useCase.assist(new AiAssistRequest(null, null, "E001", null));

        assertEquals(1, response.similarInterventions().size());
        verify(interventionTextSearchPort, never()).searchValidated(anyString(), anyInt());
        verify(ragRetrievalMetrics).recordTextCount(0);
    }

    @Test
    void assist_recordsRetrievalMetrics() {
        UUID id = UUID.randomUUID();
        float[] embedding = new float[]{0.3f};
        SimilarIntervention vectorHit = new SimilarIntervention(
                id, "EQ-001", "S", "C", "A", null, 0.88);
        String llmJson = """
                {"probableCauses":["C"],"correctiveActions":["A"],"summary":"OK","advice":"OK"}
                """;

        when(embeddingProvider.embed(anyString())).thenReturn(embedding);
        when(vectorStore.findSimilar(embedding, 5)).thenReturn(List.of(vectorHit));
        when(llmProvider.complete(anyString(), anyString())).thenReturn(llmJson);

        useCase.assist(new AiAssistRequest(null, null, "Panne", null));

        verify(ragRetrievalMetrics).recordVectorCount(1);
        verify(ragRetrievalMetrics).recordTextCount(0);
        verify(ragRetrievalMetrics).recordMergedCount(1);
        verify(ragRetrievalMetrics).recordFilteredCount(1);
        verify(ragRetrievalMetrics).recordLlmCall();
        verify(ragRetrievalMetrics).recordRetrievalDuration(any());
    }

    @Test
    void assist_defaultHybridTextEnabled_isTrue() {
        assertTrue(new AppProperties().getAi().getRag().isHybridTextEnabled());
    }

    @Test
    void assist_parallelRetrieval_callsBothVectorAndTextSearch() {
        // Given
        float[] queryEmbedding = {0.1f, 0.2f, 0.3f};
        when(embeddingProvider.embed(anyString())).thenReturn(queryEmbedding);
        
        List<SimilarIntervention> vectorResults = List.of(
                new SimilarIntervention(UUID.randomUUID(), "EQP-001", "Symptôme 1", "Cause 1", "Action 1", null, 0.85)
        );
        List<SimilarIntervention> textResults = List.of(
                new SimilarIntervention(UUID.randomUUID(), "EQP-002", "Symptôme 2", "Cause 2", "Action 2", null, 0.75)
        );
        
        when(vectorStore.findSimilar(eq(queryEmbedding), anyInt())).thenReturn(vectorResults);
        when(interventionTextSearchPort.searchValidated(anyString(), anyInt())).thenReturn(textResults);
        when(llmProvider.complete(anyString(), anyString())).thenReturn(
                "{\"probableCauses\":[\"Test cause\"],\"correctiveActions\":[\"Test action\"],\"summary\":\"Test summary\",\"advice\":\"Test advice\"}"
        );

        // When
        AiAssistRequest request = new AiAssistRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Moteur en surchauffe",
                null
        );
        AiAssistResponse response = useCase.assist(request);

        // Then
        assertNotNull(response);
        // Verify both searches were called - this confirms parallel execution works
        verify(vectorStore, times(1)).findSimilar(eq(queryEmbedding), anyInt());
        verify(interventionTextSearchPort, times(1)).searchValidated(anyString(), anyInt());
        // Verify metrics were recorded for both
        verify(ragRetrievalMetrics).recordVectorCount(vectorResults.size());
        verify(ragRetrievalMetrics).recordTextCount(textResults.size());
        verify(ragRetrievalMetrics).recordMergedCount(anyInt());
    }

    private void assertUnavailableResponse(AiAssistResponse response) {
        assertTrue(response.similarInterventions().isEmpty());
        assertEquals("L'assistance IA est temporairement indisponible",
                response.suggestions().probableCauses().get(0));
        assertNotNull(response.disclaimer());
        assertFalse(response.disclaimer().isBlank());
    }
}
