package com.ocp.eia.application.dto;

import jakarta.validation.constraints.NotBlank;

public final class AuthDto {

    private AuthDto() {}

    public record LoginRequest(
            @NotBlank String email,
            @NotBlank String password
    ) {}

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            String role,
            String nomPrenom,
            String email
    ) {}

    public record RefreshTokenRequest(
            @NotBlank String refreshToken
    ) {}
}
