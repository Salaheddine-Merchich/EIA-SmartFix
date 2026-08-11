package com.ocp.eia.modules.iam.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LogoutUseCase {

    private final RefreshTokenService refreshTokenService;

    @Transactional
    public void execute(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revoke(refreshToken);
        }
    }
}
