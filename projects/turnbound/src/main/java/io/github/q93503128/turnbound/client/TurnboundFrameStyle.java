package io.github.q93503128.turnbound.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Shared TURNBOUND frame primitives. Visible chrome is supplied by the adopted external RPG UI skin. */
final class TurnboundFrameStyle {
    static final int OUTER = TurnboundUiTokens.BACKGROUND;
    static final int BORDER = TurnboundUiTokens.BORDER;
    static final int INNER = TurnboundUiTokens.SURFACE;
    static final int INSET = TurnboundUiTokens.INSET;
    static final int TEXT = TurnboundUiTokens.TEXT_PRIMARY;
    static final int MUTED = TurnboundUiTokens.TEXT_SECONDARY;

    private TurnboundFrameStyle() {}

    static void frame(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int accent) {
        TurnboundUiSkin.panel(graphics, x, y, width, height);
    }

    static void inset(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        TurnboundUiSkin.inset(graphics, x, y, width, height);
    }

    static void divider(GuiGraphicsExtractor graphics, int x, int y, int width) {
        TurnboundUiSkin.inset(graphics, x, y, width, 3);
    }
}
