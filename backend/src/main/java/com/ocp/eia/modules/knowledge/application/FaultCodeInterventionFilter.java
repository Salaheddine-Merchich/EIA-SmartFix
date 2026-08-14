package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.modules.knowledge.domain.model.QuerySignals;
import com.ocp.eia.modules.knowledge.domain.model.SimilarIntervention;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Quand la requête mentionne un code défaut, ne conserve que les interventions correspondant exactement.
 */
public final class FaultCodeInterventionFilter {

    private FaultCodeInterventionFilter() {
    }

    public static List<SimilarIntervention> apply(
            List<SimilarIntervention> interventions,
            QuerySignals signals,
            List<SimilarIntervention> exactMatches
    ) {
        if (!signals.hasFaultCodes() || interventions.isEmpty()) {
            return interventions;
        }

        Set<String> requestedCodes = signals.faultCodes().stream()
                .map(code -> code.toUpperCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());

        Set<UUID> exactIds = new HashSet<>();
        for (SimilarIntervention exact : exactMatches) {
            exactIds.add(exact.interventionId());
        }

        List<SimilarIntervention> filtered = interventions.stream()
                .filter(item -> matchesRequestedCode(item, requestedCodes, exactIds))
                .toList();

        if (!filtered.isEmpty()) {
            return filtered;
        }
        if (!exactMatches.isEmpty()) {
            return exactMatches;
        }
        return List.of();
    }

    private static boolean matchesRequestedCode(
            SimilarIntervention item,
            Set<String> requestedCodes,
            Set<UUID> exactIds
    ) {
        if (exactIds.contains(item.interventionId())) {
            return true;
        }
        if (item.faultCode() == null || item.faultCode().isBlank()) {
            return false;
        }
        return requestedCodes.contains(item.faultCode().toUpperCase(Locale.ROOT));
    }
}
