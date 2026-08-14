package com.ocp.eia.modules.knowledge.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssistQueryValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"I", "P", "PV", "  ", "", "iiiiiiiiii", "aaaaaaaaaa", "abcdefghij"})
    void shortOrNoiseQueries_areInvalid(String query) {
        assertFalse(AssistQueryValidator.isValid(query));
    }

    @Test
    void faultCodeQuery_isValid() {
        assertTrue(AssistQueryValidator.isValid("E21"));
        assertTrue(AssistQueryValidator.isValid("F001"));
    }

    @Test
    void longDescription_isValid() {
        assertTrue(AssistQueryValidator.isValid("Pompe PV ne démarre plus"));
        assertTrue(AssistQueryValidator.isValid("panne variateur"));
    }
}
