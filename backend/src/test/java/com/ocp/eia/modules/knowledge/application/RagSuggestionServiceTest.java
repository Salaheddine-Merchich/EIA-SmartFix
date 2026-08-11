package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.application.dto.AiDto.AiSuggestions;
import com.ocp.eia.config.AppProperties;
import com.ocp.eia.modules.knowledge.domain.model.SimilarIntervention;
import com.ocp.eia.modules.knowledge.domain.port.LlmProviderPort;
import com.ocp.eia.modules.knowledge.infrastructure.observability.RagObservabilityService;
import com.ocp.eia.modules.knowledge.infrastructure.observability.RagRetrievalMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagSuggestionServiceTest {

    @Mock private AppProperties appProperties;
    @Mock private AppProperties.Ai ai;
    @Mock private AppProperties.Ai.Rag rag;
    @Mock private RagPromptBuilder ragPromptBuilder;
    @Mock private RagSuggestionParser ragSuggestionParser;
    @Mock private LlmProviderPort llmProvider;
    @Mock private RagRetrievalMetrics ragRetrievalMetrics;
    @Mock private RagObservabilityService ragObservabilityService;

    @InjectMocks private RagSuggestionService service;

    @BeforeEach
    void setUp() {
        lenient().when(appProperties.getAi()).thenReturn(ai);
        lenient().when(ai.getRag()).thenReturn(rag);
        lenient().when(rag.isFastPathEnabled()).thenReturn(true);
        lenient().when(rag.getFastPathMinSimilarity()).thenReturn(0.85);
    }

    @Test
    void generateSuggestions_highSimilarity_skipsLlm() {
        SimilarIntervention similar = similar(0.90);
        AiSuggestions historySuggestions = new AiSuggestions(
                List.of("Cause historique"),
                List.of("Action historique"),
                "Résumé",
                "Conseil"
        );
        when(ragSuggestionParser.fallbackFromHistory(eq(List.of(similar)), eq(List.of())))
                .thenReturn(historySuggestions);

        RagSuggestionService.SuggestionResult result = service.generateSuggestions(
                "Surchauffe moteur",
                List.of(similar),
                List.of()
        );

        assertTrue(result.fastPathUsed());
        assertEquals(0L, result.llmDurationMs());
        assertEquals(historySuggestions, result.suggestions());
        verify(llmProvider, never()).complete(anyString(), anyString());
        verify(ragObservabilityService).recordFastPathResponse();
        verify(ragRetrievalMetrics, never()).recordLlmCall();
    }

    @Test
    void generateSuggestions_lowSimilarity_usesLlm() throws Exception {
        SimilarIntervention similar = similar(0.75);
        AiSuggestions llmSuggestions = new AiSuggestions(
                List.of("Cause LLM"),
                List.of("Action LLM"),
                "Résumé LLM",
                "Conseil LLM"
        );
        when(ragPromptBuilder.systemPrompt()).thenReturn("system");
        when(ragPromptBuilder.userPrompt(eq("Panne"), eq(List.of(similar)), eq(List.of()))).thenReturn("user");
        when(llmProvider.complete("system", "user")).thenReturn("{\"ok\":true}");
        when(ragSuggestionParser.parse("{\"ok\":true}")).thenReturn(llmSuggestions);

        RagSuggestionService.SuggestionResult result = service.generateSuggestions(
                "Panne",
                List.of(similar),
                List.of()
        );

        assertFalse(result.fastPathUsed());
        assertEquals(llmSuggestions, result.suggestions());
        verify(llmProvider).complete("system", "user");
        verify(ragRetrievalMetrics).recordLlmCall();
        verify(ragObservabilityService, never()).recordFastPathResponse();
    }

    @Test
    void generateSuggestions_fastPathDisabled_usesLlmEvenWithHighSimilarity() throws Exception {
        when(rag.isFastPathEnabled()).thenReturn(false);
        SimilarIntervention similar = similar(0.92);
        AiSuggestions llmSuggestions = new AiSuggestions(
                List.of("Cause LLM"),
                List.of("Action LLM"),
                "Résumé",
                "Conseil"
        );
        when(ragPromptBuilder.systemPrompt()).thenReturn("system");
        when(ragPromptBuilder.userPrompt(anyString(), eq(List.of(similar)), eq(List.of()))).thenReturn("user");
        when(llmProvider.complete("system", "user")).thenReturn("{\"ok\":true}");
        when(ragSuggestionParser.parse("{\"ok\":true}")).thenReturn(llmSuggestions);

        RagSuggestionService.SuggestionResult result = service.generateSuggestions(
                "Surchauffe moteur",
                List.of(similar),
                List.of()
        );

        assertFalse(result.fastPathUsed());
        verify(llmProvider).complete("system", "user");
        verify(ragObservabilityService, never()).recordFastPathResponse();
    }

    @Test
    void generateSuggestions_usesMaxSimilarityNotFirstElement() {
        SimilarIntervention lowerFirst = similar(0.78);
        SimilarIntervention higherSecond = similar(0.91);
        AiSuggestions historySuggestions = new AiSuggestions(
                List.of("Cause historique"),
                List.of("Action historique"),
                "Résumé",
                "Conseil"
        );
        when(ragSuggestionParser.fallbackFromHistory(
                eq(List.of(lowerFirst, higherSecond)), eq(List.of()))).thenReturn(historySuggestions);

        RagSuggestionService.SuggestionResult result = service.generateSuggestions(
                "Panne",
                List.of(lowerFirst, higherSecond),
                List.of()
        );

        assertTrue(result.fastPathUsed());
        verify(llmProvider, never()).complete(anyString(), anyString());
    }

    private static SimilarIntervention similar(double score) {
        return new SimilarIntervention(
                UUID.randomUUID(), "EQ-1", "Symptômes", "Cause", "Actions", "Analyse", score);
    }
}
