package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.modules.knowledge.domain.model.QuerySignals;
import com.ocp.eia.modules.knowledge.domain.model.SearchContext;
import com.ocp.eia.modules.knowledge.domain.model.SimilarIntervention;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Re-classe les résultats fusionnés en favorisant correspondances exactes de code et contexte sémantique.
 */
public final class RetrievalReranker {

    private static final double MISMATCH_PENALTY = 0.5;
    private static final double SYMPTOM_OVERLAP_THRESHOLD = 2;

    private RetrievalReranker() {
    }

    public static List<SimilarIntervention> rerank(
            List<SimilarIntervention> merged,
            List<SimilarIntervention> exactMatches,
            QuerySignals signals,
            SearchContext context,
            double exactCodeBoost,
            double symptomBoost,
            double zoneMismatchPenalty
    ) {
        if (merged.isEmpty() && exactMatches.isEmpty()) {
            return List.of();
        }

        Set<UUID> exactIds = new HashSet<>();
        for (SimilarIntervention exact : exactMatches) {
            exactIds.add(exact.interventionId());
        }

        List<String> requestedCodes = signals.hasFaultCodes()
                ? signals.faultCodes().stream().map(c -> c.toUpperCase(Locale.ROOT)).toList()
                : List.of();

        List<ScoredIntervention> scored = new ArrayList<>();

        for (SimilarIntervention item : merged) {
            scored.add(new ScoredIntervention(
                    item,
                    score(item, exactIds, requestedCodes, signals, context, exactCodeBoost, symptomBoost, zoneMismatchPenalty)
            ));
        }

        for (SimilarIntervention exact : exactMatches) {
            if (merged.stream().noneMatch(m -> m.interventionId().equals(exact.interventionId()))) {
                scored.add(new ScoredIntervention(exact, exactCodeBoost));
            }
        }

        scored.sort(Comparator.comparingDouble(ScoredIntervention::score).reversed());

        return scored.stream()
                .map(s -> withAdjustedSimilarity(s.intervention(), Math.min(1.0, s.score())))
                .toList();
    }

    private static double score(
            SimilarIntervention item,
            Set<UUID> exactIds,
            List<String> requestedCodes,
            QuerySignals signals,
            SearchContext context,
            double exactCodeBoost,
            double symptomBoost,
            double zoneMismatchPenalty
    ) {
        double base = item.similarity();

        if (exactIds.contains(item.interventionId())) {
            return exactCodeBoost;
        }

        if (!requestedCodes.isEmpty()) {
            if (item.faultCode() != null) {
                String fault = item.faultCode().toUpperCase(Locale.ROOT);
                if (requestedCodes.contains(fault)) {
                    return Math.min(1.0, base * exactCodeBoost);
                }
                return base * MISMATCH_PENALTY;
            }
            return base * MISMATCH_PENALTY;
        }

        return applySemanticScore(base, item, signals, context, symptomBoost, zoneMismatchPenalty);
    }

    private static double applySemanticScore(
            double base,
            SimilarIntervention item,
            QuerySignals signals,
            SearchContext context,
            double symptomBoost,
            double zoneMismatchPenalty
    ) {
        double score = base;

        if (context != null && context.equipmentZone() != null && !context.equipmentZone().isBlank()) {
            if (item.equipmentZone() != null
                    && context.equipmentZone().equalsIgnoreCase(item.equipmentZone())) {
                score *= context.zoneBoost();
            } else if (item.equipmentZone() != null && !item.equipmentZone().isBlank()) {
                score *= zoneMismatchPenalty;
            }
        }

        if (context != null && context.equipmentFamily() != null && !context.equipmentFamily().isBlank()) {
            if (item.equipmentFamily() != null
                    && context.equipmentFamily().equalsIgnoreCase(item.equipmentFamily())) {
                score *= context.familyBoost();
            }
        }

        if (signals.symptomKeywords() != null && !signals.symptomKeywords().isEmpty()) {
            String combined = combineText(item.symptomes(), item.causeRacine(), item.analyseTechnique());
            int overlap = SymptomQueryExpander.countSymptomOverlap(combined, signals.symptomKeywords());
            if (overlap >= SYMPTOM_OVERLAP_THRESHOLD) {
                score *= symptomBoost;
            } else if (overlap == 1) {
                score *= 1.2;
            }
        }

        if (signals.hasSymptomCategory("no_start") && containsRotationInverse(item)) {
            score *= zoneMismatchPenalty;
        }

        for (String category : signals.symptomCategories()) {
            if (SymptomQueryExpander.hasSymptomConflict(
                    category, item.symptomes(), item.causeRacine(), item.faultCode())) {
                score *= zoneMismatchPenalty;
            }
        }

        return score;
    }

    private static boolean containsRotationInverse(SimilarIntervention item) {
        String combined = combineText(item.symptomes(), item.causeRacine()).toLowerCase(Locale.ROOT);
        return combined.contains("rotation inverse") || combined.contains("sens inverse");
    }

    private static String combineText(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                if (!sb.isEmpty()) {
                    sb.append(' ');
                }
                sb.append(part);
            }
        }
        return sb.toString();
    }

    private static SimilarIntervention withAdjustedSimilarity(SimilarIntervention item, double similarity) {
        return new SimilarIntervention(
                item.interventionId(),
                item.equipmentCode(),
                item.symptomes(),
                item.causeRacine(),
                item.actionsCorrectives(),
                item.analyseTechnique(),
                similarity,
                item.faultCode(),
                item.constructeur(),
                item.equipmentFamily(),
                item.equipmentZone()
        );
    }

    private record ScoredIntervention(SimilarIntervention intervention, double score) {
    }
}
