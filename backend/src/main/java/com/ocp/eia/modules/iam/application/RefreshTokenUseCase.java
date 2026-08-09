package com.ocp.eia.modules.iam.application;

import com.ocp.eia.application.dto.AuthDto.AuthResponse;
import com.ocp.eia.application.dto.AuthDto.RefreshTokenRequest;
import com.ocp.eia.domain.model.User;
import com.ocp.eia.infrastructure.security.CustomUserDetailsService;
import com.ocp.eia.infrastructure.security.JwtService;
import com.ocp.eia.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenUseCase {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public AuthResponse execute(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new BadRequestException("Token de rafraîchissement invalide");
        }
        String email = jwtService.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
            throw new BadRequestException("Token de rafraîchissement expiré");
        }
        User user = userDetailsService.loadEntityByEmail(email);
        String accessToken = jwtService.generateAccessToken(userDetails, user.getRole().name(), user.getNomPrenom());
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);
        return new AuthResponse(accessToken, newRefreshToken, "Bearer", user.getRole().name(), user.getNomPrenom(), user.getEmail());
    }
}
