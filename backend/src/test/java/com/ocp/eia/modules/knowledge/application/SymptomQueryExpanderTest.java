package com.ocp.eia.modules.knowledge.application;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SymptomQueryExpanderTest {

    @Test
    void detectCategories_noStart() {
        List<String> categories = SymptomQueryExpander.detectCategories("Pompe ne démarre plus");

        assertTrue(categories.contains("no_start"));
    }

    @Test
    void expandKeywords_noStart_includesVeilleAndSommeil() {
        List<String> keywords = SymptomQueryExpander.expandKeywords(List.of("no_start"));

        assertTrue(keywords.contains("veille"));
        assertTrue(keywords.contains("sommeil"));
        assertTrue(keywords.contains("lpn"));
    }

    @Test
    void countSymptomOverlap_matchesMultipleKeywords() {
        int overlap = SymptomQueryExpander.countSymptomOverlap(
                "Variateur en veille 0 Hz mode sommeil",
                List.of("veille", "sommeil", "démarrage")
        );

        assertEquals(2, overlap);
    }
}
