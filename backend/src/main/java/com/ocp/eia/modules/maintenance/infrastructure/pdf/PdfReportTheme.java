package com.ocp.eia.modules.maintenance.infrastructure.pdf;

import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;

import java.awt.Color;

public final class PdfReportTheme {

    public static final Color PRIMARY = new Color(0, 98, 51);
    public static final Color SECONDARY = new Color(30, 41, 59);
    public static final Color HEADER_BG = new Color(240, 253, 244);
    public static final Color SECTION_BG = new Color(248, 250, 252);
    public static final Color BORDER = new Color(203, 213, 225);
    public static final Color LABEL_TEXT = new Color(100, 116, 139);
    public static final Color WHITE = Color.WHITE;
    public static final Color BLACK = Color.BLACK;

    public static final float MARGIN = 40f;
    public static final float SECTION_SPACING = 16f;

    public static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, PRIMARY);
    public static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, SECONDARY);
    public static final Font SECTION_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, PRIMARY);
    public static final Font LABEL_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, LABEL_TEXT);
    public static final Font NORMAL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, BLACK);
    public static final Font SMALL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8, LABEL_TEXT);
    public static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, PRIMARY);
    public static final Font FOOTER_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8, LABEL_TEXT);
    public static final Font BANNER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, WHITE);
    public static final Font BANNER_SUB_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9, WHITE);

    private PdfReportTheme() {
    }
}
