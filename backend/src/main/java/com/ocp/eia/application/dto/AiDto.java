package com.ocp.eia.application.dto;

import com.ocp.eia.application.validation.ValidAssistQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public final class AiDto {

    private AiDto() {}

    public record AiAssistRequest(
            UUID failureId,
            UUID equipmentId,
            @NotBlank @Size(max = 4000) @ValidAssistQuery String description,
            @Min(1) @Max(20) Integer topK
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

    public record EquipmentSchemaDto(
            UUID schemaId,
            UUID equipmentId,
            String equipmentCode,
            String equipmentDesignation,
            String label,
            String schemaType,
            String sourcePdf,
            Integer sourcePage,
            String caption,
            int totalSchemasForEquipment,
            String downloadUrl
    ) {}

    public record RetrievedSchemaDto(
            UUID schemaId,
            UUID equipmentId,
            String equipmentCode,
            String equipmentDesignation,
            String label,
            String schemaType,
            String sourcePdf,
            Integer sourcePage,
            String caption,
            int totalSchemasForEquipment,
            String downloadUrl
    ) {}

    public record AiDiagnosticTraceDto(
            String query,
            List<RetrievedDocumentDto> retrievedDocuments,
            List<RetrievedSchemaDto> retrievedSchemas,
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
            List<EquipmentSchemaDto> relevantSchemas,
            AiDiagnosticTraceDto diagnosticTrace
    ) {}

    public record ConversationSummaryDto(
            java.util.UUID id,
            String title,
            java.time.Instant updatedAt
    ) {}

    public record ConversationMessageDto(
            java.util.UUID id,
            String role,
            String content,
            AiAssistResponse payload,
            java.time.Instant createdAt
    ) {}

    public record ConversationDetailDto(
            java.util.UUID id,
            String title,
            java.time.Instant createdAt,
            java.time.Instant updatedAt,
            List<ConversationMessageDto> messages
    ) {}

    public record CreateConversationRequest(
            @Size(max = 120) String title
    ) {}

    public record AppendConversationMessagesRequest(
            @NotBlank @Size(max = 4000) String userContent,
            AiAssistResponse assistantResponse
    ) {}
}
