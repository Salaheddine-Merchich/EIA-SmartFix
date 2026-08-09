package com.ocp.eia.modules.knowledge.application;

import java.time.Duration;

/**
 * Parses Spring-style duration strings used by RAG performance config ({@code 180s}, {@code 500ms}).
 */
final class DurationParse {

    private DurationParse() {
    }

    static Duration of(String timeoutStr) {
        try {
            if (timeoutStr.endsWith("ms")) {
                return Duration.ofMillis(Long.parseLong(timeoutStr.substring(0, timeoutStr.length() - 2)));
            }
            if (timeoutStr.endsWith("s")) {
                return Duration.ofSeconds(Long.parseLong(timeoutStr.substring(0, timeoutStr.length() - 1)));
            }
            return Duration.ofSeconds(Long.parseLong(timeoutStr));
        } catch (Exception e) {
            return Duration.ofSeconds(10);
        }
    }
}
