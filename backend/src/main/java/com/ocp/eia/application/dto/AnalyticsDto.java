package com.ocp.eia.application.dto;

import java.util.List;

public final class AnalyticsDto {

    private AnalyticsDto() {}

    public record RecurringDefectItem(
            String codeDefaut,
            long occurrenceCount,
            long affectedEquipmentCount,
            String lastSeenMonth
    ) {}

    public record RecurringDefectsResponse(
            List<RecurringDefectItem> defects,
            long totalRecurringCodes
    ) {}

    public record RecurringDefectsAnalysisResponse(
            List<RecurringDefectItem> defects,
            String analysis,
            String recommendations,
            String disclaimer
    ) {}
}
