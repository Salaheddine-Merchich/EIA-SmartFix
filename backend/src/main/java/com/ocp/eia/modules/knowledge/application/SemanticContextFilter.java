package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.modules.knowledge.domain.model.QuerySignals;
import com.ocp.eia.modules.knowledge.domain.model.SimilarIntervention;

import java.util.List;
import java.util.Locale;

/**
 * Filtre souple par zone/famille/symptômes quand aucun code défaut n'est extrait.
 */
public final class SemanticContextFilter {

    private SemanticContextFilter() {
    }

    public static List<SimilarIntervention> apply(
            List<SimilarIntervention> interventions,
            QuerySignals signals
    ) {
        if (signals.hasFaultCodes() || interventions.isEmpty() || !signals.hasSemanticContext()) {
            return interventions;
        }

        List<SimilarIntervention> withoutConflicts = interventions.stream()
                .filter(item -> !hasCategoryConflict(item, signals))
                .toList();

        if (!withoutConflicts.isEmpty()) {
            interventions = withoutConflicts;
        }

        String targetZone = signals.equipmentZone().orElse(null);
        String targetFamily = signals.equipmentFamily().orElse(null);
        List<String> symptomKeywords = signals.symptomKeywords();

        if (targetZone == null && targetFamily == null) {
            return preferSymptomOverlap(interventions, symptomKeywords);
        }

        List<SimilarIntervention> filtered = interventions.stream()
                .filter(item -> {
                    boolean zoneOk = targetZone == null || matchesZone(item, targetZone);
                    boolean familyOk = targetFamily == null || matchesFamily(item, targetFamily);
                    return zoneOk && familyOk;
                })
                .toList();

        if (!filtered.isEmpty()) {
            List<SimilarIntervention> withSymptoms = preferSymptomOverlap(filtered, symptomKeywords);
            if (!withSymptoms.isEmpty()) {
                return withSymptoms;
            }
            return filtered;
        }

        List<SimilarIntervention> zoneMatches = interventions.stream()
                .filter(item -> matchesZone(item, targetZone))
                .toList();
        if (!zoneMatches.isEmpty()) {
            return preferSymptomOverlap(zoneMatches, symptomKeywords);
        }

        return interventions;
    }

    private static List<SimilarIntervention> preferSymptomOverlap(
            List<SimilarIntervention> interventions,
            List<String> symptomKeywords
    ) {
        if (symptomKeywords == null || symptomKeywords.isEmpty()) {
            return interventions;
        }
        List<SimilarIntervention> withSymptoms = interventions.stream()
                .filter(item -> hasSymptomOverlap(item, symptomKeywords))
                .toList();
        return withSymptoms.isEmpty() ? interventions : withSymptoms;
    }

    private static boolean hasCategoryConflict(SimilarIntervention item, QuerySignals signals) {
        for (String category : signals.symptomCategories()) {
            if (SymptomQueryExpander.hasSymptomConflict(
                    category, item.symptomes(), item.causeRacine(), item.faultCode())) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesZone(SimilarIntervention item, String targetZone) {
        if (targetZone == null || targetZone.isBlank()) {
            return true;
        }
        return item.equipmentZone() != null
                && item.equipmentZone().equalsIgnoreCase(targetZone);
    }

    private static boolean matchesFamily(SimilarIntervention item, String targetFamily) {
        if (targetFamily == null || targetFamily.isBlank()) {
            return true;
        }
        return item.equipmentFamily() != null
                && item.equipmentFamily().equalsIgnoreCase(targetFamily);
    }

    private static boolean hasSymptomOverlap(SimilarIntervention item, List<String> symptomKeywords) {
        if (symptomKeywords == null || symptomKeywords.isEmpty()) {
            return true;
        }
        String combined = String.join(" ",
                nullToEmpty(item.symptomes()),
                nullToEmpty(item.causeRacine()),
                nullToEmpty(item.analyseTechnique()),
                nullToEmpty(item.faultCode())
        ).toLowerCase(Locale.ROOT);

        return symptomKeywords.stream()
                .anyMatch(k -> combined.contains(k.toLowerCase(Locale.ROOT)));
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
