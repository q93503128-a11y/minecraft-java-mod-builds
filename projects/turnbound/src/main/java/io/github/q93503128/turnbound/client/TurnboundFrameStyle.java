package io.github.q93503128.turnbound.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Compact framed surface language derived from the information density and nested-frame hierarchy
 * seen in BetterQuesting/REI. No external pixels or code are copied; this is a TURNBOUND renderer.
 */
final class TurnboundFrameStyle {
    static final int OUTER = 0xF00A0D12;
    static final int BORDER = 0xFF4B5668;
    static final int INNER = 0xEC151A22;
    static final int INSET = 0xE810141B;
    static final int TEXT = 0xFFF4F0E6;
    static final int MUTED = 0xFFAEB7C6;

    private TurnboundFrameStyle() {}

    static void frame(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int accent) {
        int right = x + width;
        int bottom = y + height;
        graphics.fill(x, y, right, bottom, OUTER);
        graphics.fill(x + 1, y + 1, right - 1, bottom - 1, BORDER);
        graphics.fill(x + 2, y + 2, right - 2, bottom - 2, INNER);
        graphics.fill(x + 2, y + 2, x + 5, bottom - 2, accent);
        // Small clipped corners keep the frame from reading as a generic full rectangle.
        graphics.fill(x, y, x + 3, y + 1, 0x00000000);
        graphics.fill(right - 3, bottom - 1, right, bottom, 0x00000000);
    }

    static void inset(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        int right = x + width;
        int bottom = y + height;
        graphics.fill(x, y, right, bottom, 0xD02B3340);
        graphics.fill(x + 1, y + 1, right - 1, bottom - 1, INSET);
    }

    static void divider(GuiGraphicsExtractor graphics, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 1, 0xC04B5668);
    }
}
