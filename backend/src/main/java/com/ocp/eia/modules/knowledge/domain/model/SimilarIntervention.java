package com.ocp.eia.modules.knowledge.domain.model;

import java.util.UUID;

public record SimilarIntervention(
        UUID interventionId,
        String equipmentCode,
        String symptomes,
        String causeRacine,
        String actionsCorrectives,
        String analyseTechnique,
        double similarity
) implements SimilarResult {
    
    @Override
    public UUID getId() {
        return interventionId;
    }
    
    @Override
    public ResultType getType() {
        return ResultType.INTERVENTION;
    }
}
