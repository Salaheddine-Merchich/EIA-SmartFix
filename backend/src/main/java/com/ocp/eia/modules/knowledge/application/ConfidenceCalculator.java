package com.ocp.eia.modules.knowledge.application;

public final class ConfidenceCalculator {

    private ConfidenceCalculator() {}

    public static double compute(double averageSimilarity, int filteredCount) {
        if (filteredCount <= 0 || averageSimilarity <= 0) {
            return 0.0;
        }
        double countFactor = Math.min(1.0, filteredCount / 3.0);
        double score = averageSimilarity * 100.0 * (0.75 + 0.25 * countFactor);
        return Math.round(Math.min(100.0, score) * 10.0) / 10.0;
    }

    public static String level(double confidenceScore) {
        if (confidenceScore > 85.0) {
            return "VERY_HIGH";
        }
        if (confidenceScore >= 70.0) {
            return "HIGH";
        }
        return "LOW";
    }
}
