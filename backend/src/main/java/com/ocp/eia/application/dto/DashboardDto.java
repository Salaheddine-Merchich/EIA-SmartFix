package com.ocp.eia.application.dto;

import java.util.List;
import java.util.UUID;

public final class DashboardDto {

    private DashboardDto() {}

    public record SearchResponse(
            List<InterventionDto.InterventionResponse> interventions,
            long totalElements,
            int totalPages,
            int page
    ) {}

    public record AiReliabilityStats(
            long diagnosticsCount,
            double averageConfidence,
            long totalRetrievals
    ) {}

    public record DashboardResponse(
            long totalFailures,
            long openFailures,
            long criticalOpenFailures,
            long equipmentCount,
            long validatedInterventions,
            long pendingValidations,
            long draftInterventions,
            long rejectedInterventions,
            long knowledgeDocuments,
            long activeKnowledgeDocuments,
            long activeEquipmentSchemas,
            long indexedInterventions,
            Double mttrMinutes,
            Double mtbfHours,
            List<TopEquipmentItem> topFailingEquipment,
            List<CauseItem> topCauses,
            List<FamilleItem> failuresByFamille,
            List<MonthlyTrendItem> failuresByMonth,
            AiReliabilityStats aiReliability
    ) {}

    public record TopEquipmentItem(UUID equipmentId, String code, String designation, long failureCount) {}

    public record CauseItem(String cause, long count) {}

    public record FamilleItem(String famille, long count) {}

    public record MonthlyTrendItem(String month, long count) {}
}
