package com.ocp.eia.modules.analytics.application;

import com.ocp.eia.application.dto.AnalyticsDto.RecurringDefectItem;
import com.ocp.eia.application.dto.AnalyticsDto.RecurringDefectsResponse;
import com.ocp.eia.modules.knowledge.domain.LlmUnavailableException;
import com.ocp.eia.modules.knowledge.domain.port.LlmProviderPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyzeRecurringDefectsUseCaseTest {

    @Mock
    private RecurringDefectsUseCase recurringDefectsUseCase;

    private LlmProviderPort llm;
    private ObjectProvider<LlmProviderPort> llmProvider;
    private AnalyzeRecurringDefectsUseCase useCase;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        llm = mock(LlmProviderPort.class);
        llmProvider = mock(ObjectProvider.class);
        useCase = new AnalyzeRecurringDefectsUseCase(recurringDefectsUseCase, llmProvider);
    }

    @Test
    void execute_whenLlmUnavailable_usesFrenchAggregatedFallback() {
        var defect = new RecurringDefectItem("DEF-01", 4L, 2L, "2026-07");
        when(recurringDefectsUseCase.execute(anyInt()))
                .thenReturn(new RecurringDefectsResponse(List.of(defect), 1L));
        when(llmProvider.getIfAvailable()).thenReturn(llm);
        when(llm.complete(anyString(), anyString()))
                .thenThrow(new LlmUnavailableException("Circuit breaker LLM ouvert"));

        var result = useCase.execute(10);

        assertTrue(result.analysis().contains("DEF-01"));
        assertFalse(result.analysis().contains("probableCauses"));
        assertTrue(result.recommendations().contains("DEF-01"));
    }

    @Test
    void execute_whenLlmReturnsJsonBlob_usesFrenchFallback() {
        var defect = new RecurringDefectItem("DEF-02", 3L, 1L, "2026-06");
        when(recurringDefectsUseCase.execute(anyInt()))
                .thenReturn(new RecurringDefectsResponse(List.of(defect), 1L));
        when(llmProvider.getIfAvailable()).thenReturn(llm);
        when(llm.complete(anyString(), anyString())).thenReturn("""
                {"probableCauses":["x"],"correctiveActions":["y"],"summary":"z"}
                """);

        var result = useCase.execute(5);

        assertTrue(result.analysis().contains("DEF-02"));
        assertFalse(result.analysis().contains("{"));
    }

    @Test
    void execute_whenLlmAbsent_doesNotCallComplete() {
        var defect = new RecurringDefectItem("DEF-03", 2L, 1L, "2026-05");
        when(recurringDefectsUseCase.execute(anyInt()))
                .thenReturn(new RecurringDefectsResponse(List.of(defect), 1L));
        when(llmProvider.getIfAvailable()).thenReturn(null);

        var result = useCase.execute(5);

        assertEquals(1, result.defects().size());
        assertTrue(result.analysis().contains("DEF-03"));
        verify(llm, never()).complete(anyString(), anyString());
    }

    @Test
    void execute_whenLlmReturnsStructuredSections_parsesThem() {
        var defect = new RecurringDefectItem("DEF-04", 5L, 3L, "2026-08");
        when(recurringDefectsUseCase.execute(anyInt()))
                .thenReturn(new RecurringDefectsResponse(List.of(defect), 1L));
        when(llmProvider.getIfAvailable()).thenReturn(llm);
        when(llm.complete(anyString(), anyString())).thenReturn("""
                ANALYSE: Schéma de répétition clair sur DEF-04.
                RECOMMANDATIONS:
                • Planifier une campagne de contrôle
                """);

        var result = useCase.execute(5);

        assertEquals("Schéma de répétition clair sur DEF-04.", result.analysis());
        assertTrue(result.recommendations().contains("campagne"));
    }
}
