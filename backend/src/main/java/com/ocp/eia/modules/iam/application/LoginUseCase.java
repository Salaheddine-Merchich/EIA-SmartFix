package com.ocp.eia.modules.iam.application;

import com.ocp.eia.application.dto.AuthDto.AuthResponse;
import com.ocp.eia.application.dto.AuthDto.LoginRequest;
import com.ocp.eia.domain.model.User;
import com.ocp.eia.infrastructure.security.CustomUserDetailsService;
import com.ocp.eia.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginUseCase {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthResponse execute(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        User user = userDetailsService.loadEntityByEmail(request.email());
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        String accessToken = jwtService.generateAccessToken(userDetails, user.getRole().name(), user.getNomPrenom());
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        refreshTokenService.persist(refreshToken, user.getId());
        return new AuthResponse(accessToken, refreshToken, "Bearer", user.getRole().name(), user.getNomPrenom(), user.getEmail());
    }
}
