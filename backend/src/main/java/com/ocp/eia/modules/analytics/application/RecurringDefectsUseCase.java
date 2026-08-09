package com.ocp.eia.modules.analytics.application;

import com.ocp.eia.application.dto.AnalyticsDto.RecurringDefectItem;
import com.ocp.eia.application.dto.AnalyticsDto.RecurringDefectsResponse;
import com.ocp.eia.modules.analytics.domain.port.RecurringDefectsQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecurringDefectsUseCase {

    private final RecurringDefectsQueryPort recurringDefectsQueryPort;

    public RecurringDefectsResponse execute(int limit) {
        List<RecurringDefectItem> defects = recurringDefectsQueryPort.findRecurringDefects(limit);
        long totalRecurring = recurringDefectsQueryPort.countRecurringDefectCodes();
        return new RecurringDefectsResponse(defects, totalRecurring);
    }
}
