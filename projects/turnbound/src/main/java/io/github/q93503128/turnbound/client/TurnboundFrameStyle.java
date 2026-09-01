package io.github.q93503128.turnbound.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Shared layout helpers; visual frame pixels come from the Kenney UI Adventure CC0 skin. */
final class TurnboundFrameStyle {
    static final int OUTER = 0xF00A0D12;
    static final int BORDER = 0xFF8B694A;
    static final int INNER = 0xEC151A22;
    static final int INSET = 0xE810141B;
    static final int TEXT = 0xFFF4F0E6;
    static final int MUTED = 0xFFC7C0B1;

    private TurnboundFrameStyle() {}

    static void frame(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int accent) {
        TurnboundUiSkin.panel(graphics, x, y, width, height);
        graphics.fill(x + 8, y + 8, x + 11, y + height - 8, accent);
    }

    static void inset(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        TurnboundUiSkin.inset(graphics, x, y, width, height);
    }

    static void divider(GuiGraphicsExtractor graphics, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 1, 0xB0998066);
    }
}
