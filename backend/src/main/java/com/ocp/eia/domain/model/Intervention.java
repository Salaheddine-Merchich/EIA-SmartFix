package com.ocp.eia.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "interventions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Intervention {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "failure_id", nullable = false)
    private Failure failure;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "technicien_id", nullable = false)
    private User technicien;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String symptomes;

    @Column(name = "cause_racine", columnDefinition = "TEXT")
    private String causeRacine;

    @Column(name = "analyse_technique", columnDefinition = "TEXT")
    private String analyseTechnique;

    @Column(name = "actions_correctives", columnDefinition = "TEXT")
    private String actionsCorrectives;

    @Column(name = "pieces_remplacees", columnDefinition = "TEXT")
    private String piecesRemplacees;

    @Column(name = "duree_arret_minutes")
    private Integer dureeArretMinutes;

    @Column(name = "temps_intervention_minutes")
    private Integer tempsInterventionMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_validation", nullable = false)
    @Builder.Default
    private StatutValidation statutValidation = StatutValidation.BROUILLON;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validateur_id")
    private User validateur;

    @Column(name = "date_validation")
    private Instant dateValidation;

    @Column(name = "commentaire_validation", columnDefinition = "TEXT")
    private String commentaireValidation;

    @OneToMany(mappedBy = "intervention", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 32)
    @Builder.Default
    private List<InterventionDocument> documents = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
