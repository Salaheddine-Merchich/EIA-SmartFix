package com.ocp.eia.modules.knowledge.domain.model;

import java.util.UUID;

public record SimilarIntervention(
        UUID interventionId,
        String equipmentCode,
        String symptomes,
        String causeRacine,
        String actionsCorrectives,
        String analyseTechnique,
        double similarity,
        String faultCode,
        String constructeur,
        String equipmentFamily,
        String equipmentZone
) implements SimilarResult {

    /** Compatibilité appels sans métadonnées panne/équipement. */
    public SimilarIntervention(
            UUID interventionId,
            String equipmentCode,
            String symptomes,
            String causeRacine,
            String actionsCorrectives,
            String analyseTechnique,
            double similarity,
            String faultCode,
            String constructeur
    ) {
        this(interventionId, equipmentCode, symptomes, causeRacine, actionsCorrectives,
                analyseTechnique, similarity, faultCode, constructeur, null, null);
    }

    /** Compatibilité tests et appels sans métadonnées panne. */
    public SimilarIntervention(
            UUID interventionId,
            String equipmentCode,
            String symptomes,
            String causeRacine,
            String actionsCorrectives,
            String analyseTechnique,
            double similarity
    ) {
        this(interventionId, equipmentCode, symptomes, causeRacine, actionsCorrectives,
                analyseTechnique, similarity, null, null, null, null);
    }

    @Override
    public UUID getId() {
        return interventionId;
    }

    @Override
    public ResultType getType() {
        return ResultType.INTERVENTION;
    }
}
