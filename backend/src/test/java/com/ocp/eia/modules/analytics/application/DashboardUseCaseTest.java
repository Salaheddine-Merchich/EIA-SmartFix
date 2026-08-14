package com.ocp.eia.modules.analytics.application;

import com.ocp.eia.application.dto.DashboardDto.MonthlyTrendItem;
import com.ocp.eia.domain.model.StatutPanne;
import com.ocp.eia.domain.model.StatutValidation;
import com.ocp.eia.domain.repository.EquipmentSchemaRepository;
import com.ocp.eia.domain.repository.FailureRepository;
import com.ocp.eia.domain.repository.InterventionRepository;
import com.ocp.eia.modules.knowledge.application.AiDiagnosticStatsService;
import com.ocp.eia.modules.knowledge.domain.repository.KnowledgeDocumentRepository;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardUseCaseTest {

    @Mock private FailureRepository failureRepository;
    @Mock private InterventionRepository interventionRepository;
    @Mock private KnowledgeDocumentRepository knowledgeDocumentRepository;
    @Mock private EquipmentSchemaRepository equipmentSchemaRepository;
    @Mock private JdbcTemplate jdbcTemplate;

    private DashboardUseCase dashboardUseCase;

    @BeforeEach
    void setUp() {
        ObjectProvider<AiDiagnosticStatsService> aiDiagnosticStatsService = mock(ObjectProvider.class);
        lenient().when(aiDiagnosticStatsService.getIfAvailable()).thenReturn(null);
        dashboardUseCase = new DashboardUseCase(
                failureRepository, interventionRepository, knowledgeDocumentRepository,
                equipmentSchemaRepository, jdbcTemplate, aiDiagnosticStatsService);
    }

    @Test
    void execute_returnsCountsFromRepositories() {
        when(failureRepository.count()).thenReturn(8L);
        when(failureRepository.countByStatut(StatutPanne.OUVERTE)).thenReturn(2L);
        when(failureRepository.countByStatut(StatutPanne.EN_COURS)).thenReturn(1L);
        when(interventionRepository.countByStatutValidation(StatutValidation.VALIDEE)).thenReturn(5L);
        when(interventionRepository.countByStatutValidation(StatutValidation.SOUMISE)).thenReturn(1L);
        when(interventionRepository.countByStatutValidation(StatutValidation.BROUILLON)).thenReturn(2L);
        when(interventionRepository.countByStatutValidation(StatutValidation.REJETEE)).thenReturn(1L);
        when(knowledgeDocumentRepository.count()).thenReturn(13L);
        when(knowledgeDocumentRepository.countByActiveTrue()).thenReturn(11L);
        when(equipmentSchemaRepository.countByActiveTrue()).thenReturn(20L);
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM intervention_embeddings", Long.class)).thenReturn(26L);
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
                new MonthlyTrendItem("2026-03", 8L),
                new MonthlyTrendItem("2026-04", 0L),
                new MonthlyTrendItem("2026-05", 0L),
                new MonthlyTrendItem("2026-06", 0L),
                new MonthlyTrendItem("2026-07", 14L),
                new MonthlyTrendItem("2026-08", 4L)
        ));

        var response = dashboardUseCase.execute();

        assertEquals(8L, response.totalFailures());
        assertEquals(3L, response.openFailures());
        assertEquals(5L, response.validatedInterventions());
        assertEquals(1L, response.pendingValidations());
        assertEquals(2L, response.draftInterventions());
        assertEquals(1L, response.rejectedInterventions());
        assertEquals(13L, response.knowledgeDocuments());
        assertEquals(11L, response.activeKnowledgeDocuments());
        assertEquals(20L, response.activeEquipmentSchemas());
        assertEquals(26L, response.indexedInterventions());
        assertEquals(90.0, response.mttrMinutes());
        assertEquals(120.0, response.mtbfHours());
        assertEquals(6, response.failuresByMonth().size());
        assertEquals(26L, response.failuresByMonth().stream().mapToLong(MonthlyTrendItem::count).sum());
    }

    @Test
    void fetchFailuresByMonth_returnsEmptyWhenNoFailures() {
        when(failureRepository.count()).thenReturn(0L);

        List<MonthlyTrendItem> result = dashboardUseCase.fetchFailuresByMonth();

        assertTrue(result.isEmpty());
    }

    @Test
    void fetchFailuresByMonth_zeroFillIncludesIntermediateMonths() {
        when(failureRepository.count()).thenReturn(3L);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of(
                new MonthlyTrendItem("2026-01", 1L),
                new MonthlyTrendItem("2026-02", 0L),
                new MonthlyTrendItem("2026-03", 1L),
                new MonthlyTrendItem("2026-04", 0L),
                new MonthlyTrendItem("2026-05", 1L)
        ));

        List<MonthlyTrendItem> result = dashboardUseCase.fetchFailuresByMonth();

        assertEquals(5, result.size());
        assertEquals(0L, result.get(1).count());
        assertEquals(3L, result.stream().mapToLong(MonthlyTrendItem::count).sum());
    }
}
