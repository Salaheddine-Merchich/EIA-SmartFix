package com.ocp.eia.application.dto;

public final class KnowledgeDto {

    private KnowledgeDto() {}

    public record ReindexResponse(
            int processed,
            int indexed,
            int skipped,
            int errors
    ) {}
}
