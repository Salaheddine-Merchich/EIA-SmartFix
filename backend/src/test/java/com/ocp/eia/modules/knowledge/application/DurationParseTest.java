package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.config.AppProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DurationParseTest {

    @Test
    void of_parsesSecondsAndMillis() {
        assertEquals(Duration.ofSeconds(180), DurationParse.of("180s"));
        assertEquals(Duration.ofMillis(500), DurationParse.of("500ms"));
        assertEquals(Duration.ofSeconds(45), DurationParse.of("45"));
    }

    @Test
    void of_invalid_fallsBackTo10s() {
        assertEquals(Duration.ofSeconds(10), DurationParse.of("not-a-duration"));
    }

    @Test
    void appProperties_defaultLlmTimeout_alignsWithStreamBudget() {
        assertEquals("180s", new AppProperties().getAi().getRag().getPerformance().getLlmTimeout());
        assertEquals(
                Duration.ofSeconds(180),
                DurationParse.of(new AppProperties().getAi().getRag().getPerformance().getLlmTimeout())
        );
    }
}
