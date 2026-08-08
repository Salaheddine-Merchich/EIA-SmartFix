package com.ocp.eia.modules.knowledge.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfidenceCalculatorTest {

    @Test
    void compute_withStrongSimilarity_returnsHighScore() {
        double score = ConfidenceCalculator.compute(0.91, 3);
        assertTrue(score > 85.0);
        assertEquals("VERY_HIGH", ConfidenceCalculator.level(score));
    }

    @Test
    void compute_withModerateSimilarity_returnsHighLevel() {
        double score = ConfidenceCalculator.compute(0.78, 2);
        assertTrue(score >= 70.0 && score <= 85.0);
        assertEquals("HIGH", ConfidenceCalculator.level(score));
    }

    @Test
    void compute_withNoResults_returnsZero() {
        assertEquals(0.0, ConfidenceCalculator.compute(0.0, 0));
        assertEquals("LOW", ConfidenceCalculator.level(0.0));
    }

    @Test
    void level_below70_isLow() {
        assertEquals("LOW", ConfidenceCalculator.level(65.0));
    }
}
