package com.ocp.eia.application.dto;



import jakarta.validation.constraints.NotBlank;



import java.util.List;

import java.util.UUID;



public final class AiDto {



    private AiDto() {}



    public record AiAssistRequest(

            UUID failureId,

            UUID equipmentId,

            @NotBlank String description,

            Integer topK

    ) {}



    public record AiSuggestions(

            List<String> probableCauses,

            List<String> correctiveActions,

            String summary,

            String advice

    ) {}



    public record SimilarInterventionDto(

            UUID interventionId,

            String equipmentCode,

            String symptomes,

            String causeRacine,

            String actionsCorrectives,

            double similarity

    ) {}



    public record RetrievedDocumentDto(

            UUID interventionId,

            String equipmentCode,

            String title,

            String detail,

            double similarityPercent

    ) {}



    public record RetrievalStepDto(

            String step,

            String status,

            String detail

    ) {}



    public record AiDiagnosticTraceDto(

            String query,

            List<RetrievedDocumentDto> retrievedDocuments,

            List<RetrievalStepDto> retrievalSteps,

            int vectorResultCount,

            int textResultCount,

            int mergedResultCount,

            int filteredCount,

            double averageSimilarity,

            double confidenceScore,

            String confidenceLevel,

            long retrievalDurationMs,

            long llmDurationMs

    ) {}



    public record AiAssistResponse(

            List<SimilarInterventionDto> similarInterventions,

            AiSuggestions suggestions,

            String disclaimer,

            AiDiagnosticTraceDto diagnosticTrace

    ) {}

}

