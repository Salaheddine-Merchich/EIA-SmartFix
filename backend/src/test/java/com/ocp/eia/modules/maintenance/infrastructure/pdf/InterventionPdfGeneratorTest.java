package com.ocp.eia.modules.maintenance.infrastructure.pdf;

import com.ocp.eia.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InterventionPdfGeneratorTest {

    private InterventionPdfGenerator generator;
    private Intervention intervention;
    private Equipment equipment;

    @BeforeEach
    void setUp() {
        generator = new InterventionPdfGenerator();

        equipment = Equipment.builder()
                .id(UUID.randomUUID())
                .code("EQ-001")
                .designation("Pompe centrifuge")
                .famille("Pompage")
                .zone("Zone A")
                .constructeur("Siemens")
                .miseEnService(LocalDate.of(2020, 5, 12))
                .build();

        User technicien = User.builder()
                .id(UUID.randomUUID())
                .email("tech@ocp.ma")
                .nomPrenom("Youssef Alami")
                .role(Role.TECHNICIEN)
                .build();

        User validateur = User.builder()
                .id(UUID.randomUUID())
                .email("admin@ocp.ma")
                .nomPrenom("Admin OCP")
                .role(Role.ADMIN)
                .build();

        User declarant = User.builder()
                .id(UUID.randomUUID())
                .email("decl@ocp.ma")
                .nomPrenom("Mohamed Benali")
                .role(Role.TECHNICIEN)
                .build();

        Failure failure = Failure.builder()
                .id(UUID.randomUUID())
                .equipment(equipment)
                .dateHeure(Instant.parse("2026-08-07T10:30:00Z"))
                .criticite(Criticite.HAUTE)
                .statut(StatutPanne.EN_COURS)
                .zoneService("Phosphorique")
                .codeDefaut("ERR-42")
                .descriptionInitiale("Température élevée détectée")
                .declarant(declarant)
                .build();

        intervention = Intervention.builder()
                .id(UUID.randomUUID())
                .failure(failure)
                .technicien(technicien)
                .validateur(validateur)
                .description("Intervention corrective")
                .symptomes("Machine surchauffe")
                .causeRacine("Joint défectueux")
                .analyseTechnique("Analyse thermique complète")
                .actionsCorrectives("Remplacement du joint")
                .piecesRemplacees("Joint type B")
                .dureeArretMinutes(100)
                .tempsInterventionMinutes(200)
                .statutValidation(StatutValidation.VALIDEE)
                .dateValidation(Instant.parse("2026-08-07T20:16:35Z"))
                .commentaireValidation("Intervention conforme")
                .createdAt(Instant.parse("2026-08-07T12:00:00Z"))
                .build();
    }

    @Test
    void generateInterventionReport_shouldProduceValidPdf() {
        byte[] pdf = generator.generateInterventionReport(intervention, equipment);

        assertThat(pdf).isNotEmpty();
        assertThat(pdf.length).isGreaterThan(500);
        assertThat(new String(pdf, 0, Math.min(pdf.length, 8))).startsWith("%PDF-");
    }

    @Test
    void generateInterventionReport_shouldHandleMissingEquipment() {
        byte[] pdf = generator.generateInterventionReport(intervention, null);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, Math.min(pdf.length, 8))).startsWith("%PDF-");
    }

    @Test
    void generateInterventionReport_shouldHandleDraftValidationStatus() {
        intervention.setStatutValidation(StatutValidation.BROUILLON);
        intervention.setValidateur(null);
        intervention.setDateValidation(null);

        byte[] pdf = generator.generateInterventionReport(intervention, equipment);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, Math.min(pdf.length, 8))).startsWith("%PDF-");
    }

    @Test
    void generateInterventionReport_shouldNotContainDocumentsSection() {
        byte[] pdf = generator.generateInterventionReport(intervention, equipment);
        String pdfContent = new String(pdf);

        assertThat(pdfContent).doesNotContain("DOCUMENTS JOINTS");
        assertThat(pdfContent).doesNotContain("Les documents sont disponibles via l'application web");
    }
}
