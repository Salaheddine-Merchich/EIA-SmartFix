package com.ocp.eia.modules.iam.application;

import com.ocp.eia.domain.model.RefreshToken;
import com.ocp.eia.domain.repository.RefreshTokenRepository;
import com.ocp.eia.infrastructure.security.JwtService;
import com.ocp.eia.shared.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Transactional
    public void persist(String refreshJwt, UUID userId) {
        UUID jti = jwtService.extractJti(refreshJwt);
        Instant expiresAt = jwtService.extractExpiration(refreshJwt);
        refreshTokenRepository.save(RefreshToken.builder()
                .jti(jti)
                .userId(userId)
                .expiresAt(expiresAt)
                .createdAt(Instant.now())
                .build());
    }

    @Transactional(readOnly = true)
    public void assertActive(String refreshJwt) {
        UUID jti;
        try {
            jti = jwtService.extractJti(refreshJwt);
        } catch (Exception e) {
            throw new UnauthorizedException("Session invalide");
        }
        RefreshToken stored = refreshTokenRepository.findById(jti)
                .orElseThrow(() -> new UnauthorizedException("Session invalide ou révoquée"));
        if (!stored.isActive()) {
            throw new UnauthorizedException("Session invalide ou révoquée");
        }
    }

    @Transactional
    public void revoke(String refreshJwt) {
        try {
            UUID jti = jwtService.extractJti(refreshJwt);
            refreshTokenRepository.findById(jti).ifPresent(token -> {
                if (token.getRevokedAt() == null) {
                    token.setRevokedAt(Instant.now());
                    refreshTokenRepository.save(token);
                }
            });
        } catch (Exception ignored) {
            // Invalid token — nothing to revoke
        }
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        refreshTokenRepository.revokeAllForUser(userId, Instant.now());
    }
}
