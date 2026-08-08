package com.ocp.eia.presentation.validation;

import com.ocp.eia.config.AppProperties;
import com.ocp.eia.config.SecurityConfig;
import com.ocp.eia.infrastructure.security.CustomUserDetailsService;
import com.ocp.eia.infrastructure.security.JwtAuthenticationFilter;
import com.ocp.eia.infrastructure.security.JwtService;
import com.ocp.eia.infrastructure.security.RestAccessDeniedHandler;
import com.ocp.eia.infrastructure.security.RestAuthenticationEntryPoint;
import com.ocp.eia.modules.iam.application.CreateUserUseCase;
import com.ocp.eia.modules.iam.application.DeleteUserUseCase;
import com.ocp.eia.modules.iam.application.FindUserByIdUseCase;
import com.ocp.eia.modules.iam.application.ListUsersUseCase;
import com.ocp.eia.modules.iam.application.LoginUseCase;
import com.ocp.eia.modules.iam.application.RefreshTokenUseCase;
import com.ocp.eia.modules.iam.application.UpdateUserUseCase;
import com.ocp.eia.modules.maintenance.application.CreateFailureUseCase;
import com.ocp.eia.modules.maintenance.application.DeleteFailureUseCase;
import com.ocp.eia.modules.maintenance.application.FindFailureByIdUseCase;
import com.ocp.eia.modules.maintenance.application.ListFailuresUseCase;
import com.ocp.eia.modules.maintenance.application.UpdateFailureUseCase;
import com.ocp.eia.presentation.controller.AuthController;
import com.ocp.eia.presentation.controller.FailureController;
import com.ocp.eia.presentation.controller.UserController;
import com.ocp.eia.presentation.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        AuthController.class,
        FailureController.class,
        UserController.class
})
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        AppProperties.class,
        GlobalExceptionHandler.class
})
class ValidationWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService userDetailsService;

    @MockBean private LoginUseCase loginUseCase;
    @MockBean private RefreshTokenUseCase refreshTokenUseCase;

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

    @Test
    void login_blankEmail_returnsValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": " ",
                                  "password": "Password123!"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void login_invalidEmail_returnsValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email",
                                  "password": "Password123!"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.email").exists());
    }

    @Test
    void login_shortPassword_returnsValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "tech@ocp.ma",
                                  "password": "short"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.password").exists());
    }

    @Test
    @WithMockUser(roles = "TECHNICIEN")
    void failure_zoneServiceTooLong_returnsValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/failures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "equipmentId": "%s",
                                  "dateHeure": "2026-08-08T10:00:00Z",
                                  "criticite": "MOYENNE",
                                  "zoneService": "%s"
                                }
                                """.formatted(UUID.randomUUID(), "z".repeat(121))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.zoneService").exists());
    }

    @Test
    @WithMockUser(roles = "TECHNICIEN")
    void failure_descriptionInitialeTooLong_returnsValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/failures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "equipmentId": "%s",
                                  "dateHeure": "2026-08-08T10:00:00Z",
                                  "criticite": "MOYENNE",
                                  "descriptionInitiale": "%s"
                                }
                                """.formatted(UUID.randomUUID(), "d".repeat(4001))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.descriptionInitiale").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void user_shortPassword_returnsValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "new@ocp.ma",
                                  "password": "short",
                                  "role": "TECHNICIEN",
                                  "nomPrenom": "Nouveau",
                                  "actif": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.password").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void user_invalidEmail_returnsValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "bad-email",
                                  "password": "Password123!",
                                  "role": "TECHNICIEN",
                                  "nomPrenom": "Nouveau",
                                  "actif": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.email").exists());
    }
}
