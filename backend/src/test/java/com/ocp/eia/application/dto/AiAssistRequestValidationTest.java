package com.ocp.eia.application.dto;

import com.ocp.eia.application.dto.AiDto.AiAssistRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiAssistRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void blankDescription_isInvalid() {
        Set<ConstraintViolation<AiAssistRequest>> violations =
                validator.validate(new AiAssistRequest(null, null, " ", null));
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> "description".equals(v.getPropertyPath().toString())));
    }

    @Test
    void tooLongDescription_isInvalid() {
        Set<ConstraintViolation<AiAssistRequest>> violations =
                validator.validate(new AiAssistRequest(null, null, "x".repeat(4001), null));
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> "description".equals(v.getPropertyPath().toString())));
    }

    @Test
    void validDescription_isAccepted() {
        Set<ConstraintViolation<AiAssistRequest>> violations =
                validator.validate(new AiAssistRequest(null, null, "Variateur en défaut", 3));
        assertTrue(violations.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"I", "P", "iiiiiiiiii", "abcdefghij"})
    void noiseDescription_isInvalid(String description) {
        Set<ConstraintViolation<AiAssistRequest>> violations =
                validator.validate(new AiAssistRequest(null, null, description, null));
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> "description".equals(v.getPropertyPath().toString())));
    }

    @Test
    void faultCodeDescription_isAccepted() {
        Set<ConstraintViolation<AiAssistRequest>> violations =
                validator.validate(new AiAssistRequest(null, null, "E21", null));
        assertTrue(violations.isEmpty());
    }

    @Test
    void longDescription_isAccepted() {
        Set<ConstraintViolation<AiAssistRequest>> violations =
                validator.validate(new AiAssistRequest(null, null, "Pompe PV ne démarre plus", null));
        assertTrue(violations.isEmpty());
    }
}
