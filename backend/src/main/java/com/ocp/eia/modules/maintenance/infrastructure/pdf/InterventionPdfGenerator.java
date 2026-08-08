package com.ocp.eia.modules.maintenance.infrastructure.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.ocp.eia.domain.model.Equipment;
import com.ocp.eia.domain.model.Intervention;
import com.ocp.eia.domain.model.StatutValidation;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class InterventionPdfGenerator {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] generateInterventionReport(Intervention intervention, Equipment equipment) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, PdfReportTheme.MARGIN, PdfReportTheme.MARGIN, 80f, 60f);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            writer.setPageEvent(new PdfHeaderFooterHandler(intervention.getId()));
            document.open();

            addTitle(document);
            addSummary(document, intervention, equipment);
            PdfTableBuilder.addSectionSeparator(document);
            addEquipmentSection(document, equipment);
            PdfTableBuilder.addSectionSeparator(document);
            addFailureSection(document, intervention);
            PdfTableBuilder.addSectionSeparator(document);
            addInterventionSection(document, intervention);
            if (intervention.getStatutValidation() != StatutValidation.BROUILLON) {
                PdfTableBuilder.addSectionSeparator(document);
                addValidationSection(document, intervention);
            }
            document.add(PdfTableBuilder.closingBox());

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du PDF", e);
        }
    }

    private void addTitle(Document document) throws DocumentException {
        Paragraph title = new Paragraph("RAPPORT D'INTERVENTION", PdfReportTheme.TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(6f);
        document.add(title);

        Paragraph subtitle = new Paragraph("Maintenance industrielle — EIA SmartFix", PdfReportTheme.SMALL_FONT);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(4f);
        document.add(subtitle);
    }

    private void addSummary(Document document, Intervention intervention, Equipment equipment) throws DocumentException {
        String equipmentLine = equipment != null
                ? equipment.getCode() + " — " + equipment.getDesignation()
                : null;

        String technicienLine = intervention.getTechnicien() != null
                ? intervention.getTechnicien().getNomPrenom()
                : null;

        String dateLine = intervention.getCreatedAt() != null
                ? intervention.getCreatedAt().atZone(ZoneId.systemDefault()).format(DATE_TIME_FORMATTER)
                : null;

        String statutLine = PdfLabelMapper.statutValidation(intervention.getStatutValidation());

        document.add(PdfTableBuilder.summaryGrid(equipmentLine, technicienLine, dateLine, statutLine));
    }

    private void addEquipmentSection(Document document, Equipment equipment) throws DocumentException {
        document.add(PdfTableBuilder.sectionHeader("1. INFORMATIONS ÉQUIPEMENT"));
        if (equipment == null) {
            document.add(new Paragraph("Aucune information équipement disponible.", PdfReportTheme.NORMAL_FONT));
            return;
        }

        PdfPTable table = PdfTableBuilder.twoColumnTable(100f);
        PdfTableBuilder.addRowOrDash(table, "Code équipement", equipment.getCode());
        PdfTableBuilder.addRowOrDash(table, "Désignation", equipment.getDesignation());
        PdfTableBuilder.addRowOrDash(table, "Famille", equipment.getFamille());
        PdfTableBuilder.addRowOrDash(table, "Zone", equipment.getZone());
        PdfTableBuilder.addRowOrDash(table, "Constructeur", equipment.getConstructeur());
        PdfTableBuilder.addRowOrDash(
                table,
                "Mise en service",
                equipment.getMiseEnService() != null ? equipment.getMiseEnService().format(DATE_FORMATTER) : null
        );
        document.add(table);
    }

    private void addFailureSection(Document document, Intervention intervention) throws DocumentException {
        document.add(PdfTableBuilder.sectionHeader("2. DÉTAILS DE LA PANNE"));
        var failure = intervention.getFailure();

        PdfPTable table = PdfTableBuilder.twoColumnTable(100f);
        PdfTableBuilder.addRowOrDash(
                table,
                "Date/heure de déclaration",
                failure.getDateHeure() != null
                        ? failure.getDateHeure().atZone(ZoneId.systemDefault()).format(DATE_TIME_FORMATTER)
                        : null
        );
        PdfTableBuilder.addRowOrDash(table, "Criticité", PdfLabelMapper.criticite(failure.getCriticite()));
        PdfTableBuilder.addRowOrDash(table, "Statut", PdfLabelMapper.statutPanne(failure.getStatut()));
        PdfTableBuilder.addRowOrDash(table, "Zone de service", failure.getZoneService());
        PdfTableBuilder.addRowOrDash(table, "Code défaut", failure.getCodeDefaut());
        PdfTableBuilder.addRowOrDash(
                table,
                "Déclarant",
                failure.getDeclarant() != null ? failure.getDeclarant().getNomPrenom() : null
        );
        PdfTableBuilder.addRowOrDash(
                table,
                "Responsable",
                failure.getResponsable() != null ? failure.getResponsable().getNomPrenom() : null
        );
        document.add(table);

        PdfTableBuilder.addTextBlock(document, "Description initiale", failure.getDescriptionInitiale());
    }

    private void addInterventionSection(Document document, Intervention intervention) throws DocumentException {
        document.add(PdfTableBuilder.sectionHeader("3. INTERVENTION TECHNIQUE"));

        PdfTableBuilder.addTextBlock(document, "Symptômes observés", intervention.getSymptomes());
        PdfTableBuilder.addTextBlock(document, "Cause racine identifiée", intervention.getCauseRacine());
        PdfTableBuilder.addTextBlock(document, "Analyse technique", intervention.getAnalyseTechnique());
        PdfTableBuilder.addTextBlock(document, "Actions correctives réalisées", intervention.getActionsCorrectives());
        PdfTableBuilder.addTextBlock(document, "Pièces remplacées", intervention.getPiecesRemplacees());
        PdfTableBuilder.addTextBlock(document, "Description détaillée", intervention.getDescription());

        String dureeArret = intervention.getDureeArretMinutes() != null
                ? intervention.getDureeArretMinutes() + " min"
                : "—";
        String tempsIntervention = intervention.getTempsInterventionMinutes() != null
                ? intervention.getTempsInterventionMinutes() + " min"
                : "—";

        document.add(PdfTableBuilder.metricsTable(
                "Durée d'arrêt",
                dureeArret,
                "Temps d'intervention",
                tempsIntervention
        ));
    }

    private void addValidationSection(Document document, Intervention intervention) throws DocumentException {
        document.add(PdfTableBuilder.sectionHeader("4. VALIDATION"));

        PdfPTable table = PdfTableBuilder.twoColumnTable(100f);
        PdfTableBuilder.addRowOrDash(
                table,
                "Statut de validation",
                PdfLabelMapper.statutValidation(intervention.getStatutValidation())
        );
        PdfTableBuilder.addRowOrDash(
                table,
                "Validé par",
                intervention.getValidateur() != null ? intervention.getValidateur().getNomPrenom() : null
        );
        PdfTableBuilder.addRowOrDash(
                table,
                "Date de validation",
                intervention.getDateValidation() != null
                        ? intervention.getDateValidation().atZone(ZoneId.systemDefault()).format(DATE_TIME_FORMATTER)
                        : null
        );
        document.add(table);

        PdfTableBuilder.addTextBlock(document, "Commentaires de validation", intervention.getCommentaireValidation());
    }
}
