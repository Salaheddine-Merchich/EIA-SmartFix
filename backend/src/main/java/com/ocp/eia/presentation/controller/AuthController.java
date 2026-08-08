package com.ocp.eia.presentation.controller;

import com.ocp.eia.application.dto.AuthDto.AuthResponse;
import com.ocp.eia.application.dto.AuthDto.LoginRequest;
import com.ocp.eia.application.dto.AuthDto.RefreshTokenRequest;
import com.ocp.eia.modules.iam.application.LoginUseCase;
import com.ocp.eia.modules.iam.application.RefreshTokenUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    @PostMapping("/login")
    @Operation(summary = "Connexion utilisateur")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(loginUseCase.execute(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rafraîchir le token JWT")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(refreshTokenUseCase.execute(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Déconnexion (côté client)")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}
