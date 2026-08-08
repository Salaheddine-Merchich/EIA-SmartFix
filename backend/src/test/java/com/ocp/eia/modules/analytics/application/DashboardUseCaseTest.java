package com.ocp.eia.modules.analytics.application;

import com.ocp.eia.application.dto.DashboardDto.MonthlyTrendItem;
import com.ocp.eia.domain.model.StatutPanne;
import com.ocp.eia.domain.model.StatutValidation;
import com.ocp.eia.domain.repository.FailureRepository;
import com.ocp.eia.domain.repository.InterventionRepository;
import com.ocp.eia.modules.knowledge.application.AiDiagnosticStatsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardUseCaseTest {

    @Mock private FailureRepository failureRepository;
    @Mock private InterventionRepository interventionRepository;
    @Mock private JdbcTemplate jdbcTemplate;

    private DashboardUseCase dashboardUseCase;

    @BeforeEach
    void setUp() {
        ObjectProvider<AiDiagnosticStatsService> aiDiagnosticStatsService = mock(ObjectProvider.class);
        when(aiDiagnosticStatsService.getIfAvailable()).thenReturn(null);
        dashboardUseCase = new DashboardUseCase(
                failureRepository, interventionRepository, jdbcTemplate, aiDiagnosticStatsService);
    }

    @Test
    void execute_returnsCountsFromRepositories() {
        when(failureRepository.count()).thenReturn(8L);
        when(failureRepository.countByStatut(StatutPanne.OUVERTE)).thenReturn(2L);
        when(failureRepository.countByStatut(StatutPanne.EN_COURS)).thenReturn(1L);
        when(interventionRepository.countByStatutValidation(StatutValidation.VALIDEE)).thenReturn(5L);
        when(interventionRepository.countByStatutValidation(StatutValidation.SOUMISE)).thenReturn(1L);
        when(interventionRepository.calculateMttr()).thenReturn(90.0);
        when(interventionRepository.findTopFailingEquipment(5)).thenReturn(List.<Object[]>of(
                new Object[]{UUID.randomUUID(), "MOT-001", "Moteur", 3L}
        ));
        when(interventionRepository.findTopCauses(5)).thenReturn(List.<Object[]>of(
                new Object[]{"Usure roulement", 2L}
        ));
        when(interventionRepository.countFailuresByFamille()).thenReturn(List.<Object[]>of(
                new Object[]{"Automatisme", 4L}
        ));
        when(jdbcTemplate.queryForObject(anyString(), eq(Double.class))).thenReturn(120.0);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of(
                new MonthlyTrendItem("2025-11", 3L)
        ));

        var response = dashboardUseCase.execute();

        assertEquals(8L, response.totalFailures());
        assertEquals(3L, response.openFailures());
        assertEquals(5L, response.validatedInterventions());
        assertEquals(1L, response.pendingValidations());
        assertEquals(90.0, response.mttrMinutes());
        assertEquals(120.0, response.mtbfHours());
        assertEquals(1, response.failuresByMonth().size());
        assertEquals(3L, response.failuresByMonth().getFirst().count());
    }
}
