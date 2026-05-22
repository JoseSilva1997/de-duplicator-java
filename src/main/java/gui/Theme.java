package gui;

import java.awt.Color;
import java.awt.Font;

final class Theme {

    static final Color BACKGROUND       = new Color(0xF4, 0xF6, 0xFB);
    static final Color CARD             = Color.WHITE;
    static final Color CARD_BORDER      = new Color(0xE2, 0xE8, 0xF0);
    static final Color CARD_BORDER_HOVER= new Color(0xCB, 0xD5, 0xE1);

    static final Color PRIMARY          = new Color(0x25, 0x63, 0xEB);
    static final Color PRIMARY_HOVER    = new Color(0x1D, 0x4E, 0xD8);
    static final Color PRIMARY_DISABLED = new Color(0xC7, 0xD7, 0xFA);

    static final Color TEXT_PRIMARY     = new Color(0x0F, 0x17, 0x2A);
    static final Color TEXT_SECONDARY   = new Color(0x64, 0x74, 0x8B);
    static final Color TEXT_MUTED       = new Color(0x94, 0xA3, 0xB8);

    static final Color DANGER           = new Color(0xDC, 0x26, 0x26);
    static final Color SUCCESS          = new Color(0x15, 0x9A, 0x45);
    static final Color SUCCESS_BG       = new Color(0xE7, 0xF8, 0xEE);
    static final Color NEUTRAL_BG       = new Color(0xF1, 0xF5, 0xF9);

    static final String FONT_FAMILY     = "Segoe UI";
    static final Font   FONT_REGULAR    = new Font(FONT_FAMILY, Font.PLAIN, 13);
    static final Font   FONT_MEDIUM     = new Font(FONT_FAMILY, Font.PLAIN, 13);
    static final Font   FONT_BOLD       = new Font(FONT_FAMILY, Font.BOLD,  13);
    static final Font   FONT_TITLE      = new Font(FONT_FAMILY, Font.BOLD,  18);
    static final Font   FONT_DISPLAY    = new Font(FONT_FAMILY, Font.BOLD,  28);

    static final int    CARD_ARC        = 14;

    private Theme() {}
}
