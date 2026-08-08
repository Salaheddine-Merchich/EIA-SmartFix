package com.ocp.eia.modules.analytics.application;

import com.ocp.eia.application.dto.DashboardDto.*;
import com.ocp.eia.domain.model.StatutPanne;
import com.ocp.eia.domain.model.StatutValidation;
import com.ocp.eia.domain.repository.FailureRepository;
import com.ocp.eia.domain.repository.InterventionRepository;
import com.ocp.eia.modules.knowledge.application.AiDiagnosticStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardUseCase {

    private final FailureRepository failureRepository;
    private final InterventionRepository interventionRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectProvider<AiDiagnosticStatsService> aiDiagnosticStatsService;

    public DashboardResponse execute() {
        long totalFailures = failureRepository.count();
        long openFailures = failureRepository.countByStatut(StatutPanne.OUVERTE)
                + failureRepository.countByStatut(StatutPanne.EN_COURS);
        long validatedInterventions = interventionRepository.countByStatutValidation(StatutValidation.VALIDEE);
        long pendingValidations = interventionRepository.countByStatutValidation(StatutValidation.SOUMISE);

        Double mttr = interventionRepository.calculateMttr();
        Double mtbf = calculateMtbf();

        List<TopEquipmentItem> topEquipment = interventionRepository.findTopFailingEquipment(5).stream()
                .map(row -> new TopEquipmentItem(
                        UUID.fromString(row[0].toString()),
                        (String) row[1],
                        (String) row[2],
                        ((Number) row[3]).longValue()
                ))
                .toList();

        List<CauseItem> topCauses = interventionRepository.findTopCauses(5).stream()
                .map(row -> new CauseItem((String) row[0], ((Number) row[1]).longValue()))
                .toList();

        List<FamilleItem> byFamille = interventionRepository.countFailuresByFamille().stream()
                .map(row -> new FamilleItem((String) row[0], ((Number) row[1]).longValue()))
                .toList();

        List<MonthlyTrendItem> byMonth = jdbcTemplate.query("""
                SELECT TO_CHAR(date_trunc('month', date_heure), 'YYYY-MM') AS month,
                       COUNT(*) AS cnt
                FROM failures
                GROUP BY date_trunc('month', date_heure)
                ORDER BY date_trunc('month', date_heure)
                """, (rs, rowNum) -> new MonthlyTrendItem(
                rs.getString("month"),
                rs.getLong("cnt")
        ));

        AiDiagnosticStatsService statsService = aiDiagnosticStatsService.getIfAvailable();
        AiReliabilityStats aiReliability = statsService != null
                ? statsService.snapshot().orElse(null)
                : null;

        return new DashboardResponse(
                totalFailures,
                openFailures,
                validatedInterventions,
                pendingValidations,
                mttr,
                mtbf,
                topEquipment,
                topCauses,
                byFamille,
                byMonth,
                aiReliability
        );
    }

    private Double calculateMtbf() {
        return jdbcTemplate.queryForObject("""
                WITH ordered_failures AS (
                    SELECT equipment_id, date_heure,
                           LAG(date_heure) OVER (PARTITION BY equipment_id ORDER BY date_heure) AS prev_date
                    FROM failures
                ),
                intervals AS (
                    SELECT EXTRACT(EPOCH FROM (date_heure - prev_date)) / 3600.0 AS hours_between
                    FROM ordered_failures
                    WHERE prev_date IS NOT NULL
                )
                SELECT AVG(hours_between) FROM intervals
                """, Double.class);
    }
}
