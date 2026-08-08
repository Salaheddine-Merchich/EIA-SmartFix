package com.ocp.eia.modules.maintenance.infrastructure.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class PdfHeaderFooterHandler extends PdfPageEventHelper {

    private static final DateTimeFormatter GENERATED_AT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final float BANNER_HEIGHT = 28f;

    private final String reportReference;
    private PdfTemplate totalPageTemplate;
    private BaseFont footerBaseFont;

    public PdfHeaderFooterHandler(UUID interventionId) {
        this.reportReference = interventionId.toString().substring(0, 8).toUpperCase();
    }

    @Override
    public void onOpenDocument(PdfWriter writer, Document document) {
        totalPageTemplate = writer.getDirectContent().createTemplate(30, 16);
        try {
            footerBaseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            throw new RuntimeException("Impossible d'initialiser la police PDF", e);
        }
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        float left = document.left();
        float right = document.right();
        float top = document.top();
        float bottom = document.bottom();
        int pageNumber = writer.getPageNumber();

        if (pageNumber == 1) {
            drawFirstPageBanner(writer, document, left, right, top);
        } else {
            ColumnText.showTextAligned(
                    writer.getDirectContent(),
                    Element.ALIGN_LEFT,
                    new Phrase("EIA SmartFix — OCP", PdfReportTheme.HEADER_FONT),
                    left,
                    top + 20,
                    0
            );

            ColumnText.showTextAligned(
                    writer.getDirectContent(),
                    Element.ALIGN_RIGHT,
                    new Phrase("Rapport N° " + reportReference, PdfReportTheme.HEADER_FONT),
                    right,
                    top + 20,
                    0
            );
        }

        ColumnText.showTextAligned(
                writer.getDirectContent(),
                Element.ALIGN_LEFT,
                new Phrase("Document généré automatiquement — EIA SmartFix", PdfReportTheme.FOOTER_FONT),
                left,
                bottom - 20,
                0
        );

        drawPageNumber(writer, right, bottom);
    }

    @Override
    public void onCloseDocument(PdfWriter writer, Document document) {
        totalPageTemplate.beginText();
        totalPageTemplate.setFontAndSize(footerBaseFont, 8);
        totalPageTemplate.setTextMatrix(0, 0);
        totalPageTemplate.showText(String.valueOf(writer.getPageNumber() - 1));
        totalPageTemplate.endText();
    }

    private void drawFirstPageBanner(PdfWriter writer, Document document, float left, float right, float top) {
        PdfContentByte canvas = writer.getDirectContent();
        float width = right - left;
        float bannerBottom = top + 8f;

        canvas.saveState();
        canvas.setColorFill(PdfReportTheme.PRIMARY);
        canvas.rectangle(left, bannerBottom, width, BANNER_HEIGHT);
        canvas.fill();
        canvas.restoreState();

        ColumnText.showTextAligned(
                canvas,
                Element.ALIGN_LEFT,
                new Phrase("EIA SmartFix — OCP", PdfReportTheme.BANNER_FONT),
                left + 10,
                bannerBottom + 16,
                0
        );

        ColumnText.showTextAligned(
                canvas,
                Element.ALIGN_RIGHT,
                new Phrase("Rapport d'intervention", PdfReportTheme.BANNER_SUB_FONT),
                right - 10,
                bannerBottom + 16,
                0
        );

        canvas.saveState();
        canvas.setColorStroke(PdfReportTheme.BORDER);
        canvas.setLineWidth(0.5f);
        canvas.moveTo(left, bannerBottom);
        canvas.lineTo(right, bannerBottom);
        canvas.stroke();
        canvas.restoreState();
    }

    private void drawPageNumber(PdfWriter writer, float right, float bottom) {
        PdfContentByte canvas = writer.getDirectContent();
        String prefix = "Généré le " + LocalDateTime.now().format(GENERATED_AT) + "   Page "
                + writer.getPageNumber() + " / ";
        float prefixWidth = footerBaseFont.getWidthPoint(prefix, 8);

        ColumnText.showTextAligned(
                canvas,
                Element.ALIGN_RIGHT,
                new Phrase(prefix, PdfReportTheme.FOOTER_FONT),
                right,
                bottom - 20,
                0
        );

        canvas.addTemplate(totalPageTemplate, right - prefixWidth + footerBaseFont.getWidthPoint(" / ", 8), bottom - 20);
    }
}
