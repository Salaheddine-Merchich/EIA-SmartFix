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
        "app.knowledge.provider=mock",
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
        // Fast path possible when similarité élevée (même embedding) — LLM optionnel
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

        doReturn(List.of()).when(vectorStorePort).findSimilar(any(float[].class), anyInt(), any());

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
                "Le score exact ou ILIKE doit passer le seuil");
        assertFalse(response.suggestions().probableCauses().isEmpty());
        verify(vectorStorePort, atLeastOnce()).findSimilar(any(float[].class), eq(5), any());
    }

    private Intervention seedValidatedInterventionWithCode(
            User technicien,
            User validateur,
            String codeDefaut,
            String constructeur,
            String symptomes,
            String causeRacine,
            String actions
    ) {
        Equipment equipment = equipmentRepository.save(Equipment.builder()
                .code("EQ-" + codeDefaut + "-" + UUID.randomUUID().toString().substring(0, 4))
                .designation("Equipement " + codeDefaut)
                .constructeur(constructeur)
                .famille("Variateurs")
                .build());
        Failure failure = failureRepository.save(Failure.builder()
                .equipment(equipment)
                .dateHeure(Instant.now())
                .criticite(Criticite.HAUTE)
                .statut(StatutPanne.RESOLUE)
                .codeDefaut(codeDefaut)
                .descriptionInitiale("Panne code " + codeDefaut)
                .build());
        Intervention intervention = interventionRepository.save(Intervention.builder()
                .failure(failure)
                .technicien(technicien)
                .statutValidation(StatutValidation.SOUMISE)
                .symptomes(symptomes)
                .causeRacine(causeRacine)
                .actionsCorrectives(actions)
                .description("Intervention " + codeDefaut)
                .build());

        when(embeddingProviderPort.embed(anyString())).thenReturn(fakeEmbedding(codeDefaut.hashCode()));
        authenticateAs(validateur);
        validateInterventionUseCase.execute(intervention.getId(), new ValidationRequest(true, "Validé " + codeDefaut));
        assertTrue(embeddingExists(intervention.getId()));
        return intervention;
    }

    // ---------------------------------------------------------------------
    // 8. Précision retrieval : codes PDF (E21, OUt1, 2310, F001 inconnu)
    // ---------------------------------------------------------------------

    @Test
    void ragAssist_e21InLongPhrase_findsExactIntervention() {
        User technicien = seedUser(Role.TECHNICIEN);
        User validateur = seedUser(Role.RESPONSABLE_EIA);
        Intervention e21 = seedValidatedInterventionWithCode(
                technicien, validateur, "E21", "Hitachi",
                "Disjonction thermique variateur",
                "Radiateur encrassé ou ventilateur HS",
                "Nettoyer radiateur et vérifier ventilateur"
        );

        when(llmProviderPort.complete(anyString(), anyString())).thenReturn("""
                {"probableCauses":["Radiateur encrassé"],"correctiveActions":["Nettoyer radiateur"],"summary":"E21 Hitachi","advice":"Surveiller température"}
                """);

        AiAssistResponse response = ragAssistUseCase.assist(
                new AiAssistRequest(null, null, "E21 surchauffe variateur Hitachi SJ200", 5));

        assertFalse(response.similarInterventions().isEmpty());
        assertEquals(e21.getId(), response.similarInterventions().get(0).interventionId());
    }

    @Test
    void ragAssist_e21WithCompetingHitachiCodes_returnsOnlyE21Content() {
        User technicien = seedUser(Role.TECHNICIEN);
        User validateur = seedUser(Role.RESPONSABLE_EIA);
        float[] sharedEmbedding = fakeEmbedding(42f);
        when(embeddingProviderPort.embed(anyString())).thenReturn(sharedEmbedding);

        Intervention e21 = seedValidatedInterventionWithCode(
                technicien, validateur, "E21", "Hitachi",
                "Disjonction thermique variateur",
                "Radiateur encrassé ou ventilateur HS",
                "Nettoyer radiateur et vérifier ventilateur"
        );
        seedValidatedInterventionWithCode(
                technicien, validateur, "E35", "Hitachi",
                "Code E35 surchauffe moteur",
                "Sonde thermique moteur entrees 6/L",
                "Verifier sonde [6]/[L]; reduire charge"
        );
        seedValidatedInterventionWithCode(
                technicien, validateur, "E01", "Hitachi",
                "Code E01 sur afficheur SJ200",
                "Court-circuit sortie, moteur grippé ou surcharge",
                "Verifier CC sortie et charge; STOP/RESET puis corriger cause"
        );

        AiAssistResponse response = ragAssistUseCase.assist(
                new AiAssistRequest(null, null, "E21 surchauffe variateur Hitachi", 5));

        assertEquals(1, response.similarInterventions().size(),
                "Seule l'intervention E21 doit être retenue");
        assertEquals(e21.getId(), response.similarInterventions().get(0).interventionId());

        String causes = String.join(" ", response.suggestions().probableCauses());
        assertTrue(causes.toLowerCase().contains("radiateur") || causes.toLowerCase().contains("ventilateur"),
                "Les causes doivent correspondre à E21, pas à E35/E01");
        assertFalse(causes.contains("6/L"), "Ne doit pas mélanger le contenu E35");
    }

    @Test
    void ragAssist_out1Goodrive_findsExactIntervention() {
        User technicien = seedUser(Role.TECHNICIEN);
        User validateur = seedUser(Role.RESPONSABLE_EIA);
        Intervention out1 = seedValidatedInterventionWithCode(
                technicien, validateur, "OUt1", "Goodrive",
                "Code OUt1 affiché",
                "Acceleration trop rapide ou IGBT endommagé",
                "Augmenter ACC; vérifier cables CEM"
        );

        when(llmProviderPort.complete(anyString(), anyString())).thenReturn("""
                {"probableCauses":["IGBT phase U"],"correctiveActions":["Augmenter ACC"],"summary":"OUt1","advice":"CEM"}
                """);

        AiAssistResponse response = ragAssistUseCase.assist(
                new AiAssistRequest(null, null, "OUt1 protection phase U Goodrive", 5));

        assertEquals(out1.getId(), response.similarInterventions().get(0).interventionId());
    }

    @Test
    void ragAssist_2310AbbFilature_findsExactIntervention() {
        User technicien = seedUser(Role.TECHNICIEN);
        User validateur = seedUser(Role.RESPONSABLE_EIA);
        Intervention i2310 = seedValidatedInterventionWithCode(
                technicien, validateur, "2310", "ABB",
                "Surintensité sortie filature",
                "Charge mécanique excessive",
                "Vérifier charge et accélération"
        );

        when(llmProviderPort.complete(anyString(), anyString())).thenReturn("""
                {"probableCauses":["Charge excessive"],"correctiveActions":["Vérifier charge"],"summary":"2310","advice":"Filature"}
                """);

        AiAssistResponse response = ragAssistUseCase.assist(
                new AiAssistRequest(null, null, "2310 surintensité ABB filature", 5));

        assertEquals(i2310.getId(), response.similarInterventions().get(0).interventionId());
    }

    @Test
    void ragAssist_pompePvNoStart_prefersVeilleOverConvoyage() {
        User technicien = seedUser(Role.TECHNICIEN);
        User validateur = seedUser(Role.RESPONSABLE_EIA);

        Intervention veille = seedValidatedInterventionWithZone(
                technicien, validateur, "VEI-VEILLE", "VEICHI", "Variateur", "Station PV",
                "Variateur reste en veille 0 Hz",
                "Commande X1 ou cablage commutateur",
                "Verifier commande X1; test a vide; reset F00.19"
        );
        Intervention sommeil = seedValidatedInterventionWithZone(
                technicien, validateur, "A.LPn", "Goodrive", "Variateur", "Station PV",
                "Alarme A.LPn mode sommeil",
                "Tension PV inferieure F14.11",
                "Attendre remontee > F14.12; ajuster F14.11/F14.13"
        );
        seedValidatedInterventionWithZone(
                technicien, validateur, "A-tF", "Goodrive", "Pompe", "Station PV",
                "Code A-tF affiche sur Goodrive 100-PV",
                "Reservoir plein",
                "Verifier cablage alarme plein"
        );

        when(llmProviderPort.complete(anyString(), anyString())).thenReturn("""
                {"probableCauses":["Commande X1"],"correctiveActions":["Verifier commande X1"],"summary":"Veille","advice":"PV"}
                """);

        AiAssistResponse response = ragAssistUseCase.assist(
                new AiAssistRequest(null, null, "Pompe PV ne démarre plus station solaire", 5));

        assertFalse(response.similarInterventions().isEmpty());
        assertTrue(response.similarInterventions().size() >= 2,
                "Doit remonter plusieurs causes possibles (veille, sommeil, etc.)");
        assertTrue(response.similarInterventions().stream()
                        .anyMatch(i -> i.interventionId().equals(veille.getId())),
                "Doit inclure l'intervention veille VEI-VEILLE");
        assertTrue(response.similarInterventions().stream()
                        .anyMatch(i -> i.interventionId().equals(sommeil.getId())),
                "Doit inclure l'intervention sommeil A.LPn");
        UUID firstId = response.similarInterventions().get(0).interventionId();
        assertTrue(firstId.equals(veille.getId()) || firstId.equals(sommeil.getId()),
                "La première intervention doit être veille ou sommeil, pas alarme plein");
        assertTrue(response.similarInterventions().stream()
                        .allMatch(i -> !i.symptomes().toLowerCase().contains("sens inverse")),
                "Ne doit pas remonter la rotation inverse convoyage");
    }

    private Intervention seedValidatedInterventionWithZone(
            User technicien,
            User validateur,
            String codeDefaut,
            String constructeur,
            String famille,
            String zone,
            String symptomes,
            String causeRacine,
            String actions
    ) {
        Equipment equipment = equipmentRepository.save(Equipment.builder()
                .code("EQ-" + codeDefaut + "-" + UUID.randomUUID().toString().substring(0, 4))
                .designation("Equipement " + codeDefaut)
                .constructeur(constructeur)
                .famille(famille)
                .zone(zone)
                .build());
        Failure failure = failureRepository.save(Failure.builder()
                .equipment(equipment)
                .dateHeure(Instant.now())
                .criticite(Criticite.HAUTE)
                .statut(StatutPanne.RESOLUE)
                .codeDefaut(codeDefaut)
                .descriptionInitiale("Panne " + codeDefaut)
                .build());
        Intervention intervention = interventionRepository.save(Intervention.builder()
                .failure(failure)
                .technicien(technicien)
                .statutValidation(StatutValidation.SOUMISE)
                .symptomes(symptomes)
                .causeRacine(causeRacine)
                .actionsCorrectives(actions)
                .description("Intervention " + codeDefaut)
                .build());

        when(embeddingProviderPort.embed(anyString())).thenReturn(fakeEmbedding(codeDefaut.hashCode()));
        authenticateAs(validateur);
        validateInterventionUseCase.execute(intervention.getId(), new ValidationRequest(true, "Validé " + codeDefaut));
        assertTrue(embeddingExists(intervention.getId()));
        return intervention;
    }

    @Test
    void ragAssist_unknownF001_returnsCodeNotFoundWithoutLlm() {
        when(embeddingProviderPort.embed(anyString())).thenReturn(fakeEmbedding(99f));

        AiAssistResponse response = ragAssistUseCase.assist(
                new AiAssistRequest(null, null, "F001 surchauffe convoyeur", 5));

        assertTrue(response.similarInterventions().isEmpty());
        assertTrue(response.suggestions().probableCauses().get(0).contains("F001"));
        verify(llmProviderPort, never()).complete(anyString(), anyString());
    }
}
