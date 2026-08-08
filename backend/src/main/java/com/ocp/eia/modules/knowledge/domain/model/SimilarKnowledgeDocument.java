package com.ocp.eia.modules.knowledge.domain.model;

import java.util.UUID;

public record SimilarKnowledgeDocument(
        UUID documentId,
        String title,
        String contentExcerpt, // First 500 chars or relevant excerpt
        String documentType,
        String equipmentFamily,
        String source,
        double similarity
) implements SimilarResult {
    
    @Override
    public UUID getId() {
        return documentId;
    }
    
    @Override
    public ResultType getType() {
        return ResultType.KNOWLEDGE_DOCUMENT;
    }
    /**
     * Create a truncated excerpt from full content for LLM context
     */
    public static String createExcerpt(String fullContent, int maxLength) {
        if (fullContent == null || fullContent.length() <= maxLength) {
            return fullContent;
        }
        
        String truncated = fullContent.substring(0, maxLength);
        int lastSpace = truncated.lastIndexOf(' ');
        if (lastSpace > maxLength * 0.8) {
            return truncated.substring(0, lastSpace) + "...";
        }
        return truncated + "...";
    }
}