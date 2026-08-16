package com.ocp.eia.presentation.security;

import com.ocp.eia.application.dto.EquipmentDto.EquipmentResponse;
import com.ocp.eia.application.dto.FailureDto.FailureResponse;
import com.ocp.eia.application.dto.KnowledgeDto.ReindexResponse;
import com.ocp.eia.application.dto.UserDto.UserResponse;
import com.ocp.eia.config.AppProperties;
import com.ocp.eia.config.SecurityConfig;
import com.ocp.eia.domain.model.Role;
import com.ocp.eia.infrastructure.security.AuthCookieService;
import com.ocp.eia.infrastructure.security.CustomUserDetailsService;
import com.ocp.eia.infrastructure.security.JwtAuthenticationFilter;
import com.ocp.eia.infrastructure.security.JwtService;
import com.ocp.eia.infrastructure.security.RestAccessDeniedHandler;
import com.ocp.eia.infrastructure.security.RestAuthenticationEntryPoint;
import com.ocp.eia.modules.asset.application.CreateEquipmentUseCase;
import com.ocp.eia.modules.asset.application.DeleteEquipmentUseCase;
import com.ocp.eia.modules.asset.application.DownloadEquipmentSchemaUseCase;
import com.ocp.eia.modules.asset.application.FindEquipmentByIdUseCase;
import com.ocp.eia.modules.asset.application.GetEquipmentHistoryUseCase;
import com.ocp.eia.modules.asset.application.ListEquipmentSchemasUseCase;
import com.ocp.eia.modules.asset.application.SearchEquipmentUseCase;
import com.ocp.eia.modules.asset.application.UpdateEquipmentUseCase;
import com.ocp.eia.modules.iam.application.CreateUserUseCase;
import com.ocp.eia.modules.iam.application.DeleteUserUseCase;
import com.ocp.eia.modules.iam.application.FindUserByIdUseCase;
import com.ocp.eia.modules.iam.application.ListUsersUseCase;
import com.ocp.eia.modules.iam.application.UpdateUserUseCase;
import com.ocp.eia.modules.knowledge.application.IndexKnowledgeDocumentUseCase;
import com.ocp.eia.modules.knowledge.application.RagAssistStreamUseCase;
import com.ocp.eia.modules.knowledge.application.RagAssistUseCase;
import com.ocp.eia.modules.knowledge.application.ReindexKnowledgeUseCase;
import com.ocp.eia.modules.maintenance.application.CreateFailureUseCase;
import com.ocp.eia.modules.maintenance.application.DeleteFailureUseCase;
import com.ocp.eia.modules.maintenance.application.FindFailureByIdUseCase;
import com.ocp.eia.modules.maintenance.application.ListFailuresUseCase;
import com.ocp.eia.modules.maintenance.application.UpdateFailureUseCase;
import com.ocp.eia.presentation.controller.AiController;
import com.ocp.eia.presentation.controller.EquipmentController;
import com.ocp.eia.presentation.controller.FailureController;
import com.ocp.eia.presentation.controller.KnowledgeAdminController;
import com.ocp.eia.presentation.controller.UserController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RBAC slice tests. Knowledge/AI controllers use {@code @ConditionalOnBean} and are therefore
 * registered via {@link ConditionalControllers} so PreAuthorize rules can be exercised in WebMvcTest.
 */
@WebMvcTest(controllers = {
        EquipmentController.class,
        FailureController.class,
        UserController.class
})
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        AppProperties.class,
        SecurityRbacWebMvcTest.ConditionalControllers.class
})
class SecurityRbacWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService userDetailsService;
    @MockBean private AuthCookieService authCookieService;

    @MockBean private SearchEquipmentUseCase searchEquipmentUseCase;
    @MockBean private FindEquipmentByIdUseCase findEquipmentByIdUseCase;
    @MockBean private GetEquipmentHistoryUseCase getEquipmentHistoryUseCase;
    @MockBean private CreateEquipmentUseCase createEquipmentUseCase;
    @MockBean private UpdateEquipmentUseCase updateEquipmentUseCase;
    @MockBean private DeleteEquipmentUseCase deleteEquipmentUseCase;
    @MockBean private ListEquipmentSchemasUseCase listEquipmentSchemasUseCase;
    @MockBean private DownloadEquipmentSchemaUseCase downloadEquipmentSchemaUseCase;

    @MockBean private ListFailuresUseCase listFailuresUseCase;
    @MockBean private FindFailureByIdUseCase findFailureByIdUseCase;
    @MockBean private CreateFailureUseCase createFailureUseCase;
    @MockBean private UpdateFailureUseCase updateFailureUseCase;
    @MockBean private DeleteFailureUseCase deleteFailureUseCase;

    @MockBean private ListUsersUseCase listUsersUseCase;
    @MockBean private FindUserByIdUseCase findUserByIdUseCase;
    @MockBean private CreateUserUseCase createUserUseCase;
    @MockBean private UpdateUserUseCase updateUserUseCase;
    @MockBean private DeleteUserUseCase deleteUserUseCase;

    @MockBean private ReindexKnowledgeUseCase reindexKnowledgeUseCase;
    @MockBean private IndexKnowledgeDocumentUseCase indexKnowledgeDocumentUseCase;
    @MockBean private RagAssistUseCase ragAssistUseCase;
    @MockBean private RagAssistStreamUseCase ragAssistStreamUseCase;

    @TestConfiguration
    static class ConditionalControllers {
        @Bean
        KnowledgeAdminController knowledgeAdminController(
                ReindexKnowledgeUseCase reindexKnowledgeUseCase,
                IndexKnowledgeDocumentUseCase indexKnowledgeDocumentUseCase) {
            return new KnowledgeAdminController(reindexKnowledgeUseCase, indexKnowledgeDocumentUseCase);
        }

        @Bean
        AiController aiController(
                RagAssistUseCase ragAssistUseCase,
                RagAssistStreamUseCase ragAssistStreamUseCase) {
            return new AiController(ragAssistUseCase, ragAssistStreamUseCase);
        }
    }

    @Test
    void protectedEndpoint_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/equipment"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @WithMockUser(roles = "TECHNICIEN")
    void adminOnlyEndpoint_asTechnicien_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "EQ-001",
                                  "designation": "Pompe"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminOnlyEndpoint_asAdmin_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(createEquipmentUseCase.execute(any())).thenReturn(new EquipmentResponse(
                id, "EQ-001", "Pompe", null, null, null, null, 0
        ));

        mockMvc.perform(post("/api/v1/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "EQ-001",
                                  "designation": "Pompe"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("EQ-001"));
    }

    @Test
    @WithMockUser(roles = "TECHNICIEN")
    void technicienCanCreateFailure() throws Exception {
        UUID equipmentId = UUID.randomUUID();
        when(createFailureUseCase.execute(any())).thenReturn(new FailureResponse(
                UUID.randomUUID(),
                equipmentId,
                "EQ-001",
                "Pompe",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                null
        ));

        mockMvc.perform(post("/api/v1/failures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "equipmentId": "%s",
                                  "dateHeure": "2026-08-08T10:00:00Z",
                                  "criticite": "MOYENNE"
                                }
                                """.formatted(equipmentId)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "TECHNICIEN")
    void createUser_asTechnicien_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "tech@example.com",
                                  "password": "password123",
                                  "role": "TECHNICIEN",
                                  "nomPrenom": "Tech User",
                                  "actif": true
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_asAdmin_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(createUserUseCase.execute(any())).thenReturn(new UserResponse(
                id, "admin.created@example.com", Role.TECHNICIEN, "Tech User", true
        ));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin.created@example.com",
                                  "password": "password123",
                                  "role": "TECHNICIEN",
                                  "nomPrenom": "Tech User",
                                  "actif": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("admin.created@example.com"));
    }

    @Test
    @WithMockUser(roles = "TECHNICIEN")
    void knowledgeReindex_asTechnicien_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/admin/knowledge/reindex"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void knowledgeReindex_asAdmin_returns200() throws Exception {
        when(reindexKnowledgeUseCase.execute()).thenReturn(new ReindexResponse(10, 8, 1, 1));

        mockMvc.perform(post("/api/v1/admin/knowledge/reindex"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(10))
                .andExpect(jsonPath("$.indexed").value(8));
    }

    @Test
    void aiAssist_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/ai/assist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Vibrations anormales sur pompe"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
