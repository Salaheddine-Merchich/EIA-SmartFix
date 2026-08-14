package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.application.dto.AiDto.AiSuggestions;
import com.ocp.eia.config.AppProperties;
import com.ocp.eia.modules.knowledge.domain.model.QuerySignals;
import com.ocp.eia.modules.knowledge.domain.model.SimilarIntervention;
import com.ocp.eia.modules.knowledge.domain.model.SimilarKnowledgeDocument;
import com.ocp.eia.modules.knowledge.domain.port.LlmProviderPort;
import com.ocp.eia.modules.knowledge.infrastructure.observability.RagObservabilityService;
import com.ocp.eia.modules.knowledge.infrastructure.observability.RagRetrievalMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "app.knowledge.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class RagSuggestionService {

    private final AppProperties appProperties;
    private final RagPromptBuilder ragPromptBuilder;
    private final RagSuggestionParser ragSuggestionParser;
    private final LlmProviderPort llmProvider;
    private final RagRetrievalMetrics ragRetrievalMetrics;
    private final RagObservabilityService ragObservabilityService;

    public SuggestionResult generateSuggestions(
            String description,
            List<SimilarIntervention> similar,
            List<SimilarKnowledgeDocument> knowledgeDocuments
    ) {
        QuerySignals signals = QuerySignalExtractor.extract(description);
        double similarityThreshold = appProperties.getAi().getRag().getSimilarityThreshold();

        if (!AssistQueryValidator.isValid(description)) {
            log.info("RAG suggestion skipped: invalid query");
            return new SuggestionResult(ragSuggestionParser.vagueQueryFallback(), 0L, false);
        }

        if (!RagEvidencePolicy.hasProjectEvidence(similar, knowledgeDocuments, signals, similarityThreshold)) {
            log.info("RAG suggestion skipped: insufficient project evidence");
            ragObservabilityService.recordFallbackResponse();
            return new SuggestionResult(ragSuggestionParser.insufficientEvidenceFallback(), 0L, false);
        }

        if (shouldUseFastPath(similar)) {
            double topSimilarity = topSimilarity(similar);
            log.info("RAG fast path: topSimilarity={}, skipping LLM", topSimilarity);
            ragObservabilityService.recordFastPathResponse();
            return new SuggestionResult(
                    ragSuggestionParser.fallbackFromHistory(similar, knowledgeDocuments),
                    0L,
                    true
            );
        }

        double topSimilarity = topSimilarity(similar);
        if (!similar.isEmpty()) {
            log.info(
                    "RAG fast path skipped: topSimilarity={}, minSimilarity={}",
                    topSimilarity,
                    appProperties.getAi().getRag().getFastPathMinSimilarity()
            );
        }

        String systemPrompt = ragPromptBuilder.systemPrompt();
        String userPrompt = ragPromptBuilder.userPrompt(description, similar, knowledgeDocuments);
        log.info(
                "LLM complete start: systemPromptChars={}, userPromptChars={}, interventions={}, docs={}",
                systemPrompt.length(),
                userPrompt.length(),
                similar.size(),
                knowledgeDocuments.size()
        );
        if (log.isDebugEnabled()) {
            log.debug("LLM userPrompt prepared: chars={}", userPrompt.length());
        }

        long llmStart = System.nanoTime();
        try {
            ragRetrievalMetrics.recordLlmCall();
            String response = llmProvider.complete(systemPrompt, userPrompt);
            long durationMs = elapsedMs(llmStart);
            int responseChars = response != null ? response.length() : 0;
            log.info("LLM complete done: responseChars={}, durationMs={}", responseChars, durationMs);
            return new SuggestionResult(ragSuggestionParser.parse(response), durationMs, false);
        } catch (Exception e) {
            log.error("Erreur génération LLM after {}ms: {}", elapsedMs(llmStart), e.getMessage());
            if (similar.isEmpty()) {
                return new SuggestionResult(
                        ragSuggestionParser.insufficientEvidenceFallback(),
                        elapsedMs(llmStart),
                        false
                );
            }
            return new SuggestionResult(
                    ragSuggestionParser.fallbackFromHistory(similar, knowledgeDocuments),
                    elapsedMs(llmStart),
                    false
            );
        }
    }

    public boolean shouldUseFastPath(List<SimilarIntervention> similar) {
        if (!appProperties.getAi().getRag().isFastPathEnabled() || similar.isEmpty()) {
            return false;
        }
        double minSimilarity = appProperties.getAi().getRag().getFastPathMinSimilarity();
        return topSimilarity(similar) >= minSimilarity;
    }

    public boolean hasProjectEvidence(
            String description,
            List<SimilarIntervention> similar,
            List<SimilarKnowledgeDocument> knowledgeDocuments
    ) {
        QuerySignals signals = QuerySignalExtractor.extract(description);
        return RagEvidencePolicy.hasProjectEvidence(
                similar,
                knowledgeDocuments,
                signals,
                appProperties.getAi().getRag().getSimilarityThreshold()
        );
    }

    private static double topSimilarity(List<SimilarIntervention> similar) {
        return similar.stream().mapToDouble(SimilarIntervention::similarity).max().orElse(0.0);
    }

    private static long elapsedMs(long startNano) {
        return (System.nanoTime() - startNano) / 1_000_000L;
    }

    public record SuggestionResult(AiSuggestions suggestions, long llmDurationMs, boolean fastPathUsed) {}
}
