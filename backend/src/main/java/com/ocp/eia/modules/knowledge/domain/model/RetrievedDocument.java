package com.ocp.eia.modules.knowledge.domain.model;

import java.util.UUID;

public record RetrievedDocument(
        UUID interventionId,
        String equipmentCode,
        String symptomes,
        String causeRacine,
        double similarity
) {}
