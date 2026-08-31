package kr.moonseungjun.titanbreak.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Shared hard-surface interface language for TITANBREAK.
 * The palette intentionally avoids generic blue-card dashboard styling: neutral black metal carries
 * the surface, data cyan is informational, hazard yellow is interactive, and signal red is destructive.
 */
public final class TitanInterfaceTheme {
    public static final int BACKDROP_TOP = 0xE706090B;
    public static final int BACKDROP_BOTTOM = 0xF20B0E11;
    public static final int PANEL = 0xF1080C0F;
    public static final int PANEL_ALT = 0xF20F1519;
    public static final int PANEL_SOFT = 0xD9141B20;
    public static final int LINE = 0xFF35434A;
    public static final int LINE_SOFT = 0x88404B50;
    public static final int TEXT = 0xFFE8ECEB;
    public static final int TEXT_MUTED = 0xFF8B989A;
    public static final int ACCENT = 0xFFF0D23D;
    public static final int CYAN = 0xFF43D7E8;
    public static final int GOOD = 0xFF6AD7A2;
    public static final int BAD = 0xFFE25B65;
    public static final int OCCUPIED = 0xFF58B88C;
    public static final int SIGNAL_RED = 0xFFE94A57;
    public static final int VIOLET = 0xFFB46EEA;
    public static final int ORANGE = 0xFFF08A46;
    public static final int BLUE = 0xFF5BA7F0;

    private TitanInterfaceTheme() {}

    public static void backdrop(GuiGraphicsExtractor graphics, int width, int height) {
        graphics.fillGradient(0, 0, width, height, BACKDROP_TOP, BACKDROP_BOTTOM);
        int grid = 24;
        for (int x = 0; x < width; x += grid) {
            graphics.verticalLine(x, 0, height, 0x1200E7FF);
        }
        for (int y = 0; y < height; y += grid) {
            graphics.horizontalLine(0, width, y, 0x1000E7FF);
        }
        for (int y = 6; y < height; y += 18) {
            graphics.horizontalLine(0, width, y, 0x08000000);
        }
    }

    public static void shell(GuiGraphicsExtractor graphics, Font font, Component title,
                             int left, int top, int right, int bottom) {
        cutPanel(graphics, left, top, right, bottom, 0xF406090B, LINE, 7);
        graphics.fill(left + 7, top + 1, right - 1, top + 29, PANEL_ALT);
        graphics.fill(left + 7, top + 29, right - 1, top + 31, ACCENT);
        graphics.fill(left + 10, top + 8, left + 13, top + 22, CYAN);
        graphics.text(font, title, left + 19, top + 10, TEXT, false);
        graphics.fill(right - 96, top + 8, right - 14, top + 9, 0x6643D7E8);
        graphics.fill(right - 62, top + 13, right - 14, top + 14, 0x55F0D23D);
        graphics.fill(right - 35, top + 18, right - 14, top + 19, 0x55E94A57);
        cornerBrackets(graphics, left, top, right, bottom, ACCENT);
    }

    public static void panel(GuiGraphicsExtractor graphics, int left, int top, int right, int bottom) {
        cutPanel(graphics, left, top, right, bottom, PANEL, LINE_SOFT, 5);
    }

    public static void panelHeader(GuiGraphicsExtractor graphics, Font font, Component text,
                                   int left, int top, int right) {
        graphics.fill(left + 5, top + 1, right - 1, top + 18, PANEL_ALT);
        graphics.fill(left + 5, top + 17, right - 1, top + 18, CYAN);
        graphics.fill(left + 7, top + 5, left + 9, top + 14, ACCENT);
        graphics.text(font, text, left + 13, top + 5, TEXT_MUTED, false);
    }

    public static void selectionFrame(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
        graphics.horizontalLine(x - 2, x + 7, y - 2, color);
        graphics.verticalLine(x - 2, y - 2, y + 5, color);
        graphics.horizontalLine(x + w - 7, x + w + 2, y - 2, color);
        graphics.verticalLine(x + w + 2, y - 2, y + 5, color);
        graphics.horizontalLine(x - 2, x + 7, y + h + 2, color);
        graphics.verticalLine(x - 2, y + h - 5, y + h + 2, color);
        graphics.horizontalLine(x + w - 7, x + w + 2, y + h + 2, color);
        graphics.verticalLine(x + w + 2, y + h - 5, y + h + 2, color);
    }

    public static void meter(GuiGraphicsExtractor graphics, int x, int y, int width,
                             double fraction, int fillColor) {
        fraction = Math.max(0.0D, Math.min(1.0D, fraction));
        graphics.fill(x, y, x + width, y + 4, 0xFF151B1F);
        int filled = (int) Math.round(width * fraction);
        if (filled > 0) graphics.fill(x, y, x + filled, y + 4, fillColor);
        graphics.horizontalLine(x, x + width, y + 5, LINE_SOFT);
    }

    public static void connector(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int color) {
        int midX = (x1 + x2) / 2;
        graphics.horizontalLine(Math.min(x1, midX), Math.max(x1, midX), y1, color);
        graphics.verticalLine(midX, Math.min(y1, y2), Math.max(y1, y2), color);
        graphics.horizontalLine(Math.min(midX, x2), Math.max(midX, x2), y2, color);
    }

    public static int wrapped(GuiGraphicsExtractor graphics, Font font, Component text,
                              int x, int y, int width, int color, int maxLines) {
        if (width <= 4 || maxLines <= 0) return y;
        List<FormattedCharSequence> lines = font.split(text, width);
        int count = Math.min(maxLines, lines.size());
        for (int i = 0; i < count; i++) {
            graphics.text(font, lines.get(i), x, y + i * (font.lineHeight + 2), color, false);
        }
        return y + count * (font.lineHeight + 2);
    }

    public static int regionColor(kr.moonseungjun.titanbreak.augmentation.AugmentationCatalog.Region region) {
        return switch (region) {
            case EYE -> CYAN;
            case BRAIN -> VIOLET;
            case NERVES -> ORANGE;
            case SPINE -> ACCENT;
            case HEART -> SIGNAL_RED;
            case SKELETON -> 0xFFC8D3D6;
            case SKIN -> 0xFF47B8A8;
            case LEFT_ARM, RIGHT_ARM -> 0xFFFF6A51;
            case LEFT_LEG, RIGHT_LEG -> BLUE;
            case AUX_ORGAN -> 0xFFCF78D9;
        };
    }

    public static void cyberButton(GuiGraphicsExtractor graphics, Font font, Component message,
                                   int left, int top, int width, int height,
                                   boolean active, boolean focused) {
        int right = left + width;
        int bottom = top + height;
        int edge = active ? (focused ? ACCENT : CYAN) : 0xFF4C5457;
        int fill = active ? (focused ? 0xF21B1D17 : 0xED0D1417) : 0xD80B0E10;
        cutPanel(graphics, left, top, right, bottom, fill, edge, 4);
        graphics.fill(left + 5, top + 3, left + 7, bottom - 3, edge);
        if (focused && active) graphics.fill(right - 18, top + 3, right - 5, top + 4, ACCENT);
        int color = active ? TEXT : 0xFF687174;
        graphics.centeredText(font, message, left + width / 2, top + Math.max(3, (height - font.lineHeight) / 2), color);
    }

    private static void cutPanel(GuiGraphicsExtractor graphics, int left, int top, int right, int bottom,
                                 int fill, int edge, int cut) {
        graphics.fill(left + cut, top, right, bottom, fill);
        graphics.fill(left, top + cut, right, bottom - cut, fill);
        graphics.horizontalLine(left + cut, right - 1, top, edge);
        graphics.horizontalLine(left, right - cut - 1, bottom - 1, edge);
        graphics.verticalLine(left, top + cut, bottom - cut - 1, edge);
        graphics.verticalLine(right - 1, top, bottom - 1, edge);
        graphics.horizontalLine(left, left + cut, top + cut, edge);
        graphics.horizontalLine(right - cut, right, bottom - cut - 1, edge);
    }

    private static void cornerBrackets(GuiGraphicsExtractor graphics, int left, int top, int right, int bottom, int color) {
        graphics.fill(left, top, left + 13, top + 2, color);
        graphics.fill(left, top, left + 2, top + 13, color);
        graphics.fill(right - 13, top, right, top + 2, color);
        graphics.fill(right - 2, top, right, top + 13, color);
        graphics.fill(left, bottom - 2, left + 13, bottom, color);
        graphics.fill(left, bottom - 13, left + 2, bottom, color);
        graphics.fill(right - 13, bottom - 2, right, bottom, color);
        graphics.fill(right - 2, bottom - 13, right, bottom, color);
    }
}
