package io.github.q93503128.turnbound.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Shared map-marker shapes so location type is readable without relying on color alone. */
final class AsterMarchMarkerStyle {
    private static final int SHADOW = 0xE014171B;

    private AsterMarchMarkerStyle() {}

    static int color(AsterMarchMapData.Kind kind) {
        return switch (kind) {
            case FACILITY -> TurnboundUiTokens.PRIMARY;
            case HUNT -> TurnboundUiTokens.SUCCESS;
            case RELAY -> TurnboundUiTokens.ACCENT;
            case BOSS -> TurnboundUiTokens.DANGER;
        };
    }

    static void draw(GuiGraphicsExtractor graphics, int cx, int cy, AsterMarchMapData.Kind kind) {
        int color = color(kind);
        switch (kind) {
            case FACILITY -> {
                graphics.fill(cx - 3, cy - 3, cx + 4, cy + 4, SHADOW);
                graphics.fill(cx - 2, cy - 2, cx + 3, cy + 3, color);
            }
            case HUNT -> {
                graphics.fill(cx - 1, cy - 4, cx + 2, cy + 5, SHADOW);
                graphics.fill(cx - 4, cy - 1, cx + 5, cy + 2, SHADOW);
                graphics.fill(cx, cy - 3, cx + 1, cy + 4, color);
                graphics.fill(cx - 3, cy, cx + 4, cy + 1, color);
            }
            case RELAY -> {
                graphics.fill(cx - 4, cy - 4, cx + 5, cy + 5, SHADOW);
                graphics.fill(cx - 3, cy - 3, cx + 4, cy - 1, color);
                graphics.fill(cx - 3, cy + 2, cx + 4, cy + 4, color);
                graphics.fill(cx - 3, cy - 1, cx - 1, cy + 2, color);
                graphics.fill(cx + 2, cy - 1, cx + 4, cy + 2, color);
            }
            case BOSS -> {
                graphics.fill(cx - 4, cy - 4, cx + 5, cy + 5, SHADOW);
                graphics.fill(cx - 3, cy - 3, cx - 1, cy - 1, color);
                graphics.fill(cx + 2, cy - 3, cx + 4, cy - 1, color);
                graphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, color);
                graphics.fill(cx - 3, cy + 2, cx - 1, cy + 4, color);
                graphics.fill(cx + 2, cy + 2, cx + 4, cy + 4, color);
            }
        }
    }

    static void drawSmall(GuiGraphicsExtractor graphics, int cx, int cy, AsterMarchMapData.Kind kind) {
        int color = color(kind);
        switch (kind) {
            case FACILITY -> {
                graphics.fill(cx - 2, cy - 2, cx + 3, cy + 3, SHADOW);
                graphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, color);
            }
            case HUNT -> {
                graphics.fill(cx, cy - 2, cx + 1, cy + 3, color);
                graphics.fill(cx - 2, cy, cx + 3, cy + 1, color);
            }
            case RELAY -> {
                graphics.fill(cx - 2, cy - 2, cx + 3, cy + 3, color);
                graphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, SHADOW);
            }
            case BOSS -> {
                graphics.fill(cx - 2, cy - 2, cx + 3, cy + 3, SHADOW);
                graphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, color);
                graphics.fill(cx - 2, cy, cx + 3, cy + 1, color);
                graphics.fill(cx, cy - 2, cx + 1, cy + 3, color);
            }
        }
    }
}
