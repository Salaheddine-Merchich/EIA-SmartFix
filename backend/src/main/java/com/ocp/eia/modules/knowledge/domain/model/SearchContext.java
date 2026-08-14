package com.ocp.eia.modules.knowledge.domain.model;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Contexte pour prioriser la recherche RAG basée sur l'équipement, la panne et les signaux texte.
 */
public record SearchContext(
        UUID equipmentId,
        UUID failureId,
        String equipmentFamily,
        String equipmentZone,
        String manufacturer,
        List<String> extractedFaultCodes,
        Double equipmentBoost,
        Double familyBoost,
        Double zoneBoost,
        Double manufacturerBoost
) {

    public static SearchContext none() {
        return new SearchContext(null, null, null, null, null, List.of(), 1.0, 1.0, 1.0, 1.0);
    }

    public static SearchContext of(UUID equipmentId, UUID failureId, String equipmentFamily, String equipmentZone) {
        return new SearchContext(equipmentId, failureId, equipmentFamily, equipmentZone, null, List.of(),
                2.0, 1.5, 1.2, 1.8);
    }

    public static SearchContext withBoosts(
            UUID equipmentId,
            UUID failureId,
            String equipmentFamily,
            String equipmentZone,
            double equipmentBoost,
            double familyBoost,
            double zoneBoost
    ) {
        return new SearchContext(equipmentId, failureId, equipmentFamily, equipmentZone, null, List.of(),
                equipmentBoost, familyBoost, zoneBoost, 1.8);
    }

    public static SearchContext withSignals(
            UUID equipmentId,
            UUID failureId,
            String equipmentFamily,
            String equipmentZone,
            String manufacturer,
            List<String> extractedFaultCodes,
            double equipmentBoost,
            double familyBoost,
            double zoneBoost,
            double manufacturerBoost
    ) {
        return new SearchContext(
                equipmentId,
                failureId,
                equipmentFamily,
                equipmentZone,
                manufacturer,
                extractedFaultCodes != null ? List.copyOf(extractedFaultCodes) : List.of(),
                equipmentBoost,
                familyBoost,
                zoneBoost,
                manufacturerBoost
        );
    }

    public boolean hasFilters() {
        return equipmentId != null
                || failureId != null
                || (equipmentFamily != null && !equipmentFamily.isBlank())
                || (equipmentZone != null && !equipmentZone.isBlank())
                || (manufacturer != null && !manufacturer.isBlank())
                || (extractedFaultCodes != null && !extractedFaultCodes.isEmpty());
    }

    public double calculateBoost(UUID resultEquipmentId, String resultFamily, String resultZone, String resultConstructeur) {
        if (equipmentId != null && equipmentId.equals(resultEquipmentId)) {
            return equipmentBoost;
        }

        if (manufacturer != null && !manufacturer.isBlank()
                && resultConstructeur != null
                && resultConstructeur.toLowerCase(Locale.ROOT).contains(manufacturer.toLowerCase(Locale.ROOT))) {
            return manufacturerBoost;
        }

        if (equipmentFamily != null && equipmentFamily.equalsIgnoreCase(resultFamily)) {
            return familyBoost;
        }

        if (equipmentZone != null && equipmentZone.equalsIgnoreCase(resultZone)) {
            return zoneBoost;
        }

        return 1.0;
    }
}
