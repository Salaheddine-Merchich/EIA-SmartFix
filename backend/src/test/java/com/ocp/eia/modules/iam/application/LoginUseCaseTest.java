package com.ocp.eia.modules.iam.application;

import com.ocp.eia.application.dto.AuthDto.AuthResponse;
import com.ocp.eia.application.dto.AuthDto.LoginRequest;
import com.ocp.eia.domain.model.Role;
import com.ocp.eia.domain.model.User;
import com.ocp.eia.infrastructure.security.CustomUserDetailsService;
import com.ocp.eia.infrastructure.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private CustomUserDetailsService userDetailsService;

    @InjectMocks private LoginUseCase useCase;

    @Test
    void execute_returnsAuthResponseWithTokens() {
        LoginRequest request = new LoginRequest("admin@ocp.ma", "Password123!");
        User user = User.builder()
                .email("admin@ocp.ma")
                .nomPrenom("Admin OCP")
                .role(Role.ADMIN)
                .build();
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("admin@ocp.ma")
                .password("hash")
                .roles("ADMIN")
                .build();

        when(userDetailsService.loadEntityByEmail("admin@ocp.ma")).thenReturn(user);
        when(userDetailsService.loadUserByUsername("admin@ocp.ma")).thenReturn(userDetails);
        when(jwtService.generateAccessToken(userDetails, "ADMIN", "Admin OCP")).thenReturn("access-token");
        when(jwtService.generateRefreshToken(userDetails)).thenReturn("refresh-token");

        AuthResponse response = useCase.execute(request);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService).generateAccessToken(userDetails, "ADMIN", "Admin OCP");
        verify(jwtService).generateRefreshToken(userDetails);
        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals("ADMIN", response.role());
        assertEquals("Admin OCP", response.nomPrenom());
        assertEquals("admin@ocp.ma", response.email());
    }

    @Test
    void execute_authenticatesWithEmailAndPassword() {
        LoginRequest request = new LoginRequest("tech@ocp.ma", "secret");
        User user = User.builder().email("tech@ocp.ma").nomPrenom("Tech").role(Role.TECHNICIEN).build();
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("tech@ocp.ma")
                .password("hash")
                .roles("TECHNICIEN")
                .build();

        when(userDetailsService.loadEntityByEmail("tech@ocp.ma")).thenReturn(user);
        when(userDetailsService.loadUserByUsername("tech@ocp.ma")).thenReturn(userDetails);
        when(jwtService.generateAccessToken(eq(userDetails), eq("TECHNICIEN"), eq("Tech"))).thenReturn("at");
        when(jwtService.generateRefreshToken(userDetails)).thenReturn("rt");

        useCase.execute(request);

        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("tech@ocp.ma", "secret")
        );
    }
}
