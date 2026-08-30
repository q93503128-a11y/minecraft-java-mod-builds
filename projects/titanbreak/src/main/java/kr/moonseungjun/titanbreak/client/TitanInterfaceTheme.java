package kr.moonseungjun.titanbreak.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Shared visual language for TITANBREAK machine and augmentation interfaces. */
public final class TitanInterfaceTheme {
    public static final int BACKDROP_TOP = 0xD80B0F13;
    public static final int BACKDROP_BOTTOM = 0xE812191E;
    public static final int PANEL = 0xF0141C22;
    public static final int PANEL_ALT = 0xF01A252D;
    public static final int PANEL_SOFT = 0xB81D2A32;
    public static final int LINE = 0xFF40515C;
    public static final int LINE_SOFT = 0x8840515C;
    public static final int TEXT = 0xFFE6EEF1;
    public static final int TEXT_MUTED = 0xFF8EA1AB;
    public static final int ACCENT = 0xFFE1B45B;
    public static final int CYAN = 0xFF63B4C6;
    public static final int GOOD = 0xFF79C393;
    public static final int BAD = 0xFFD97777;
    public static final int OCCUPIED = 0xFF5A9070;

    private TitanInterfaceTheme() {}

    public static void backdrop(GuiGraphicsExtractor graphics, int width, int height) {
        graphics.fillGradient(0, 0, width, height, BACKDROP_TOP, BACKDROP_BOTTOM);
    }

    public static void shell(GuiGraphicsExtractor graphics, Font font, Component title,
                             int left, int top, int right, int bottom) {
        graphics.fill(left, top, right, bottom, 0xF20C1115);
        graphics.outline(left, top, right - left, bottom - top, LINE);
        graphics.fill(left + 1, top + 1, right - 1, top + 32, PANEL_ALT);
        graphics.fill(left + 1, top + 31, right - 1, top + 33, ACCENT);
        graphics.text(font, title, left + 14, top + 11, TEXT);
        cornerCuts(graphics, left, top, right, bottom, ACCENT);
    }

    public static void panel(GuiGraphicsExtractor graphics, int left, int top, int right, int bottom) {
        graphics.fill(left, top, right, bottom, PANEL);
        graphics.outline(left, top, right - left, bottom - top, LINE_SOFT);
    }

    public static void panelHeader(GuiGraphicsExtractor graphics, Font font, Component text,
                                   int left, int top, int right) {
        graphics.fill(left, top, right, top + 18, PANEL_ALT);
        graphics.fill(left, top + 17, right, top + 18, CYAN);
        graphics.text(font, text, left + 7, top + 5, TEXT_MUTED);
    }

    public static void selectionFrame(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
        graphics.outline(x - 2, y - 2, w + 4, h + 4, color);
        graphics.fill(x - 2, y - 2, x + 7, y, color);
        graphics.fill(x + w - 5, y - 2, x + w + 2, y, color);
        graphics.fill(x - 2, y + h, x + 7, y + h + 2, color);
        graphics.fill(x + w - 5, y + h, x + w + 2, y + h + 2, color);
    }

    public static void meter(GuiGraphicsExtractor graphics, int x, int y, int width,
                             double fraction, int fillColor) {
        fraction = Math.max(0.0D, Math.min(1.0D, fraction));
        graphics.fill(x, y, x + width, y + 5, 0xFF26333A);
        int filled = (int) Math.round(width * fraction);
        if (filled > 0) graphics.fill(x, y, x + filled, y + 5, fillColor);
        graphics.outline(x, y, width, 5, LINE_SOFT);
    }

    public static void connector(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int color) {
        int midX = (x1 + x2) / 2;
        graphics.horizontalLine(Math.min(x1, midX), Math.max(x1, midX), y1, color);
        graphics.verticalLine(midX, Math.min(y1, y2), Math.max(y1, y2), color);
        graphics.horizontalLine(Math.min(midX, x2), Math.max(midX, x2), y2, color);
    }

    private static void cornerCuts(GuiGraphicsExtractor graphics, int left, int top, int right, int bottom, int color) {
        graphics.fill(left, top, left + 12, top + 2, color);
        graphics.fill(left, top, left + 2, top + 12, color);
        graphics.fill(right - 12, top, right, top + 2, color);
        graphics.fill(right - 2, top, right, top + 12, color);
        graphics.fill(left, bottom - 2, left + 12, bottom, color);
        graphics.fill(left, bottom - 12, left + 2, bottom, color);
        graphics.fill(right - 12, bottom - 2, right, bottom, color);
        graphics.fill(right - 2, bottom - 12, right, bottom, color);
    }
}
