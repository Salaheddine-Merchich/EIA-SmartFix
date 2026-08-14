package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.modules.knowledge.domain.model.QuerySignals;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extrait codes défaut, constructeur, zone/famille équipement et symptômes depuis une requête en langage naturel.
 */
public final class QuerySignalExtractor {

    private static final Pattern CODE_PATTERNS = Pattern.compile(
            "\\b(?:"
                    + "2310-TRV|A581-TRV|"
                    + "OUt[1-9]|OC\\d?|OV\\d?|PV\\d+|"
                    + "E\\d{2}|"
                    + "A\\.\\d+|"
                    + "[A-Z]{1,2}\\d{3,4}[A-Z0-9]?|"
                    + "F\\d{3}|"
                    + "\\d{4}"
                    + ")\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Map<String, String> MANUFACTURER_ALIASES = Map.ofEntries(
            Map.entry("hitachi", "Hitachi"),
            Map.entry("abb", "ABB"),
            Map.entry("goodrive", "Goodrive"),
            Map.entry("veichi", "VEICHI"),
            Map.entry("siemens", "Siemens"),
            Map.entry("acs880", "ABB"),
            Map.entry("sj200", "Hitachi"),
            Map.entry("si23", "VEICHI")
    );

    private static final Map<String, String> EQUIPMENT_HINTS = Map.ofEntries(
            Map.entry("filature", "filature"),
            Map.entry("traverse", "traverse"),
            Map.entry("convoyeur", "convoyage"),
            Map.entry("convoyage", "convoyage"),
            Map.entry("pompe", "pompe"),
            Map.entry("moteur", "moteur"),
            Map.entry("variateur", "variateur"),
            Map.entry("goodrive", "goodrive"),
            Map.entry("pv", "pv")
    );

    /** Phrases composées en premier (priorité décroissante). */
    private static final List<ZoneFamilyRule> ZONE_FAMILY_RULES = List.of(
            // Pompe PV : panne souvent sur le variateur — filtrer la zone, pas la famille
            new ZoneFamilyRule("pompe pv", null, "Station PV"),
            new ZoneFamilyRule("pompe solaire", null, "Station PV"),
            new ZoneFamilyRule("pompage solaire", null, "Station PV"),
            new ZoneFamilyRule("station solaire", null, "Station PV"),
            new ZoneFamilyRule("photovoltaïque", null, "Station PV"),
            new ZoneFamilyRule("photovoltaique", null, "Station PV"),
            new ZoneFamilyRule("variateur pv", "Variateur", "Station PV"),
            new ZoneFamilyRule("goodrive pv", "Variateur", "Station PV"),
            new ZoneFamilyRule("filature", null, "Filature"),
            new ZoneFamilyRule("traverse", null, "Filature"),
            new ZoneFamilyRule("convoyeur", "Convoyeur", "Zone Convoyage"),
            new ZoneFamilyRule("convoyage", "Convoyeur", "Zone Convoyage"),
            new ZoneFamilyRule("pompe", "Pompe", null),
            new ZoneFamilyRule(" var pv", null, "Station PV"),
            new ZoneFamilyRule(" pv ", null, "Station PV")
    );

    private QuerySignalExtractor() {
    }

    public static QuerySignals extract(String query) {
        if (query == null || query.isBlank()) {
            return QuerySignals.empty();
        }

        String normalized = query.trim();
        String lower = normalized.toLowerCase(Locale.ROOT);

        Set<String> codes = new LinkedHashSet<>();
        Matcher matcher = CODE_PATTERNS.matcher(normalized);
        while (matcher.find()) {
            codes.add(normalizeCode(matcher.group()));
        }

        Optional<String> manufacturer = detectManufacturer(lower);
        Optional<String> equipmentHint = detectEquipmentHint(lower);
        ZoneFamilyMatch zoneFamily = detectZoneFamily(lower);
        List<String> symptomCategories = SymptomQueryExpander.detectCategories(normalized);
        List<String> symptomKeywords = SymptomQueryExpander.expandKeywords(symptomCategories);

        return new QuerySignals(
                List.copyOf(codes),
                manufacturer,
                equipmentHint,
                zoneFamily.family(),
                zoneFamily.zone(),
                symptomKeywords,
                symptomCategories
        );
    }

    static String normalizeCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        String trimmed = raw.trim();
        if (trimmed.matches("(?i)OUt\\d")) {
            return "OUt" + trimmed.substring(3);
        }
        if (trimmed.matches("(?i)ou?t\\d")) {
            return "OUt" + trimmed.replaceAll("(?i)^ou?t", "");
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    private static Optional<String> detectManufacturer(String lower) {
        for (Map.Entry<String, String> entry : MANUFACTURER_ALIASES.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }

    private static Optional<String> detectEquipmentHint(String lower) {
        for (Map.Entry<String, String> entry : EQUIPMENT_HINTS.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }

    private static ZoneFamilyMatch detectZoneFamily(String lower) {
        String family = null;
        String zone = null;

        for (ZoneFamilyRule rule : ZONE_FAMILY_RULES) {
            if (lower.contains(rule.phrase())) {
                if (rule.family() != null) {
                    family = rule.family();
                }
                if (rule.zone() != null) {
                    zone = rule.zone();
                }
            }
        }

        if (lower.contains("si23") && (lower.contains("pv") || lower.contains("solaire") || lower.contains("pompe"))) {
            if (family == null) {
                family = "Variateur";
            }
            if (zone == null) {
                zone = "Station PV";
            }
        }

        if (isStationPvPumpContext(lower, zone)) {
            family = null;
        }

        return new ZoneFamilyMatch(
                family != null ? Optional.of(family) : Optional.empty(),
                zone != null ? Optional.of(zone) : Optional.empty()
        );
    }

    private static boolean isStationPvPumpContext(String lower, String zone) {
        if (!"Station PV".equals(zone)) {
            return false;
        }
        return lower.contains("pompe pv")
                || lower.contains("pompe solaire")
                || lower.contains("pompage solaire")
                || lower.contains("station solaire")
                || (lower.contains("pompe") && lower.contains(" pv"));
    }

    private record ZoneFamilyRule(String phrase, String family, String zone) {
    }

    private record ZoneFamilyMatch(Optional<String> family, Optional<String> zone) {
    }
}
