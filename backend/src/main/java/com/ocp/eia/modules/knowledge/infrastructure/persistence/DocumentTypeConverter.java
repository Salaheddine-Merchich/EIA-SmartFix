package com.ocp.eia.modules.knowledge.infrastructure.persistence;

import com.ocp.eia.modules.knowledge.domain.model.KnowledgeDocument.DocumentType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;

@Converter(autoApply = true)
public class DocumentTypeConverter implements AttributeConverter<DocumentType, String> {

    @Override
    public String convertToDatabaseColumn(DocumentType attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public DocumentType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return Arrays.stream(DocumentType.values())
                .filter(type -> type.getValue().equalsIgnoreCase(dbData))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown document type: " + dbData));
    }
}
