package com.ocp.eia.modules.knowledge.evaluation;

import com.ocp.eia.config.AppProperties;
import com.ocp.eia.domain.model.*;
import com.ocp.eia.domain.repository.EquipmentRepository;
import com.ocp.eia.domain.repository.FailureRepository;
import com.ocp.eia.domain.repository.InterventionRepository;
import com.ocp.eia.domain.repository.UserRepository;
import com.ocp.eia.application.dto.InterventionDto.ValidationRequest;
import com.ocp.eia.modules.knowledge.domain.port.EmbeddingProviderPort;
import com.ocp.eia.modules.knowledge.domain.port.InterventionTextSearchPort;
import com.ocp.eia.modules.knowledge.domain.port.LlmProviderPort;
import com.ocp.eia.modules.knowledge.domain.port.VectorStorePort;
import com.ocp.eia.modules.maintenance.application.ValidateInterventionUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Benchmark d'intégration : pipeline réel (pgvector + FTS) avec embedding/LLM mockés.
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.ai.autoconfigure.ollama.OllamaAutoConfiguration",
        "app.knowledge.enabled=true",
        "spring.main.allow-bean-definition-overriding=true"
})
@Testcontainers
@Import(RagEvaluationIntegrationTest.SyncExecutorTestConfig.class)
class RagEvaluationIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres")
    );

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @TestConfiguration
    static class SyncExecutorTestConfig {
        @Bean(name = "taskExecutor")
        Executor taskExecutor() {
            return new SyncTaskExecutor();
        }
    }

    @MockBean private EmbeddingProviderPort embeddingProviderPort;
    @MockBean private LlmProviderPort llmProviderPort;

    @Autowired private AppProperties appProperties;
    @Autowired private VectorStorePort vectorStorePort;
    @Autowired private InterventionTextSearchPort textSearchPort;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private EquipmentRepository equipmentRepository;
    @Autowired private FailureRepository failureRepository;
    @Autowired private InterventionRepository interventionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ValidateInterventionUseCase validateInterventionUseCase;

    private RagEvaluationRunner runner;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM intervention_embeddings");
        jdbcTemplate.update("DELETE FROM intervention_documents");
        jdbcTemplate.update("DELETE FROM interventions");
        jdbcTemplate.update("DELETE FROM failures");
        jdbcTemplate.update("DELETE FROM equipment");
        jdbcTemplate.update("DELETE FROM users");
        SecurityContextHolder.clearContext();
        reset(embeddingProviderPort, llmProviderPort);

        runner = new RagEvaluationRunner(
                embeddingProviderPort, vectorStorePort, textSearchPort, llmProviderPort, appProperties);
    }

    @Test
    void run_e001Scenario_hitAt1WithRealPostgres() {
        User technicien = seedUser(Role.TECHNICIEN);
        User validateur = seedUser(Role.RESPONSABLE_EIA);

        Equipment equipment = equipmentRepository.save(Equipment.builder()
                .code("EQ-CONV-SIE")
                .designation("Convoyeur Siemens")
                .constructeur("Siemens")
                .build());
        Failure failure = failureRepository.save(Failure.builder()
                .equipment(equipment)
                .dateHeure(Instant.now())
                .criticite(Criticite.HAUTE)
                .statut(StatutPanne.EN_COURS)
                .codeDefaut("E001")
                .descriptionInitiale("Défaut E001 convoyeur")
                .build());
        Intervention intervention = interventionRepository.save(Intervention.builder()
                .failure(failure)
                .technicien(technicien)
                .statutValidation(StatutValidation.SOUMISE)
                .symptomes("Alarme E001 sur HMI")
                .causeRacine("Paramétrage incorrect")
                .actionsCorrectives("Reset paramètres usine")
                .build());

        float[] embedding = fakeEmbedding(42f);
        when(embeddingProviderPort.embed(anyString())).thenReturn(embedding);
        when(llmProviderPort.complete(anyString(), anyString())).thenReturn("OK");

        authenticateAs(validateur);
        validateInterventionUseCase.execute(intervention.getId(), new ValidationRequest(true, "OK"));

        RagEvaluationCase evalCase = new RagEvaluationCase(
                "E001-integration",
                "Le convoyeur Siemens affiche le code E001",
                intervention.getId(),
                "Benchmark E001"
        );

        RagEvaluationReport report = runner.run(List.of(evalCase));

        assertEquals(1, report.questionCount());
        assertEquals(100.0, report.precisionAt1Percent());
        assertTrue(report.caseResults().get(0).hitAt1());
        assertTrue(report.caseResults().get(0).timings().textSearchMs() >= 0);
        assertNotNull(report.toTextReport());
        assertTrue(report.toTextReport().contains("Top1 Accuracy"));
    }

    private User seedUser(Role role) {
        return userRepository.save(User.builder()
                .email(role.name().toLowerCase() + "-" + UUID.randomUUID() + "@eval.test")
                .passwordHash("hash")
                .role(role)
                .nomPrenom("Eval " + role.name())
                .actif(true)
                .build());
    }

    private void authenticateAs(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        user.getEmail(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
                )
        );
    }

    private static float[] fakeEmbedding(float seed) {
        float[] v = new float[768];
        v[0] = seed;
        return v;
    }
}
