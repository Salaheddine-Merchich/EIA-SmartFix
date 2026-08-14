package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.application.dto.AiDto.AiAssistRequest;
import com.ocp.eia.application.dto.AiDto.AiAssistResponse;
import com.ocp.eia.application.dto.AiDto.AiSuggestions;
import com.ocp.eia.config.AppProperties;
import com.ocp.eia.modules.knowledge.application.RagRetrievalService.RetrievalOutcome;
import com.ocp.eia.modules.knowledge.domain.model.SimilarIntervention;
import com.ocp.eia.modules.knowledge.domain.model.SimilarKnowledgeDocument;
import com.ocp.eia.modules.knowledge.domain.port.LlmProviderPort;
import com.ocp.eia.modules.knowledge.infrastructure.observability.RagObservabilityService;
import com.ocp.eia.modules.knowledge.infrastructure.observability.RagRetrievalMetrics;
import com.ocp.eia.modules.monitoring.application.event.AiServiceUnavailableEvent;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagAssistUseCaseTest {

    @Mock private RagRetrievalService ragRetrievalService;
    @Mock private RagSuggestionService ragSuggestionService;
    @Mock private RagSuggestionParser ragSuggestionParser;
    @Mock private RagRetrievalMetrics ragRetrievalMetrics;
    @Mock private RagObservabilityService ragObservabilityService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private AiDiagnosticStatsService diagnosticStatsService;

    @InjectMocks private RagAssistUseCase useCase;

    @BeforeEach
    void setUp() {
        Timer.Sample sample = mock(Timer.Sample.class);
        lenient().when(ragRetrievalMetrics.startRetrievalTimer()).thenReturn(sample);
        lenient().doNothing().when(ragRetrievalMetrics).recordRetrievalDuration(any());
    }

    @Test
    void assist_noEvidence_returnsFallbackSuggestions() {
        AiAssistRequest request = new AiAssistRequest(null, null, "Panne variateur", null);
        AiSuggestions fallback = new AiSuggestions(
                List.of("Aucune intervention similaire validée trouvée"),
                List.of("Consulter la documentation constructeur"),
                "Pas assez de données",
                "Documentez"
        );
        when(ragRetrievalService.retrieve(request)).thenReturn(successOutcome(List.of(), 0, 0, 0));
        when(ragSuggestionService.generateSuggestions(eq("Panne variateur"), eq(List.of()), eq(List.of())))
                .thenReturn(new RagSuggestionService.SuggestionResult(fallback, 0L, false));

        AiAssistResponse response = useCase.assist(request);

        assertTrue(response.similarInterventions().isEmpty());
        assertEquals("Aucune intervention similaire validée trouvée", response.suggestions().probableCauses().get(0));
        verify(ragObservabilityService).recordSuccessfulQuery();
        verify(diagnosticStatsService).record(any());
    }

    @Test
    void assist_withSimilarInterventions_usesLlmAndReturnsSuggestions() throws Exception {
        AiAssistRequest request = new AiAssistRequest(null, null, "Défaut moteur", 3);
        UUID interventionId = UUID.randomUUID();
        SimilarIntervention similar = similar(interventionId, 0.88);
        AiSuggestions suggestions = new AiSuggestions(
                List.of("Cause moteur"),
                List.of("Action moteur"),
                "Résumé",
                "Conseil"
        );

        when(ragRetrievalService.retrieve(request)).thenReturn(successOutcome(List.of(similar), 1, 0, 1));
        when(ragSuggestionService.generateSuggestions(eq("Défaut moteur"), eq(List.of(similar)), eq(List.of())))
                .thenReturn(new RagSuggestionService.SuggestionResult(suggestions, 1200L, false));

        AiAssistResponse response = useCase.assist(request);

        assertEquals(1, response.similarInterventions().size());
        assertEquals(interventionId, response.similarInterventions().get(0).interventionId());
        assertEquals(suggestions, response.suggestions());
        verify(ragObservabilityService).incrementActiveQueries();
        verify(ragObservabilityService).decrementActiveQueries();
    }

    @Test
    void assist_highSimilarity_skipsLlmViaFastPath() {
        AiAssistRequest request = new AiAssistRequest(null, null, "Surchauffe moteur", null);
        SimilarIntervention similar = similar(UUID.randomUUID(), 0.90);
        AiSuggestions fastPathSuggestions = new AiSuggestions(
                List.of("Roulement arriere grippe"),
                List.of("Remplacement roulement SKF 6312"),
                "Historique similaire",
                "Valider avant intervention"
        );

        when(ragRetrievalService.retrieve(request)).thenReturn(successOutcome(List.of(similar), 1, 0, 1));
        when(ragSuggestionService.generateSuggestions(eq("Surchauffe moteur"), eq(List.of(similar)), eq(List.of())))
                .thenReturn(new RagSuggestionService.SuggestionResult(fastPathSuggestions, 0L, true));

        AiAssistResponse response = useCase.assist(request);

        assertEquals(fastPathSuggestions, response.suggestions());
        assertEquals(1, response.similarInterventions().size());
        verify(ragObservabilityService).recordSuccessfulQuery();
    }

    @Test
    void assist_retrievalUnavailable_returnsControlledResponseAndPublishesEvent() {
        AiAssistRequest request = new AiAssistRequest(null, null, "Panne", null);
        when(ragRetrievalService.retrieve(request)).thenReturn(
                RetrievalOutcome.unavailable(true, 12L, "FAILED", "embedding")
        );

        AiAssistResponse response = useCase.assist(request);

        assertTrue(response.similarInterventions().isEmpty());
        assertEquals("L'assistance IA est temporairement indisponible",
                response.suggestions().probableCauses().get(0));
        assertEquals("FAILED", response.diagnosticTrace().retrievalSteps().get(0).status());
        verify(eventPublisher).publishEvent(any(AiServiceUnavailableEvent.class));
        verify(ragObservabilityService).recordFallbackResponse();
        verify(ragSuggestionService, never()).generateSuggestions(anyString(), any(), any());
    }

    @Test
    void assist_llmFailure_fallsBackToHistory() {
        AiAssistRequest request = new AiAssistRequest(null, null, "Panne", null);
        SimilarIntervention similar = similar(UUID.randomUUID(), 0.75);
        AiSuggestions fallback = new AiSuggestions(
                List.of("Cause historique"),
                List.of("Action historique"),
                "Fallback",
                "Conseil"
        );

        when(ragRetrievalService.retrieve(request)).thenReturn(successOutcome(List.of(similar), 1, 0, 1));
        when(ragSuggestionService.generateSuggestions(eq("Panne"), eq(List.of(similar)), eq(List.of())))
                .thenReturn(new RagSuggestionService.SuggestionResult(fallback, 500L, false));

        AiAssistResponse response = useCase.assist(request);

        assertEquals(fallback, response.suggestions());
        assertEquals(1, response.similarInterventions().size());
        verify(ragObservabilityService).recordSuccessfulQuery();
    }

    @Test
    void assist_codeNotFound_returnsUnknownCodeFallbackWithoutLlm() {
        AiAssistRequest request = new AiAssistRequest(null, null, "F001 surchauffe convoyeur", null);
        AiSuggestions unknownCode = new AiSuggestions(
                List.of("Le code défaut F001 n'existe pas dans la base de connaissances validée"),
                List.of("Vérifier le code affiché"),
                "Aucune intervention validée ne correspond au code F001.",
                "Documentez l'intervention"
        );
        when(ragRetrievalService.retrieve(request)).thenReturn(
                RetrievalOutcome.codeNotFound("F001", true, 3L)
        );
        when(ragSuggestionParser.unknownFaultCodeFallback("F001")).thenReturn(unknownCode);

        AiAssistResponse response = useCase.assist(request);

        assertTrue(response.similarInterventions().isEmpty());
        assertTrue(response.suggestions().probableCauses().get(0).contains("F001"));
        verify(ragSuggestionService, never()).generateSuggestions(anyString(), any(), any());
        verify(ragObservabilityService).recordFallbackResponse();
    }

    private static SimilarIntervention similar(UUID id, double score) {
        return new SimilarIntervention(id, "EQ-1", "Symptômes", "Cause", "Actions", "Analyse", score);
    }

    private static RetrievalOutcome successOutcome(
            List<SimilarIntervention> relevant,
            int vectorCount,
            int textCount,
            int mergedCount
    ) {
        return new RetrievalOutcome(
                relevant,
                List.of(),
                relevant,
                List.of(),
                vectorCount,
                textCount,
                mergedCount,
                "OK",
                true,
                5L,
                false,
                null,
                false,
                null
        );
    }
}
