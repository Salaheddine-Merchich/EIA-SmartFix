package com.ocp.eia.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDto {

    private AuthDto() {}

    public record LoginRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 100) String password
    ) {}

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            String role,
            String nomPrenom,
            String email
    ) {}

    /** refreshToken optional when sent via HttpOnly cookie. */
    public record RefreshTokenRequest(
            @Size(max = 2000) String refreshToken
    ) {}
}
