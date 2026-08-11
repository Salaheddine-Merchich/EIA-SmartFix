package com.ocp.eia.modules.iam.application;

import com.ocp.eia.application.dto.AuthDto.AuthResponse;
import com.ocp.eia.domain.model.User;
import com.ocp.eia.infrastructure.security.CustomUserDetailsService;
import com.ocp.eia.infrastructure.security.JwtService;
import com.ocp.eia.shared.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenUseCase {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthResponse execute(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UnauthorizedException("Session invalide");
        }
        try {
            if (!jwtService.isRefreshToken(refreshToken)) {
                throw new UnauthorizedException("Session invalide");
            }
            String email = jwtService.extractUsername(refreshToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            if (!jwtService.isTokenValid(refreshToken, userDetails)) {
                throw new UnauthorizedException("Session expirée");
            }
            refreshTokenService.assertActive(refreshToken);
            refreshTokenService.revoke(refreshToken);

            User user = userDetailsService.loadEntityByEmail(email);
            String accessToken = jwtService.generateAccessToken(userDetails, user.getRole().name(), user.getNomPrenom());
            String newRefreshToken = jwtService.generateRefreshToken(userDetails);
            refreshTokenService.persist(newRefreshToken, user.getId());
            return new AuthResponse(
                    accessToken,
                    newRefreshToken,
                    "Bearer",
                    user.getRole().name(),
                    user.getNomPrenom(),
                    user.getEmail()
            );
        } catch (UnauthorizedException e) {
            throw e;
        } catch (UsernameNotFoundException e) {
            throw new UnauthorizedException("Session invalide");
        } catch (Exception e) {
            throw new UnauthorizedException("Session invalide");
        }
    }
}
