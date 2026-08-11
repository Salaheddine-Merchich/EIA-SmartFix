package com.ocp.eia.presentation.controller;

import com.ocp.eia.application.dto.AuthDto.AuthResponse;
import com.ocp.eia.application.dto.AuthDto.LoginRequest;
import com.ocp.eia.application.dto.AuthDto.RefreshTokenRequest;
import com.ocp.eia.infrastructure.security.AuthCookieService;
import com.ocp.eia.modules.iam.application.LoginUseCase;
import com.ocp.eia.modules.iam.application.LogoutUseCase;
import com.ocp.eia.modules.iam.application.RefreshTokenUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification")
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final AuthCookieService authCookieService;

    @PostMapping("/login")
    @Operation(summary = "Connexion utilisateur")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        AuthResponse auth = loginUseCase.execute(request);
        authCookieService.writeAuthCookies(response, auth.accessToken(), auth.refreshToken());
        return ResponseEntity.ok(auth);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rafraîchir le token JWT")
    public ResponseEntity<AuthResponse> refresh(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        String refreshToken = request != null ? request.refreshToken() : null;
        if (refreshToken == null || refreshToken.isBlank()) {
            refreshToken = authCookieService.readCookie(httpRequest, AuthCookieService.REFRESH_COOKIE);
        }
        AuthResponse auth = refreshTokenUseCase.execute(refreshToken);
        authCookieService.writeAuthCookies(response, auth.accessToken(), auth.refreshToken());
        return ResponseEntity.ok(auth);
    }

    @PostMapping("/logout")
    @Operation(summary = "Déconnexion (révocation refresh + cookies)")
    public ResponseEntity<Void> logout(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        String refreshToken = request != null ? request.refreshToken() : null;
        if (refreshToken == null || refreshToken.isBlank()) {
            refreshToken = authCookieService.readCookie(httpRequest, AuthCookieService.REFRESH_COOKIE);
        }
        logoutUseCase.execute(refreshToken);
        authCookieService.clearAuthCookies(response);
        return ResponseEntity.noContent().build();
    }
}
