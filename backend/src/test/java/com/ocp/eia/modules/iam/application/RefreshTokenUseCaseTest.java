package com.ocp.eia.modules.iam.application;

import com.ocp.eia.application.dto.AuthDto.AuthResponse;
import com.ocp.eia.domain.model.Role;
import com.ocp.eia.domain.model.User;
import com.ocp.eia.infrastructure.security.CustomUserDetailsService;
import com.ocp.eia.infrastructure.security.JwtService;
import com.ocp.eia.shared.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenUseCaseTest {

    @Mock private JwtService jwtService;
    @Mock private CustomUserDetailsService userDetailsService;
    @Mock private RefreshTokenService refreshTokenService;

    @InjectMocks private RefreshTokenUseCase useCase;

    @Test
    void execute_returnsNewTokensAndRotates() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("admin@ocp.ma")
                .nomPrenom("Admin OCP")
                .role(Role.ADMIN)
                .build();
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("admin@ocp.ma")
                .password("hash")
                .roles("ADMIN")
                .build();

        when(jwtService.isRefreshToken("old-refresh-token")).thenReturn(true);
        when(jwtService.extractUsername("old-refresh-token")).thenReturn("admin@ocp.ma");
        when(userDetailsService.loadUserByUsername("admin@ocp.ma")).thenReturn(userDetails);
        when(jwtService.isTokenValid("old-refresh-token", userDetails)).thenReturn(true);
        when(userDetailsService.loadEntityByEmail("admin@ocp.ma")).thenReturn(user);
        when(jwtService.generateAccessToken(userDetails, "ADMIN", "Admin OCP")).thenReturn("new-access");
        when(jwtService.generateRefreshToken(userDetails)).thenReturn("new-refresh");

        AuthResponse response = useCase.execute("old-refresh-token");

        assertEquals("new-access", response.accessToken());
        assertEquals("new-refresh", response.refreshToken());
        verify(refreshTokenService).assertActive("old-refresh-token");
        verify(refreshTokenService).revoke("old-refresh-token");
        verify(refreshTokenService).persist("new-refresh", userId);
    }

    @Test
    void execute_invalidRefreshToken_throwsUnauthorized() {
        when(jwtService.isRefreshToken("not-a-refresh-token")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> useCase.execute("not-a-refresh-token"));
        verify(userDetailsService, never()).loadUserByUsername(any());
    }

    @Test
    void execute_expiredRefreshToken_throwsUnauthorized() {
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("admin@ocp.ma")
                .password("hash")
                .roles("ADMIN")
                .build();

        when(jwtService.isRefreshToken("expired-token")).thenReturn(true);
        when(jwtService.extractUsername("expired-token")).thenReturn("admin@ocp.ma");
        when(userDetailsService.loadUserByUsername("admin@ocp.ma")).thenReturn(userDetails);
        when(jwtService.isTokenValid("expired-token", userDetails)).thenReturn(false);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> useCase.execute("expired-token"));
        assertEquals("Session expirée", ex.getMessage());
        verify(jwtService, never()).generateAccessToken(any(), any(), any());
    }
}
