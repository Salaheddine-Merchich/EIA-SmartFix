package com.ocp.eia.modules.analytics.application;

import com.ocp.eia.application.dto.AnalyticsDto.RecurringDefectItem;
import com.ocp.eia.application.dto.AnalyticsDto.RecurringDefectsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecurringDefectsUseCase {

    private final JdbcTemplate jdbcTemplate;

    public RecurringDefectsResponse execute(int limit) {
        List<RecurringDefectItem> defects = jdbcTemplate.query("""
                SELECT code_defaut,
                       COUNT(*) AS occurrence_count,
                       COUNT(DISTINCT equipment_id) AS affected_equipment_count,
                       TO_CHAR(MAX(date_heure), 'YYYY-MM') AS last_seen_month
                FROM failures
                WHERE code_defaut IS NOT NULL AND TRIM(code_defaut) <> ''
                GROUP BY code_defaut
                HAVING COUNT(*) > 1
                ORDER BY COUNT(*) DESC
                LIMIT ?
                """, (rs, rowNum) -> new RecurringDefectItem(
                rs.getString("code_defaut"),
                rs.getLong("occurrence_count"),
                rs.getLong("affected_equipment_count"),
                rs.getString("last_seen_month")
        ), limit);

        Long totalRecurring = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM (
                    SELECT code_defaut FROM failures
                    WHERE code_defaut IS NOT NULL AND TRIM(code_defaut) <> ''
                    GROUP BY code_defaut HAVING COUNT(*) > 1
                ) recurring
                """, Long.class);

        return new RecurringDefectsResponse(defects, totalRecurring == null ? 0L : totalRecurring);
    }
}
