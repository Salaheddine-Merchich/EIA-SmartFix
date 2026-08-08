package com.ocp.eia.presentation.security;

import com.ocp.eia.application.dto.EquipmentDto.EquipmentResponse;
import com.ocp.eia.application.dto.FailureDto.FailureResponse;
import com.ocp.eia.config.AppProperties;
import com.ocp.eia.config.SecurityConfig;
import com.ocp.eia.infrastructure.security.CustomUserDetailsService;
import com.ocp.eia.infrastructure.security.JwtAuthenticationFilter;
import com.ocp.eia.infrastructure.security.JwtService;
import com.ocp.eia.infrastructure.security.RestAccessDeniedHandler;
import com.ocp.eia.infrastructure.security.RestAuthenticationEntryPoint;
import com.ocp.eia.modules.asset.application.CreateEquipmentUseCase;
import com.ocp.eia.modules.asset.application.DeleteEquipmentUseCase;
import com.ocp.eia.modules.asset.application.FindEquipmentByIdUseCase;
import com.ocp.eia.modules.asset.application.GetEquipmentHistoryUseCase;
import com.ocp.eia.modules.asset.application.SearchEquipmentUseCase;
import com.ocp.eia.modules.asset.application.UpdateEquipmentUseCase;
import com.ocp.eia.modules.maintenance.application.CreateFailureUseCase;
import com.ocp.eia.modules.maintenance.application.DeleteFailureUseCase;
import com.ocp.eia.modules.maintenance.application.FindFailureByIdUseCase;
import com.ocp.eia.modules.maintenance.application.ListFailuresUseCase;
import com.ocp.eia.modules.maintenance.application.UpdateFailureUseCase;
import com.ocp.eia.presentation.controller.EquipmentController;
import com.ocp.eia.presentation.controller.FailureController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

@WebMvcTest(controllers = {EquipmentController.class, FailureController.class})
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        AppProperties.class
})
class SecurityRbacWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService userDetailsService;

    @MockBean private SearchEquipmentUseCase searchEquipmentUseCase;
    @MockBean private FindEquipmentByIdUseCase findEquipmentByIdUseCase;
    @MockBean private GetEquipmentHistoryUseCase getEquipmentHistoryUseCase;
    @MockBean private CreateEquipmentUseCase createEquipmentUseCase;
    @MockBean private UpdateEquipmentUseCase updateEquipmentUseCase;
    @MockBean private DeleteEquipmentUseCase deleteEquipmentUseCase;

    @MockBean private ListFailuresUseCase listFailuresUseCase;
    @MockBean private FindFailureByIdUseCase findFailureByIdUseCase;
    @MockBean private CreateFailureUseCase createFailureUseCase;
    @MockBean private UpdateFailureUseCase updateFailureUseCase;
    @MockBean private DeleteFailureUseCase deleteFailureUseCase;

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
}
