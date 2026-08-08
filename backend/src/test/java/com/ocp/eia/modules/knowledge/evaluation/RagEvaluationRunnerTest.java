package com.ocp.eia.modules.knowledge.evaluation;

import com.ocp.eia.config.AppProperties;
import com.ocp.eia.modules.knowledge.domain.model.SimilarIntervention;
import com.ocp.eia.modules.knowledge.domain.port.EmbeddingProviderPort;
import com.ocp.eia.modules.knowledge.domain.port.InterventionTextSearchPort;
import com.ocp.eia.modules.knowledge.domain.port.LlmProviderPort;
import com.ocp.eia.modules.knowledge.domain.port.VectorStorePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RagEvaluationRunnerTest {

    @Mock private EmbeddingProviderPort embeddingProvider;
    @Mock private VectorStorePort vectorStore;
    @Mock private InterventionTextSearchPort textSearchPort;
    @Mock private LlmProviderPort llmProvider;

    private AppProperties appProperties;
    private RagEvaluationRunner runner;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.getAi().getRag().setTopK(5);
        appProperties.getAi().getRag().setSimilarityThreshold(0.70);
        appProperties.getAi().getRag().setHybridTextEnabled(true);
        runner = new RagEvaluationRunner(
                embeddingProvider, vectorStore, textSearchPort, llmProvider, appProperties);
    }

    @Test
    void evaluateCase_hitAt1_whenExpectedInTopFilteredResults() {
        UUID expected = UUID.randomUUID();
        float[] embedding = new float[]{0.5f};
        SimilarIntervention hit = row(expected, 0.92);
        SimilarIntervention low = row(UUID.randomUUID(), 0.40);

        when(embeddingProvider.embed(anyString())).thenReturn(embedding);
        when(vectorStore.findSimilar(embedding, 5)).thenReturn(List.of(hit));
        when(textSearchPort.searchValidated(anyString(), eq(5))).thenReturn(List.of(low));
        when(llmProvider.complete(anyString(), anyString())).thenReturn("OK");

        RagEvaluationCase evalCase = new RagEvaluationCase(
                "test-hit", "Le convoyeur Siemens affiche le code E001", expected, "test");
        RagEvaluationResult result = runner.evaluateCase(evalCase);

        assertTrue(result.success());
        assertTrue(result.hitAt1());
        assertEquals(1, result.rankOfExpected());
        assertEquals(1.0, result.reciprocalRank());
        assertTrue(result.timings().totalMs() >= 0);
        verify(llmProvider).complete(anyString(), anyString());
    }

    @Test
    void evaluateCase_textOnlyHit_whenVectorEmpty() {
        UUID expected = UUID.randomUUID();
        float[] embedding = new float[]{0.1f};
        SimilarIntervention textHit = row(expected, 0.78);

        when(embeddingProvider.embed(anyString())).thenReturn(embedding);
        when(vectorStore.findSimilar(embedding, 5)).thenReturn(List.of());
        when(textSearchPort.searchValidated("E001", 5)).thenReturn(List.of(textHit));
        when(llmProvider.complete(anyString(), anyString())).thenReturn("OK");

        RagEvaluationCase evalCase = new RagEvaluationCase("e001", "E001", expected, "code défaut");
        RagEvaluationResult result = runner.evaluateCase(evalCase);

        assertTrue(result.hitAt1());
        assertEquals(1, result.filteredCount());
        assertTrue(result.timings().textSearchMs() >= 0);
    }

    @Test
    void evaluateCase_hybridDisabled_skipsTextSearch() {
        appProperties.getAi().getRag().setHybridTextEnabled(false);
        UUID expected = UUID.randomUUID();
        float[] embedding = new float[]{0.2f};
        SimilarIntervention vectorHit = row(expected, 0.85);

        when(embeddingProvider.embed(anyString())).thenReturn(embedding);
        when(vectorStore.findSimilar(embedding, 5)).thenReturn(List.of(vectorHit));
        when(llmProvider.complete(anyString(), anyString())).thenReturn("OK");

        RagEvaluationResult result = runner.evaluateCase(
                new RagEvaluationCase("v", "Panne", expected, "d"));

        verify(textSearchPort, never()).searchValidated(anyString(), anyInt());
        assertEquals(0, result.timings().textSearchMs());
        assertTrue(result.hitAt1());
    }

    @Test
    void run_producesAggregatedReport() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        float[] embedding = new float[]{0.3f};

        when(embeddingProvider.embed(anyString())).thenReturn(embedding);
        when(vectorStore.findSimilar(any(), anyInt())).thenReturn(List.of(row(id1, 0.90)));
        when(textSearchPort.searchValidated(anyString(), anyInt())).thenReturn(List.of());
        when(llmProvider.complete(anyString(), anyString())).thenReturn("OK");

        List<RagEvaluationCase> cases = List.of(
                new RagEvaluationCase("c1", "Q1", id1, "d1"),
                new RagEvaluationCase("c2", "Q2", id2, "d2")
        );

        RagEvaluationReport report = runner.run(cases);

        assertEquals(2, report.questionCount());
        assertEquals(50.0, report.precisionAt1Percent());
        assertNotNull(report.toTextReport());
        assertTrue(report.toTextReport().contains("Questions"));
    }

    @Test
    void evaluateCase_failureWhenEmbeddingThrows() {
        when(embeddingProvider.embed(anyString())).thenThrow(new RuntimeException("Ollama down"));

        RagEvaluationResult result = runner.evaluateCase(
                new RagEvaluationCase("fail", "Q", UUID.randomUUID(), "d"));

        assertFalse(result.success());
        assertNotNull(result.errorMessage());
    }

    @Test
    void evaluateCase_withoutLlm_skipsLlmTiming() {
        runner = new RagEvaluationRunner(
                embeddingProvider, vectorStore, textSearchPort, llmProvider, appProperties, false);
        UUID expected = UUID.randomUUID();
        float[] embedding = new float[]{0.2f};

        when(embeddingProvider.embed(anyString())).thenReturn(embedding);
        when(vectorStore.findSimilar(embedding, 5)).thenReturn(List.of(row(expected, 0.88)));

        RagEvaluationResult result = runner.evaluateCase(
                new RagEvaluationCase("no-llm", "Q", expected, "d"));

        assertEquals(0, result.timings().llmMs());
        verify(llmProvider, never()).complete(anyString(), anyString());
    }

    private static SimilarIntervention row(UUID id, double similarity) {
        return new SimilarIntervention(id, "EQ-001", "S", "C", "A", null, similarity);
    }
}
