package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.application.dto.AiDto.AiAssistRequest;
import com.ocp.eia.application.dto.AiDto.AiAssistResponse;
import com.ocp.eia.application.dto.AiDto.AiSuggestions;
import com.ocp.eia.modules.knowledge.application.RagRetrievalService.RetrievalOutcome;
import com.ocp.eia.modules.knowledge.domain.model.SearchContext;
import com.ocp.eia.modules.knowledge.domain.model.SimilarIntervention;
import com.ocp.eia.modules.knowledge.infrastructure.observability.RagObservabilityService;
import com.ocp.eia.modules.knowledge.infrastructure.observability.RagRetrievalMetrics;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagAssistTraceTest {

    @Mock private RagRetrievalService ragRetrievalService;
    @Mock private RagSuggestionService ragSuggestionService;
    @Mock private RagSuggestionParser ragSuggestionParser;
    @Mock private RagRetrievalMetrics ragRetrievalMetrics;
    @Mock private RagObservabilityService ragObservabilityService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private AiDiagnosticStatsService diagnosticStatsService;
    @Mock private EquipmentSchemaMatcher equipmentSchemaMatcher;
    @Mock private SearchContextFactory searchContextFactory;

    @InjectMocks private RagAssistUseCase useCase;

    @BeforeEach
    void setUp() {
        Timer.Sample sample = mock(Timer.Sample.class);
        lenient().when(ragRetrievalMetrics.startRetrievalTimer()).thenReturn(sample);
        lenient().doNothing().when(ragRetrievalMetrics).recordRetrievalDuration(any());
        lenient().when(searchContextFactory.from(any(), any())).thenReturn(SearchContext.none());
        lenient().when(equipmentSchemaMatcher.match(any(), any())).thenReturn(List.of());
    }

    @Test
    void assist_includesDiagnosticTraceWithSources() {
        UUID interventionId = UUID.randomUUID();
        SimilarIntervention similar = new SimilarIntervention(
                interventionId, "CV-101", "Surchauffe convoyeur", "Code E001", "Remplacement", "Analyse", 0.91
        );
        AiAssistRequest request = new AiAssistRequest(null, null, "Panne convoyeur", null);
        AiSuggestions suggestions = new AiSuggestions(
                List.of("Cause"), List.of("Action"), "Résumé", "Conseil"
        );

        when(ragRetrievalService.retrieve(request)).thenReturn(new RetrievalOutcome(
                List.of(similar), List.of(), List.of(similar), List.of(),
                1, 0, 1, "OK", true, 8L, false, null, false, null
        ));
        when(ragSuggestionService.generateSuggestions(eq("Panne convoyeur"), eq(List.of(similar)), eq(List.of())))
                .thenReturn(new RagSuggestionService.SuggestionResult(suggestions, 50L, false));

        AiAssistResponse response = useCase.assist(request);

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
        AiAssistRequest request = new AiAssistRequest(null, null, "Test", null);
        when(ragRetrievalService.retrieve(request)).thenReturn(
                RetrievalOutcome.unavailable(true, 3L, "FAILED", "embedding")
        );

        AiAssistResponse response = useCase.assist(request);

        assertNotNull(response.diagnosticTrace());
        assertEquals("FAILED", response.diagnosticTrace().retrievalSteps().get(0).status());
        assertEquals(0, response.diagnosticTrace().filteredCount());
    }
}
