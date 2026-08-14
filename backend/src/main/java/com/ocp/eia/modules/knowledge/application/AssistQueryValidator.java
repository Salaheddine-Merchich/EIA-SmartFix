package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.modules.knowledge.domain.model.QuerySignals;

import java.util.Arrays;
import java.util.Locale;

/**
 * Validates AI assist queries: rejects noise and requires meaningful content
 * for free-text descriptions (min 10 chars, or recognized fault code).
 */
public final class AssistQueryValidator {

    public static final int MIN_FREE_TEXT_LENGTH = 10;
    public static final int MIN_FAULT_CODE_LENGTH = 3;
    private static final int MIN_DISTINCT_CHARACTERS = 3;
    private static final double MAX_SINGLE_CHAR_RATIO = 0.60;

    private AssistQueryValidator() {}

    public static boolean isValid(String description) {
        String trimmed = description == null ? "" : description.trim();
        if (trimmed.isEmpty()) {
            return false;
        }

        QuerySignals signals = QuerySignalExtractor.extract(trimmed);
        if (signals.hasFaultCodes()) {
            return trimmed.length() >= MIN_FAULT_CODE_LENGTH;
        }
        if (signals.hasSemanticContext()) {
            return trimmed.length() >= MIN_FAULT_CODE_LENGTH;
        }
        if (trimmed.length() < MIN_FREE_TEXT_LENGTH) {
            return false;
        }
        return hasMeaningfulFreeText(trimmed);
    }

    static boolean hasMeaningfulFreeText(String trimmed) {
        if (hasDominantRepeatedCharacter(trimmed)) {
            return false;
        }
        long distinctChars = trimmed.toLowerCase(Locale.ROOT).chars().distinct().count();
        if (distinctChars < MIN_DISTINCT_CHARACTERS) {
            return false;
        }
        long significantWords = Arrays.stream(trimmed.split("\\s+"))
                .map(word -> word.replaceAll("[^\\p{L}]", ""))
                .filter(word -> word.length() >= 2)
                .count();
        return significantWords >= 2;
    }

    private static boolean hasDominantRepeatedCharacter(String trimmed) {
        if (trimmed.isEmpty()) {
            return false;
        }
        for (char c : trimmed.toCharArray()) {
            int count = 0;
            for (char ch : trimmed.toCharArray()) {
                if (ch == c) {
                    count++;
                }
            }
            if ((double) count / trimmed.length() > MAX_SINGLE_CHAR_RATIO) {
                return true;
            }
        }
        return false;
    }
}
