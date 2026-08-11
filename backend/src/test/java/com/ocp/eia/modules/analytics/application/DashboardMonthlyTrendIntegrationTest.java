package com.ocp.eia.modules.analytics.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.ai.autoconfigure.ollama.OllamaAutoConfiguration",
        "app.knowledge.enabled=false"
})
@Testcontainers
class DashboardMonthlyTrendIntegrationTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID EQUIPMENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres")
    );

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DashboardUseCase dashboardUseCase;

    @BeforeEach
    void resetFailures() {
        jdbcTemplate.update("DELETE FROM intervention_embeddings");
        jdbcTemplate.update("DELETE FROM intervention_documents");
        jdbcTemplate.update("DELETE FROM interventions");
        jdbcTemplate.update("DELETE FROM failures");
        jdbcTemplate.update("DELETE FROM equipment");
        jdbcTemplate.update("DELETE FROM users");

        jdbcTemplate.update("""
                INSERT INTO users (id, email, password_hash, nom_prenom, role, actif)
                VALUES (?, 'tech-dashboard@ocp.ma', 'hash', 'Tech Dashboard', 'TECHNICIEN', true)
                """, USER_ID);
        jdbcTemplate.update("""
                INSERT INTO equipment (id, code, designation, famille, zone, constructeur, mise_en_service)
                VALUES (?, 'EQ-TEST', 'Equipement test', 'Mecanique', 'Zone A', 'TestCo', CURRENT_DATE)
                """, EQUIPMENT_ID);
    }

    @Test
    void failuresByMonth_zeroFillsGapsAndSumsToTotalFailures() {
        insertFailure(UUID.randomUUID(), "2026-01-10 08:00:00");
        insertFailure(UUID.randomUUID(), "2026-03-10 08:00:00");
        insertFailure(UUID.randomUUID(), "2026-05-10 08:00:00");

        var response = dashboardUseCase.execute();

        assertEquals(3L, response.totalFailures());
        assertEquals(5, response.failuresByMonth().size());
        assertEquals("2026-01", response.failuresByMonth().getFirst().month());
        assertEquals("2026-05", response.failuresByMonth().getLast().month());
        assertTrue(response.failuresByMonth().stream()
                .anyMatch(item -> "2026-02".equals(item.month()) && item.count() == 0));
        assertTrue(response.failuresByMonth().stream()
                .anyMatch(item -> "2026-04".equals(item.month()) && item.count() == 0));
        assertEquals(3L, response.failuresByMonth().stream().mapToLong(item -> item.count()).sum());
    }

    private void insertFailure(UUID id, String timestamp) {
        jdbcTemplate.update("""
                INSERT INTO failures (id, equipment_id, date_heure, criticite, zone_service, responsable_id, statut, description_initiale)
                VALUES (?, ?, ?::timestamp, 'MOYENNE', 'Zone A', ?, 'RESOLUE', 'Panne test')
                """, id, EQUIPMENT_ID, timestamp, USER_ID);
    }
}
