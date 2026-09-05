package io.github.q93503128.turnbound.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Shared TURNBOUND frame primitives. Generic chrome uses project design tokens. */
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
        int railTop = y + TurnboundUiTokens.S;
        int railBottom = y + height - TurnboundUiTokens.S;
        if (railBottom > railTop) graphics.fill(x + TurnboundUiTokens.S, railTop, x + TurnboundUiTokens.S + 3, railBottom, accent);
    }

    static void inset(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        TurnboundUiSkin.inset(graphics, x, y, width, height);
    }

    static void divider(GuiGraphicsExtractor graphics, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 1, 0xA88B735B);
    }
}
