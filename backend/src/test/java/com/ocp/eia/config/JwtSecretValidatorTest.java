package com.ocp.eia.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtSecretValidatorTest {

    @Test
    void validate_devProfileWithPlaceholder_allowsBoot() {
        AppProperties props = new AppProperties();
        props.getJwt().setSecret(
                "change-this-to-a-secure-random-secret-at-least-256-bits-long-for-production");
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");
        JwtSecretValidator validator = new JwtSecretValidator(props, env);
        assertDoesNotThrow(validator::validate);
    }

    @Test
    void validate_prodProfileWithPlaceholder_fails() {
        AppProperties props = new AppProperties();
        props.getJwt().setSecret(
                "change-this-to-a-secure-random-secret-at-least-256-bits-long-for-production");
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        JwtSecretValidator validator = new JwtSecretValidator(props, env);
        assertThrows(IllegalStateException.class, validator::validate);
    }

    @Test
    void validate_prodProfileWithStrongSecret_ok() {
        AppProperties props = new AppProperties();
        props.getJwt().setSecret("a-very-strong-production-secret-key-32chars-min");
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        JwtSecretValidator validator = new JwtSecretValidator(props, env);
        assertDoesNotThrow(validator::validate);
    }
}
