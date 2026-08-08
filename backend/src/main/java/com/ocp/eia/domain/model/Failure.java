package com.ocp.eia.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "failures")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Failure {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Column(name = "date_heure", nullable = false)
    private Instant dateHeure;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Criticite criticite;

    @Column(name = "zone_service")
    private String zoneService;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "declarant_id")
    private User declarant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id")
    private User responsable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutPanne statut = StatutPanne.OUVERTE;

    @Column(name = "description_initiale", columnDefinition = "TEXT")
    private String descriptionInitiale;

    @Column(name = "code_defaut", length = 100)
    private String codeDefaut;

    @OneToMany(mappedBy = "failure", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Intervention> interventions = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
