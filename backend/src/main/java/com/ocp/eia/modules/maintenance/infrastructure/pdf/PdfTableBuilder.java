package com.ocp.eia.modules.maintenance.infrastructure.pdf;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

public final class PdfTableBuilder {

    private PdfTableBuilder() {
    }

    public static PdfPTable twoColumnTable(float widthPercentage) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{35f, 65f});
        table.setWidthPercentage(widthPercentage);
        table.setSpacingBefore(6f);
        table.setSpacingAfter(10f);
        return table;
    }

    public static void addRow(PdfPTable table, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        table.addCell(labelCell(label));
        table.addCell(valueCell(value));
    }

    public static void addRowOrDash(PdfPTable table, String label, String value) {
        table.addCell(labelCell(label));
        table.addCell(valueCell(value));
    }

    public static PdfPTable metricsTable(String leftLabel, String leftValue, String rightLabel, String rightValue)
            throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100f);
        table.setSpacingBefore(8f);
        table.setSpacingAfter(8f);

        table.addCell(metricCell(leftLabel, leftValue));
        table.addCell(metricCell(rightLabel, rightValue));
        return table;
    }

    public static PdfPTable summaryGrid(
            String equipment,
            String technicien,
            String date,
            String statut
    ) throws DocumentException {
        PdfPTable outer = new PdfPTable(1);
        outer.setWidthPercentage(100f);
        outer.setSpacingBefore(12f);
        outer.setSpacingAfter(16f);

        PdfPCell outerCell = new PdfPCell();
        outerCell.setBackgroundColor(PdfReportTheme.HEADER_BG);
        outerCell.setBorderColor(PdfReportTheme.PRIMARY);
        outerCell.setBorderWidth(1f);
        outerCell.setPadding(12f);

        Paragraph title = new Paragraph("Synthèse", PdfReportTheme.SECTION_FONT);
        title.setSpacingAfter(8f);
        outerCell.addElement(title);

        PdfPTable grid = new PdfPTable(2);
        grid.setWidthPercentage(100f);
        grid.setSpacingBefore(4f);

        grid.addCell(summaryCell("Équipement", equipment));
        grid.addCell(summaryCell("Technicien", technicien));
        grid.addCell(summaryCell("Date d'intervention", date));
        grid.addCell(summaryCell("Statut validation", statut));

        outerCell.addElement(grid);
        outer.addCell(outerCell);
        return outer;
    }

    public static PdfPTable sectionHeader(String title) throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100f);
        table.setSpacingBefore(PdfReportTheme.SECTION_SPACING);
        table.setSpacingAfter(8f);

        PdfPCell cell = new PdfPCell(new Phrase(title, PdfReportTheme.SECTION_FONT));
        cell.setBackgroundColor(PdfReportTheme.HEADER_BG);
        cell.setBorderColor(PdfReportTheme.PRIMARY);
        cell.setBorderWidthLeft(4f);
        cell.setBorderWidthTop(0f);
        cell.setBorderWidthRight(0f);
        cell.setBorderWidthBottom(0f);
        cell.setPaddingTop(6f);
        cell.setPaddingBottom(6f);
        cell.setPaddingLeft(10f);
        table.addCell(cell);
        return table;
    }

    public static void addSectionSeparator(Document document) throws DocumentException {
        Paragraph separator = new Paragraph();
        separator.add(new Chunk(new com.lowagie.text.pdf.draw.LineSeparator(
                0.5f, 100f, PdfReportTheme.BORDER, Element.ALIGN_CENTER, -2f
        )));
        separator.setSpacingBefore(6f);
        separator.setSpacingAfter(10f);
        document.add(separator);
    }

    public static PdfPTable closingBox() throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100f);
        table.setSpacingBefore(16f);
        table.setSpacingAfter(8f);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(PdfReportTheme.SECTION_BG);
        cell.setBorderColor(PdfReportTheme.BORDER);
        cell.setPadding(12f);

        Paragraph line1 = new Paragraph("Document établi par EIA SmartFix — OCP", PdfReportTheme.LABEL_FONT);
        line1.setSpacingAfter(4f);
        Paragraph line2 = new Paragraph(
                "Ce rapport constitue la trace officielle de l'intervention.",
                PdfReportTheme.SMALL_FONT
        );
        cell.addElement(line1);
        cell.addElement(line2);
        table.addCell(cell);
        return table;
    }

    public static void addTextBlock(Document document, String label, String value) throws DocumentException {
        if (value == null || value.isBlank()) {
            return;
        }

        document.add(new Paragraph(label, PdfReportTheme.LABEL_FONT));

        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100f);
        table.setSpacingBefore(4f);
        table.setSpacingAfter(10f);

        PdfPCell cell = new PdfPCell(new Paragraph(value, PdfReportTheme.NORMAL_FONT));
        cell.setBackgroundColor(PdfReportTheme.SECTION_BG);
        cell.setBorderColor(PdfReportTheme.BORDER);
        cell.setPadding(10f);
        table.addCell(cell);
        document.add(table);
    }

    private static PdfPCell summaryCell(String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(PdfReportTheme.WHITE);
        cell.setBorderColor(PdfReportTheme.BORDER);
        cell.setPadding(10f);

        Paragraph labelParagraph = new Paragraph(label, PdfReportTheme.LABEL_FONT);
        labelParagraph.setSpacingAfter(4f);
        Paragraph valueParagraph = new Paragraph(PdfLabelMapper.orDash(value), PdfReportTheme.NORMAL_FONT);

        cell.addElement(labelParagraph);
        cell.addElement(valueParagraph);
        return cell;
    }

    private static PdfPCell labelCell(String label) {
        PdfPCell cell = new PdfPCell(new Phrase(label, PdfReportTheme.LABEL_FONT));
        cell.setBackgroundColor(PdfReportTheme.SECTION_BG);
        cell.setBorderColor(PdfReportTheme.BORDER);
        cell.setPadding(8f);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private static PdfPCell valueCell(String value) {
        PdfPCell cell = new PdfPCell(new Phrase(PdfLabelMapper.orDash(value), PdfReportTheme.NORMAL_FONT));
        cell.setBorderColor(PdfReportTheme.BORDER);
        cell.setPadding(8f);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private static PdfPCell metricCell(String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(PdfReportTheme.SECTION_BG);
        cell.setBorderColor(PdfReportTheme.BORDER);
        cell.setPadding(10f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph title = new Paragraph(label, PdfReportTheme.LABEL_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        Paragraph content = new Paragraph(PdfLabelMapper.orDash(value), PdfReportTheme.SUBTITLE_FONT);
        content.setAlignment(Element.ALIGN_CENTER);

        cell.addElement(title);
        cell.addElement(content);
        return cell;
    }
}
