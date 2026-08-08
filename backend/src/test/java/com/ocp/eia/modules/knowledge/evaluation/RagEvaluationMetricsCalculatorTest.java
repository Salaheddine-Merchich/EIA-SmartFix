package com.ocp.eia.modules.knowledge.evaluation;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RagEvaluationMetricsCalculatorTest {

    private static final UUID TARGET = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Test
    void rankOfExpected_foundAtPositionTwo() {
        List<UUID> ids = List.of(UUID.randomUUID(), TARGET, UUID.randomUUID());
        assertEquals(2, RagEvaluationMetricsCalculator.rankOfExpected(ids, TARGET));
    }

    @Test
    void rankOfExpected_notFound_returnsZero() {
        assertEquals(0, RagEvaluationMetricsCalculator.rankOfExpected(List.of(UUID.randomUUID()), TARGET));
    }

    @Test
    void reciprocalRank_atRankOne() {
        assertEquals(1.0, RagEvaluationMetricsCalculator.reciprocalRank(1));
    }

    @Test
    void reciprocalRank_notFound_returnsZero() {
        assertEquals(0.0, RagEvaluationMetricsCalculator.reciprocalRank(0));
    }

    @Test
    void hitAt_withinTopK() {
        List<UUID> ids = List.of(UUID.randomUUID(), TARGET);
        assertTrue(RagEvaluationMetricsCalculator.hitAt(ids, TARGET, 3));
        assertFalse(RagEvaluationMetricsCalculator.hitAt(ids, TARGET, 1));
    }

    @Test
    void hitRateAt_computesPercentage() {
        RagEvaluationCase case1 = new RagEvaluationCase("c1", "q1", TARGET, "d1");
        RagEvaluationResult hit = result(case1, List.of(TARGET), true, true, true, 1.0);
        RagEvaluationResult miss = result(case1, List.of(UUID.randomUUID()), false, false, false, 0.0);

        assertEquals(50.0, RagEvaluationMetricsCalculator.hitRateAt(List.of(hit, miss), 1));
    }

    @Test
    void meanReciprocalRank_averagesReciprocalRanks() {
        RagEvaluationCase c = new RagEvaluationCase("c", "q", TARGET, "d");
        RagEvaluationResult r1 = result(c, List.of(TARGET), true, true, true, 1.0);
        RagEvaluationResult r2 = result(c, List.of(UUID.randomUUID(), TARGET), false, true, true, 0.5);

        assertEquals(0.75, RagEvaluationMetricsCalculator.meanReciprocalRank(List.of(r1, r2)));
    }

    private static RagEvaluationResult result(
            RagEvaluationCase evalCase,
            List<UUID> retrieved,
            boolean hit1,
            boolean hit3,
            boolean hit5,
            double rr) {
        return new RagEvaluationResult(
                evalCase, retrieved, List.of(), RagEvaluationMetricsCalculator.rankOfExpected(retrieved, TARGET),
                hit1, hit3, hit5, rr, 0.85, 2, 1,
                new RagEvaluationTimings(10, 5, 3, 1, 20, 39),
                true, null
        );
    }
}
