package com.ocp.eia.modules.knowledge.integration;

import com.ocp.eia.application.dto.AiDto.AiAssistRequest;
import com.ocp.eia.application.dto.AiDto.AiAssistResponse;
import com.ocp.eia.application.dto.InterventionDto.ValidationRequest;
import com.ocp.eia.domain.model.*;
import com.ocp.eia.domain.repository.EquipmentRepository;
import com.ocp.eia.domain.repository.FailureRepository;
import com.ocp.eia.domain.repository.InterventionRepository;
import com.ocp.eia.domain.repository.UserRepository;
import com.ocp.eia.modules.knowledge.application.RagAssistUseCase;
import com.ocp.eia.modules.knowledge.domain.port.EmbeddingProviderPort;
import com.ocp.eia.modules.knowledge.domain.port.LlmProviderPort;
import com.ocp.eia.modules.knowledge.domain.port.VectorStorePort;
import com.ocp.eia.modules.maintenance.application.DeleteInterventionUseCase;
import com.ocp.eia.modules.maintenance.application.ValidateInterventionUseCase;
import org.junit.jupiter.api.BeforeEach;
import com.ocp.eia.modules.maintenance.application.event.InterventionKnowledgePayload;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests d'intégration du pipeline RAG réel (PR #6) :
 * <pre>
 * ValidateInterventionUseCase -&gt; InterventionValidatedEvent -&gt; @TransactionalEventListener(AFTER_COMMIT)
 *   -&gt; IndexInterventionUseCase -&gt; EmbeddingProviderPort -&gt; PgVectorStoreAdapter (Postgres/pgvector)
 *   -&gt; RagAssistUseCase
 * </pre>
 *
 * Choix d'intégration (voir rapport PR #6 - section "Décision") :
 * <ul>
 *     <li>PostgreSQL + extension pgvector : réels, via Testcontainers, migrations Flyway complètes
 *     (mêmes scripts que la production).</li>
 *     <li>Ollama (EmbeddingProviderPort / LlmProviderPort) : mockés avec Mockito. Ce sont exactement
 *     les ports définis par l'architecture hexagonale pour isoler l'infrastructure IA ; un vrai
 *     conteneur Ollama nécessiterait de télécharger des modèles (lent, non déterministe, dépendant du
 *     réseau) pour un gain de couverture marginal, puisque la logique métier testée ici (indexation,
 *     recherche vectorielle, orchestration RAG, gestion d'erreur) ne dépend pas du contenu réel des
 *     réponses du modèle.</li>
 * </ul>
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.ai.autoconfigure.ollama.OllamaAutoConfiguration",
        "app.knowledge.enabled=true",
        "spring.main.allow-bean-definition-overriding=true"
})
@Testcontainers
@Import(KnowledgeRagIntegrationTest.SyncExecutorTestConfig.class)
class KnowledgeRagIntegrationTest {

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

    /**
     * Remplace l'executor "taskExecutor" (voir {@code AsyncConfig}) par une exécution synchrone
     * UNIQUEMENT dans ce contexte de test, afin de rendre déterministe la chaîne
     * {@code @TransactionalEventListener(AFTER_COMMIT)} + {@code @Async} sans introduire de
     * dépendance de polling (Awaitility) ni modifier le code de production.
     */
    @TestConfiguration
    static class SyncExecutorTestConfig {
        @Bean(name = "taskExecutor")
        Executor taskExecutor() {
            return new SyncTaskExecutor();
        }
    }

    @MockBean
    private EmbeddingProviderPort embeddingProviderPort;

    @MockBean
    private LlmProviderPort llmProviderPort;

    @SpyBean
    private VectorStorePort vectorStorePort;

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private EquipmentRepository equipmentRepository;
    @Autowired private FailureRepository failureRepository;
    @Autowired private InterventionRepository interventionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ValidateInterventionUseCase validateInterventionUseCase;
    @Autowired private DeleteInterventionUseCase deleteInterventionUseCase;
    @Autowired private RagAssistUseCase ragAssistUseCase;

    @BeforeEach
    void cleanDatabase() {
        // Repart d'un état propre avant chaque test : purge les données de démo (V4/V6) et celles
        // potentiellement créées par le CommandLineRunner de réindexation du profil "dev"
        // (KnowledgeDevReindexConfiguration), ainsi que les données du test précédent.
        // Ordre imposé par les contraintes FK (cf. V1-V5__*.sql).
        jdbcTemplate.update("DELETE FROM intervention_embeddings");
        jdbcTemplate.update("DELETE FROM intervention_documents");
        jdbcTemplate.update("DELETE FROM interventions");
        jdbcTemplate.update("DELETE FROM failures");
        jdbcTemplate.update("DELETE FROM equipment");
        jdbcTemplate.update("DELETE FROM users");
        SecurityContextHolder.clearContext();
        reset(embeddingProviderPort, llmProviderPort, vectorStorePort);
    }

    // ---------------------------------------------------------------------
    // Helpers de seed
    // ---------------------------------------------------------------------

    private User seedUser(Role role) {
        return userRepository.save(User.builder()
                .email(role.name().toLowerCase() + "-" + UUID.randomUUID() + "@eia.test")
                .passwordHash("hash")
                .role(role)
                .nomPrenom("Test " + role.name())
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

    private Intervention seedSoumiseIntervention(User technicien, String symptomes, String causeRacine, String actions) {
        Equipment equipment = equipmentRepository.save(Equipment.builder()
                .code("EQ-" + UUID.randomUUID().toString().substring(0, 8))
                .designation("Variateur de vitesse")
                .build());
        Failure failure = failureRepository.save(Failure.builder()
                .equipment(equipment)
                .dateHeure(Instant.now())
                .criticite(Criticite.HAUTE)
                .statut(StatutPanne.EN_COURS)
                .build());
        return interventionRepository.save(Intervention.builder()
                .failure(failure)
                .technicien(technicien)
                .statutValidation(StatutValidation.SOUMISE)
                .symptomes(symptomes)
                .causeRacine(causeRacine)
                .actionsCorrectives(actions)
                .description("Description test")
                .build());
    }

    private boolean embeddingExists(UUID interventionId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM intervention_embeddings WHERE intervention_id = ?",
                Integer.class, interventionId);
        return count != null && count > 0;
    }

    private static float[] fakeEmbedding(float seed) {
        float[] v = new float[768];
        v[0] = seed;
        return v;
    }

    private static String toVectorLiteral(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    // ---------------------------------------------------------------------
    // 1. Validation d'une intervention -> event -> AFTER_COMMIT -> embedding indexé dans pgvector
    // ---------------------------------------------------------------------

    @Test
    void validateIntervention_publishesEvent_indexesEmbeddingInPgVector() {
        User technicien = seedUser(Role.TECHNICIEN);
        User validateur = seedUser(Role.RESPONSABLE_EIA);
        Intervention intervention = seedSoumiseIntervention(technicien,
                "Surchauffe moteur", "Roulement défectueux", "Remplacement roulement");

        when(embeddingProviderPort.embed(anyString())).thenReturn(fakeEmbedding(1f));

        authenticateAs(validateur);
        validateInterventionUseCase.execute(intervention.getId(), new ValidationRequest(true, "Conforme"));

        assertTrue(embeddingExists(intervention.getId()),
                "L'embedding doit être présent dans pgvector après validation (event AFTER_COMMIT)");
        verify(embeddingProviderPort, times(1)).embed(contains("Surchauffe moteur"));
        verify(vectorStorePort, times(1)).upsert(eq(intervention.getId()), any(float[].class), anyString());

        String contenu = jdbcTemplate.queryForObject(
                "SELECT contenu_indexe FROM intervention_embeddings WHERE intervention_id = ?",
                String.class, intervention.getId());
        assertNotNull(contenu);
        assertTrue(contenu.contains("Roulement défectueux"));
    }

    // ---------------------------------------------------------------------
    // 2. Suppression d'intervention -> event -> remove() -> suppression embedding
    // ---------------------------------------------------------------------

    @Test
    void deleteIntervention_publishesRemovedEvent_removesEmbeddingViaListener() {
        User technicien = seedUser(Role.TECHNICIEN);
        Intervention intervention = seedSoumiseIntervention(technicien,
                "Vibration excessive", "Balourd", "Rééquilibrage");

        // Simule une intervention déjà indexée précédemment (sans dépendre du flux de validation).
        jdbcTemplate.update("""
                INSERT INTO intervention_embeddings (intervention_id, embedding, contenu_indexe, indexe_le)
                VALUES (?, ?::vector, 'contenu test', now())
                """, intervention.getId(), toVectorLiteral(fakeEmbedding(2f)));
        assertTrue(embeddingExists(intervention.getId()), "précondition : embedding déjà indexé");

        authenticateAs(technicien);
        deleteInterventionUseCase.execute(intervention.getId());

        assertFalse(embeddingExists(intervention.getId()),
                "L'embedding doit avoir disparu après suppression de l'intervention");
        assertTrue(interventionRepository.findById(intervention.getId()).isEmpty());
        // Preuve que le listener a bien appelé remove() (et pas seulement le cascade FK ON DELETE) :
        verify(vectorStorePort, times(1)).delete(intervention.getId());
    }

    // ---------------------------------------------------------------------
    // 3. Question RAG -> embedding question -> similarité vectorielle -> LLM -> réponse non vide
    // ---------------------------------------------------------------------

    @Test
    void ragAssist_withIndexedIntervention_returnsNonEmptySuggestions() {
        User technicien = seedUser(Role.TECHNICIEN);
        User validateur = seedUser(Role.RESPONSABLE_EIA);
        Intervention intervention = seedSoumiseIntervention(technicien,
                "Surchauffe variateur", "Ventilation obstruée", "Nettoyage filtre");

        float[] sharedEmbedding = fakeEmbedding(3f);
        when(embeddingProviderPort.embed(anyString())).thenReturn(sharedEmbedding);
        authenticateAs(validateur);
        validateInterventionUseCase.execute(intervention.getId(), new ValidationRequest(true, "OK"));
        assertTrue(embeddingExists(intervention.getId()), "précondition : intervention indexée");

        String llmJson = """
                {"probableCauses":["Ventilation obstruée"],"correctiveActions":["Nettoyer le filtre"],"summary":"Piste ventilation","advice":"Vérifier le flux d'air"}
                """;
        when(llmProviderPort.complete(anyString(), anyString())).thenReturn(llmJson);

        AiAssistRequest request = new AiAssistRequest(null, null, "Le variateur chauffe anormalement", 5);
        AiAssistResponse response = ragAssistUseCase.assist(request);

        assertFalse(response.similarInterventions().isEmpty(), "au moins une intervention similaire attendue");
        assertEquals(intervention.getId(), response.similarInterventions().get(0).interventionId());
        assertNotNull(response.suggestions());
        assertFalse(response.suggestions().probableCauses().isEmpty());
        assertEquals("Ventilation obstruée", response.suggestions().probableCauses().get(0));
        assertNotNull(response.disclaimer());
        assertFalse(response.disclaimer().isBlank());
        verify(llmProviderPort, times(1)).complete(anyString(), anyString());
    }

    // ---------------------------------------------------------------------
    // 4. Recherche sans résultat -> retour contrôlé (pas d'appel LLM inutile)
    // ---------------------------------------------------------------------

    @Test
    void ragAssist_noIndexedInterventions_returnsControlledFallback() {
        when(embeddingProviderPort.embed(anyString())).thenReturn(fakeEmbedding(9f));

        AiAssistRequest request = new AiAssistRequest(null, null, "Panne inconnue jamais rencontrée", 5);
        AiAssistResponse response = ragAssistUseCase.assist(request);

        assertTrue(response.similarInterventions().isEmpty());
        assertEquals("Aucune intervention similaire validée trouvée",
                response.suggestions().probableCauses().get(0));
        assertNotNull(response.disclaimer());
        verify(llmProviderPort, never()).complete(anyString(), anyString());
    }

    // ---------------------------------------------------------------------
    // 5. Ollama indisponible -> gestion d'erreur -> pas de crash applicatif
    // ---------------------------------------------------------------------

    @Test
    void validateIntervention_embeddingProviderDown_businessTransactionStillSucceeds() {
        User technicien = seedUser(Role.TECHNICIEN);
        User validateur = seedUser(Role.RESPONSABLE_EIA);
        Intervention intervention = seedSoumiseIntervention(technicien,
                "Fuite hydraulique", "Joint usé", "Remplacement joint");

        when(embeddingProviderPort.embed(anyString()))
                .thenThrow(new RuntimeException("Connection refused: Ollama indisponible"));

        authenticateAs(validateur);
        assertDoesNotThrow(() ->
                validateInterventionUseCase.execute(intervention.getId(), new ValidationRequest(true, "OK")),
                "La panne Ollama ne doit jamais faire échouer la transaction métier de validation");

        Intervention reloaded = interventionRepository.findById(intervention.getId()).orElseThrow();
        assertEquals(StatutValidation.VALIDEE, reloaded.getStatutValidation(),
                "La validation métier doit réussir même si l'IA est indisponible");
        assertFalse(embeddingExists(intervention.getId()),
                "Aucun embedding ne doit être créé si l'embedding provider échoue");
    }

    @Test
    void ragAssist_embeddingProviderDown_returnsControlledResponse() {
        when(embeddingProviderPort.embed(anyString()))
                .thenThrow(new RuntimeException("Connection refused: Ollama indisponible"));

        AiAssistRequest request = new AiAssistRequest(null, null, "Panne quelconque", 5);
        AiAssistResponse response = ragAssistUseCase.assist(request);

        assertTrue(response.similarInterventions().isEmpty());
        assertEquals("L'assistance IA est temporairement indisponible",
                response.suggestions().probableCauses().get(0));
        assertNotNull(response.disclaimer());
        verify(llmProviderPort, never()).complete(anyString(), anyString());
    }

    // ---------------------------------------------------------------------
    // 6. Réindexation globale (PR #9) : même contexte métier que l'indexation temps réel
    // ---------------------------------------------------------------------

    @Test
    void findByStatutValidationWithDetails_loadsFailureAndEquipment_withoutLazyInitializationException() {
        User technicien = seedUser(Role.TECHNICIEN);
        User validateur = seedUser(Role.RESPONSABLE_EIA);
        Equipment equipment = equipmentRepository.save(Equipment.builder()
                .code("REINDEX-" + UUID.randomUUID().toString().substring(0, 8))
                .designation("Convoyeur réindexation")
                .famille("Convoyage")
                .zone("Atelier test")
                .constructeur("ABB")
                .build());
        Failure failure = failureRepository.save(Failure.builder()
                .equipment(equipment)
                .dateHeure(Instant.now())
                .criticite(Criticite.CRITIQUE)
                .zoneService("Ligne test")
                .statut(StatutPanne.OUVERTE)
                .descriptionInitiale("Arrêt convoyeur")
                .codeDefaut("REINDEX-01")
                .build());
        Intervention intervention = interventionRepository.save(Intervention.builder()
                .failure(failure)
                .technicien(technicien)
                .statutValidation(StatutValidation.SOUMISE)
                .symptomes("Courroie cassée")
                .causeRacine("Usure")
                .commentaireValidation("Conforme réindex")
                .dureeArretMinutes(45)
                .build());

        authenticateAs(validateur);
        validateInterventionUseCase.execute(intervention.getId(), new ValidationRequest(true, "Validé"));

        Intervention fromReindexQuery = interventionRepository
                .findByStatutValidationWithDetails(StatutValidation.VALIDEE)
                .stream()
                .filter(i -> i.getId().equals(intervention.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Intervention validée introuvable via requête réindexation"));

        assertDoesNotThrow(() -> {
            assertEquals("REINDEX-01", fromReindexQuery.getFailure().getCodeDefaut());
            assertEquals(equipment.getCode(), fromReindexQuery.getFailure().getEquipment().getCode());
        }, "failure et equipment doivent être initialisés (JOIN FETCH), pas de LazyInitializationException");

        InterventionKnowledgePayload reindexPayload =
                InterventionKnowledgePayload.fromIntervention(fromReindexQuery);
        assertEquals("REINDEX-01", reindexPayload.failureCodeDefaut());
        assertEquals(equipment.getCode(), reindexPayload.equipmentCode());
        assertTrue(reindexPayload.toIndexedContent().contains("Famille: Convoyage"));
        assertTrue(reindexPayload.toIndexedContent().contains("Criticité: CRITIQUE"));
    }

    @Test
    void reindexQuery_producesSameIndexedContent_asRealtimeValidationPath() {
        User technicien = seedUser(Role.TECHNICIEN);
        User validateur = seedUser(Role.RESPONSABLE_EIA);
        Intervention intervention = seedSoumiseIntervention(technicien,
                "Surchauffe", "Ventilation", "Nettoyage");

        authenticateAs(validateur);
        validateInterventionUseCase.execute(intervention.getId(), new ValidationRequest(true, "OK"));

        String realtimeContent = InterventionKnowledgePayload.fromIntervention(
                interventionRepository.findByIdWithDetails(intervention.getId()).orElseThrow()
        ).toIndexedContent();

        String reindexContent = InterventionKnowledgePayload.fromIntervention(
                interventionRepository.findByStatutValidationWithDetails(StatutValidation.VALIDEE)
                        .stream()
                        .filter(i -> i.getId().equals(intervention.getId()))
                        .findFirst()
                        .orElseThrow()
        ).toIndexedContent();

        assertEquals(realtimeContent, reindexContent,
                "réindexation globale et indexation temps réel doivent produire le même contenu indexé");
    }

    // ---------------------------------------------------------------------
    // 7. Hybrid search (PR #13) : code défaut E001 via FTS quand vectoriel vide
    // ---------------------------------------------------------------------

    @Test
    void ragAssist_faultCodeE001_textSearchFindsWhenVectorReturnsNothing() {
        User technicien = seedUser(Role.TECHNICIEN);
        User validateur = seedUser(Role.RESPONSABLE_EIA);
        Equipment equipment = equipmentRepository.save(Equipment.builder()
                .code("EQ-E001-TEST")
                .designation("Variateur E001")
                .constructeur("Siemens")
                .build());
        Failure failure = failureRepository.save(Failure.builder()
                .equipment(equipment)
                .dateHeure(Instant.now())
                .criticite(Criticite.HAUTE)
                .statut(StatutPanne.EN_COURS)
                .codeDefaut("E001")
                .descriptionInitiale("Défaut variateur E001")
                .build());
        Intervention intervention = interventionRepository.save(Intervention.builder()
                .failure(failure)
                .technicien(technicien)
                .statutValidation(StatutValidation.SOUMISE)
                .symptomes("Alarme E001 sur HMI")
                .causeRacine("Paramétrage incorrect")
                .actionsCorrectives("Réinitialiser paramètres usine")
                .description("Intervention code E001")
                .build());

        when(embeddingProviderPort.embed(anyString())).thenReturn(fakeEmbedding(7f));
        authenticateAs(validateur);
        validateInterventionUseCase.execute(intervention.getId(), new ValidationRequest(true, "Validé E001"));
        assertTrue(embeddingExists(intervention.getId()), "précondition : intervention indexée");

        doReturn(List.of()).when(vectorStorePort).findSimilar(any(float[].class), anyInt());

        String llmJson = """
                {"probableCauses":["Paramétrage incorrect"],"correctiveActions":["Réinitialiser paramètres usine"],"summary":"Piste E001","advice":"Vérifier paramètres"}
                """;
        when(llmProviderPort.complete(anyString(), anyString())).thenReturn(llmJson);

        AiAssistRequest request = new AiAssistRequest(null, null, "E001", 5);
        AiAssistResponse response = ragAssistUseCase.assist(request);

        assertFalse(response.similarInterventions().isEmpty(),
                "La recherche texte doit retrouver l'intervention via code_defaut E001");
        assertEquals(intervention.getId(), response.similarInterventions().get(0).interventionId());
        assertTrue(response.similarInterventions().get(0).similarity() >= 0.70,
                "Le score ILIKE (0.75) doit passer le seuil");
        assertEquals("Paramétrage incorrect", response.suggestions().probableCauses().get(0));
        verify(vectorStorePort, atLeastOnce()).findSimilar(any(float[].class), eq(5));
        verify(llmProviderPort, times(1)).complete(anyString(), contains("Paramétrage incorrect"));
    }
}
