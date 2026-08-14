package com.ocp.eia.modules.knowledge.evaluation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RagEvaluationDatasetTest {

    @Test
    void standardCases_containsRepresentativeScenarios() {
        var cases = RagEvaluationDataset.standardCases();

        assertFalse(cases.isEmpty());
        assertTrue(cases.size() >= 8);
        assertTrue(cases.stream().anyMatch(c -> c.caseId().equals("E21-hitachi-surchauffe")));
        assertTrue(cases.stream().anyMatch(c -> c.question().contains("E21")));
        assertTrue(cases.stream().anyMatch(c -> c.caseId().equals("F001-inconnu")));
        assertTrue(cases.stream().anyMatch(c -> c.caseId().equals("pompe-pv-no-start")));
        assertTrue(cases.stream().anyMatch(c -> c.expectedInterventionId() == null));
    }

    @Test
    void standardCases_allHaveValidQuestions() {
        RagEvaluationDataset.standardCases().forEach(c -> {
            assertNotNull(c.caseId());
            assertFalse(c.question().isBlank());
        });
    }
}
