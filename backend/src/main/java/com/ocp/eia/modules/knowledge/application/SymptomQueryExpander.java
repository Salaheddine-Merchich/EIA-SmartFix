package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.modules.knowledge.domain.model.QuerySignals;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Expansion de requêtes symptômes en mots-clés métier pour FTS et ILIKE.
 */
public final class SymptomQueryExpander {

    private static final Map<String, List<String>> CATEGORY_TRIGGERS = Map.of(
            "no_start", List.of(
                    "ne démarre plus", "ne demarre plus", "ne tourne plus",
                    "reste à l'arrêt", "reste a l'arret", "reste a l'arrêt",
                    "0 hz", "à l'arrêt", "a l'arret", "ne part plus", "bloqué", "bloque"
            ),
            "low_output", List.of(
                    "débit faible", "debit faible", "peu d'eau", "peu d eau", "débit eau", "debit eau"
            ),
            "overheat", List.of(
                    "surchauffe", "chauffe anormalement", "sent le brûlé", "sent le brule", "surchauffé"
            )
    );

    private static final Map<String, List<String>> CATEGORY_KEYWORDS = Map.of(
            "no_start", List.of("veille", "sommeil", "démarrage", "demarrage", "wake", "sleep", "lpn", "lut", "lfr", "0 hz"),
            "low_output", List.of("débit", "debit", "rotation", "ensoleillement", "eau"),
            "overheat", List.of("radiateur", "ventilateur", "surchauffe", "oh1", "oh2", "e21")
    );

    private static final Map<String, List<String>> CATEGORY_CONFLICTS = Map.of(
            "no_start", List.of(
                    "alarme plein", "reservoir plein", "réservoir plein", "a-tf", "plein eau",
                    "rotation inverse", "sens inverse", "debit faible", "débit faible"
            ),
            "low_output", List.of("veille", "sommeil", "0 hz")
    );

    private SymptomQueryExpander() {
    }

    public static List<String> detectCategories(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String lower = query.toLowerCase(Locale.ROOT);
        List<String> categories = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : CATEGORY_TRIGGERS.entrySet()) {
            for (String trigger : entry.getValue()) {
                if (lower.contains(trigger)) {
                    categories.add(entry.getKey());
                    break;
                }
            }
        }
        return List.copyOf(categories);
    }

    public static List<String> expandKeywords(List<String> categories) {
        Set<String> keywords = new LinkedHashSet<>();
        for (String category : categories) {
            List<String> expanded = CATEGORY_KEYWORDS.get(category);
            if (expanded != null) {
                keywords.addAll(expanded);
            }
        }
        return List.copyOf(keywords);
    }

    public static String buildFtsQuery(String originalQuery, QuerySignals signals) {
        if (originalQuery == null || originalQuery.isBlank()) {
            return originalQuery;
        }
        if (signals.symptomKeywords() == null || signals.symptomKeywords().isEmpty()) {
            return originalQuery;
        }
        StringBuilder sb = new StringBuilder(originalQuery.trim());
        for (String keyword : signals.symptomKeywords()) {
            sb.append(' ').append(keyword);
        }
        return sb.toString();
    }

    public static boolean hasSymptomConflict(String category, String symptomes, String causeRacine, String faultCode) {
        List<String> conflicts = CATEGORY_CONFLICTS.get(category);
        if (conflicts == null || conflicts.isEmpty()) {
            return false;
        }
        String combined = String.join(" ",
                symptomes != null ? symptomes : "",
                causeRacine != null ? causeRacine : "",
                faultCode != null ? faultCode : ""
        ).toLowerCase(Locale.ROOT);

        return conflicts.stream().anyMatch(combined::contains);
    }

    public static int countSymptomOverlap(String text, List<String> symptomKeywords) {
        if (text == null || text.isBlank() || symptomKeywords == null || symptomKeywords.isEmpty()) {
            return 0;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        int count = 0;
        for (String keyword : symptomKeywords) {
            if (lower.contains(keyword.toLowerCase(Locale.ROOT))) {
                count++;
            }
        }
        return count;
    }
}
