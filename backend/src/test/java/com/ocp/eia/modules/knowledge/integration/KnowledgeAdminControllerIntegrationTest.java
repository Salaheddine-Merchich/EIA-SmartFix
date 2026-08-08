package com.ocp.eia.modules.knowledge.integration;

import com.ocp.eia.domain.model.*;
import com.ocp.eia.domain.repository.EquipmentRepository;
import com.ocp.eia.domain.repository.FailureRepository;
import com.ocp.eia.domain.repository.InterventionRepository;
import com.ocp.eia.domain.repository.UserRepository;
import com.ocp.eia.modules.knowledge.domain.port.EmbeddingProviderPort;
import com.ocp.eia.modules.knowledge.domain.port.LlmProviderPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.ai.autoconfigure.ollama.OllamaAutoConfiguration",
        "app.knowledge.enabled=true",
        "app.knowledge.provider=mock",
        "spring.main.allow-bean-definition-overriding=true"
})
@AutoConfigureMockMvc
@Testcontainers
@Import(KnowledgeAdminControllerIntegrationTest.SyncExecutorTestConfig.class)
class KnowledgeAdminControllerIntegrationTest {

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

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private EquipmentRepository equipmentRepository;
    @Autowired private FailureRepository failureRepository;
    @Autowired private InterventionRepository interventionRepository;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM intervention_embeddings");
        jdbcTemplate.update("DELETE FROM intervention_documents");
        jdbcTemplate.update("DELETE FROM interventions");
        jdbcTemplate.update("DELETE FROM failures");
        jdbcTemplate.update("DELETE FROM equipment");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void reindex_asAdmin_returnsSummaryAndIndexesValidatedInterventions() throws Exception {
        User technicien = userRepository.save(User.builder()
                .email("tech-" + UUID.randomUUID() + "@eia.test")
                .passwordHash("hash")
                .role(Role.TECHNICIEN)
                .nomPrenom("Technicien")
                .actif(true)
                .build());
        Equipment equipment = equipmentRepository.save(Equipment.builder()
                .code("ADM-EQ-" + UUID.randomUUID().toString().substring(0, 6))
                .designation("Pompe admin")
                .build());
        Failure failure = failureRepository.save(Failure.builder()
                .equipment(equipment)
                .dateHeure(Instant.now())
                .criticite(Criticite.MOYENNE)
                .statut(StatutPanne.OUVERTE)
                .build());
        interventionRepository.save(Intervention.builder()
                .failure(failure)
                .technicien(technicien)
                .statutValidation(StatutValidation.VALIDEE)
                .symptomes("Fuite joint")
                .causeRacine("Usure")
                .build());

        float[] embedding = new float[768];
        embedding[0] = 0.7f;
        when(embeddingProviderPort.embed(anyString())).thenReturn(embedding);

        mockMvc.perform(post("/api/v1/admin/knowledge/reindex"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(1))
                .andExpect(jsonPath("$.indexed").value(1))
                .andExpect(jsonPath("$.skipped").value(0))
                .andExpect(jsonPath("$.errors").value(0));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM intervention_embeddings", Integer.class);
        assertEquals(1, count);
    }

    @Test
    @WithMockUser(roles = "TECHNICIEN")
    void reindex_asTechnicien_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/admin/knowledge/reindex"))
                .andExpect(status().isForbidden());
    }
}
