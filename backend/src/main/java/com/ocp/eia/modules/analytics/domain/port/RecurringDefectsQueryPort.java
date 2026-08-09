package com.ocp.eia.modules.analytics.domain.port;

import com.ocp.eia.application.dto.AnalyticsDto.RecurringDefectItem;

import java.util.List;

public interface RecurringDefectsQueryPort {

    List<RecurringDefectItem> findRecurringDefects(int limit);

    long countRecurringDefectCodes();
}
