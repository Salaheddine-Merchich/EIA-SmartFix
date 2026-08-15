package com.ocp.eia.modules.analytics.application;

import com.ocp.eia.application.dto.DashboardDto.AiReliabilityStats;
import com.ocp.eia.application.dto.DashboardDto.MonthlyTrendItem;
import com.ocp.eia.domain.model.Criticite;
import com.ocp.eia.domain.model.StatutPanne;
import com.ocp.eia.domain.model.StatutValidation;
import com.ocp.eia.domain.model.User;
import com.ocp.eia.domain.repository.EquipmentRepository;
import com.ocp.eia.domain.repository.EquipmentSchemaRepository;
import com.ocp.eia.domain.repository.FailureRepository;
import com.ocp.eia.domain.repository.InterventionRepository;
import com.ocp.eia.infrastructure.security.SecurityUtils;
import com.ocp.eia.modules.knowledge.domain.repository.KnowledgeDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardUseCaseTest {

    @Mock private FailureRepository failureRepository;
    @Mock private EquipmentRepository equipmentRepository;
    @Mock private InterventionRepository interventionRepository;
    @Mock private KnowledgeDocumentRepository knowledgeDocumentRepository;
    @Mock private EquipmentSchemaRepository equipmentSchemaRepository;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private SecurityUtils securityUtils;

    private DashboardUseCase dashboardUseCase;

    @BeforeEach
    void setUp() {
        lenient().doThrow(new UsernameNotFoundException("Utilisateur non authentifié"))
                .when(securityUtils).getCurrentUser();
        dashboardUseCase = new DashboardUseCase(
                failureRepository, equipmentRepository, interventionRepository, knowledgeDocumentRepository,
                equipmentSchemaRepository, jdbcTemplate, securityUtils);
    }

    @Test
    void execute_returnsCountsFromRepositories() {
        stubCoreCounts();
        when(jdbcTemplate.queryForObject(eq(DashboardUseCase.INDEXED_INTERVENTIONS_SQL), eq(Long.class))).thenReturn(26L);
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
        assertEquals(2L, response.criticalOpenFailures());
        assertEquals(14L, response.equipmentCount());
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
        assertNull(response.aiReliability());
    }

    @Test
    void execute_returnsAiReliabilityFromConversationTraces() {
        stubCoreCounts();
        UUID userId = UUID.randomUUID();
        doReturn(User.builder().id(userId).email("admin@ocp.ma").build()).when(securityUtils).getCurrentUser();
        when(jdbcTemplate.queryForObject(eq(DashboardUseCase.INDEXED_INTERVENTIONS_SQL), eq(Long.class))).thenReturn(26L);
        when(jdbcTemplate.queryForObject(anyString(), eq(Double.class))).thenReturn(120.0);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of());
        when(jdbcTemplate.query(eq(DashboardUseCase.AI_RELIABILITY_SQL), any(ResultSetExtractor.class), eq(userId)))
                .thenReturn(new AiReliabilityStats(4L, 83.3, 4L));

        var response = dashboardUseCase.execute();

        assertEquals(4L, response.aiReliability().diagnosticsCount());
        assertEquals(83.3, response.aiReliability().averageConfidence());
        assertEquals(4L, response.aiReliability().totalRetrievals());
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

    private void stubCoreCounts() {
        when(failureRepository.count()).thenReturn(8L);
        when(failureRepository.countByStatut(StatutPanne.OUVERTE)).thenReturn(2L);
        when(failureRepository.countByStatut(StatutPanne.EN_COURS)).thenReturn(1L);
        when(failureRepository.countByCriticiteInAndStatutIn(
                List.of(Criticite.HAUTE, Criticite.CRITIQUE),
                List.of(StatutPanne.OUVERTE, StatutPanne.EN_COURS)
        )).thenReturn(2L);
        when(equipmentRepository.count()).thenReturn(14L);
        when(interventionRepository.countByStatutValidation(StatutValidation.VALIDEE)).thenReturn(5L);
        when(interventionRepository.countByStatutValidation(StatutValidation.SOUMISE)).thenReturn(1L);
        when(interventionRepository.countByStatutValidation(StatutValidation.BROUILLON)).thenReturn(2L);
        when(interventionRepository.countByStatutValidation(StatutValidation.REJETEE)).thenReturn(1L);
        when(knowledgeDocumentRepository.count()).thenReturn(13L);
        when(knowledgeDocumentRepository.countByActiveTrue()).thenReturn(11L);
        when(equipmentSchemaRepository.countByActiveTrue()).thenReturn(20L);
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
    }
}
