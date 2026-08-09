package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.application.dto.AiDto.AiAssistResponse;
import com.ocp.eia.application.dto.AiDto.AiSuggestions;
import com.ocp.eia.application.dto.AiDto.SimilarInterventionDto;
import com.ocp.eia.modules.knowledge.domain.model.AiDiagnosticTrace;
import com.ocp.eia.modules.knowledge.domain.model.RetrievedDocument;
import com.ocp.eia.modules.knowledge.domain.model.SimilarIntervention;

import java.util.List;

/**
 * Shared RAG diagnostic trace construction and response DTO mapping
 * for {@link RagAssistUseCase} and {@link RagAssistStreamUseCase}.
 */
public final class AiDiagnosticTraceFactory {

    public static final String DISCLAIMER =
            "Assistance uniquement — les décisions finales restent celles du technicien ou de l'ingénieur.";

    private AiDiagnosticTraceFactory() {}

    public static List<SimilarInterventionDto> toSimilarDtos(List<SimilarIntervention> relevant) {
        return relevant.stream()
                .map(s -> new SimilarInterventionDto(
                        s.interventionId(),
                        s.equipmentCode(),
                        s.symptomes(),
                        s.causeRacine(),
                        s.actionsCorrectives(),
                        s.similarity()
                ))
                .toList();
    }

    public static AiDiagnosticTrace buildTrace(
            String query,
            List<SimilarIntervention> relevant,
            int vectorCount,
            int textCount,
            int mergedCount,
            String embeddingStatus,
            boolean hybridEnabled,
            long retrievalDurationMs,
            long llmDurationMs
    ) {
        List<RetrievedDocument> documents = relevant.stream()
                .map(s -> new RetrievedDocument(
                        s.interventionId(),
                        s.equipmentCode(),
                        s.symptomes(),
                        s.causeRacine(),
                        s.similarity()
                ))
                .toList();

        double averageSimilarity = relevant.isEmpty()
                ? 0.0
                : relevant.stream().mapToDouble(SimilarIntervention::similarity).average().orElse(0.0);
        double confidenceScore = ConfidenceCalculator.compute(averageSimilarity, relevant.size());

        return new AiDiagnosticTrace(
                query,
                documents,
                vectorCount,
                textCount,
                mergedCount,
                relevant.size(),
                averageSimilarity,
                confidenceScore,
                retrievalDurationMs,
                llmDurationMs,
                embeddingStatus,
                hybridEnabled
        );
    }

    public static AiAssistResponse toResponse(
            List<SimilarIntervention> relevant,
            AiSuggestions suggestions,
            AiDiagnosticTrace trace
    ) {
        return new AiAssistResponse(
                toSimilarDtos(relevant),
                suggestions,
                DISCLAIMER,
                AiDiagnosticTraceMapper.toDto(trace)
        );
    }
}
