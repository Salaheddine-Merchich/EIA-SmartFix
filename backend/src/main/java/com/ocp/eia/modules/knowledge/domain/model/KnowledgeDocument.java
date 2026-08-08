package com.ocp.eia.modules.knowledge.domain.model;

// import com.ocp.eia.modules.knowledge.application.KnowledgeDocumentIndexListener; // Temporarily removed
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "knowledge_documents")
// @EntityListeners(KnowledgeDocumentIndexListener.class) // Temporarily disabled
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Convert(converter = DocumentTypeConverter.class)
    @Column(name = "document_type", nullable = false, length = 100)
    private DocumentType documentType;

    @Column(name = "equipment_family", length = 100)
    private String equipmentFamily;

    @Column(name = "source", nullable = false, length = 200)
    private String source;

    @Column(name = "language", length = 10)
    private String language = "fr";

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "active")
    private Boolean active = true;

    public enum DocumentType {
        MANUAL("manual"),
        PROCEDURE("procedure"), 
        GUIDE("guide"),
        FAQ("faq"),
        STANDARD("standard"),
        TROUBLESHOOTING("troubleshooting");

        private final String value;

        DocumentType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}