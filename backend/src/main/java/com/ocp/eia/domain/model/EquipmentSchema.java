package com.ocp.eia.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "equipment_schemas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipmentSchema {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Column(nullable = false, length = 200)
    private String label;

    @Column(name = "schema_type", nullable = false, length = 50)
    private String schemaType;

    @Column(name = "source_pdf", length = 300)
    private String sourcePdf;

    @Column(name = "source_page")
    private Integer sourcePage;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(columnDefinition = "TEXT")
    private String caption;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "trigger_keywords", nullable = false)
    private String[] triggerKeywords;

    @Column(nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
