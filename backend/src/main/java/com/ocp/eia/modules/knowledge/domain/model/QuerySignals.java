package com.ocp.eia.modules.knowledge.domain.model;

import java.util.List;
import java.util.Optional;

/**
 * Signaux extraits d'une requête utilisateur pour affiner la recherche RAG.
 */
public record QuerySignals(
        List<String> faultCodes,
        Optional<String> manufacturer,
        Optional<String> equipmentHint,
        Optional<String> equipmentFamily,
        Optional<String> equipmentZone,
        List<String> symptomKeywords,
        List<String> symptomCategories
) {
    public static QuerySignals empty() {
        return new QuerySignals(
                List.of(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                List.of(),
                List.of()
        );
    }

    public boolean hasFaultCodes() {
        return faultCodes != null && !faultCodes.isEmpty();
    }

    public String primaryFaultCode() {
        return hasFaultCodes() ? faultCodes.get(0) : null;
    }

    public boolean hasSemanticContext() {
        return equipmentFamily().isPresent()
                || equipmentZone().isPresent()
                || (symptomKeywords != null && !symptomKeywords.isEmpty());
    }

    public boolean hasSymptomCategory(String category) {
        return symptomCategories != null && symptomCategories.contains(category);
    }
}
