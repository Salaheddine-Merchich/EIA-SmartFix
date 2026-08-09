package com.ocp.eia.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Set;

/**
 * Fails fast when a non-dev profile runs with a missing or placeholder JWT secret.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtSecretValidator {

    private static final String PLACEHOLDER =
            "change-this-to-a-secure-random-secret-at-least-256-bits-long-for-production";

    private final AppProperties appProperties;
    private final Environment environment;

    @PostConstruct
    public void validate() {
        Set<String> profiles = Set.copyOf(Arrays.asList(environment.getActiveProfiles()));
        boolean prodLike = profiles.contains("prod")
                || (!profiles.isEmpty() && profiles.stream().noneMatch(p -> p.equals("dev") || p.equals("test")));

        String secret = appProperties.getJwt().getSecret();
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("JWT_SECRET is required but missing");
        }
        if (prodLike && (PLACEHOLDER.equals(secret) || secret.length() < 32)) {
            throw new IllegalStateException(
                    "JWT_SECRET must be a strong secret (min 32 chars) when profile is not exclusively 'dev'");
        }
        if (PLACEHOLDER.equals(secret)) {
            log.warn("JWT_SECRET uses the default development placeholder — never deploy with this value");
        }
    }
}
