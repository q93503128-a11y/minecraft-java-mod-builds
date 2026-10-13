package io.github.q93503128.turnbound.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/** Draw-only compositor for generated summon textures. It never creates or renders a live entity. */
final class SummonLayerRenderer {
    private SummonLayerRenderer() {}

    static void sprite(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int width, int height) {
        if (graphics == null || texture == null || width <= 0 || height <= 0) return;
        graphics.blit(texture, x, y, x + width, y + height, 0.0F, 1.0F, 0.0F, 1.0F);
    }

    static void fullScreen(GuiGraphicsExtractor graphics, Identifier texture, int width, int height) {
        sprite(graphics, texture, 0, 0, width, height);
    }

    static void centered(GuiGraphicsExtractor graphics, Identifier texture, int centerX, int centerY, int width, int height) {
        sprite(graphics, texture, centerX - width / 2, centerY - height / 2, width, height);
    }

    static void portrait(GuiGraphicsExtractor graphics, String characterId, boolean silhouette,
                         int x, int y, int width, int height) {
        SummonPortraitSpec.Spec spec = SummonPortraitSpec.forCharacter(characterId);
        int scaledW = Math.max(1, Math.round(width * spec.scale()));
        int scaledH = Math.max(1, Math.round(height * spec.scale()));
        int dx = Math.round(width * spec.xBias());
        int dy = Math.round(height * spec.yBias());
        int px = x + (width - scaledW) / 2 + dx;
        int py = y + (height - scaledH) / 2 + dy;
        sprite(graphics, silhouette ? SummonAssets.silhouette(characterId) : SummonAssets.portrait(characterId),
                px, py, scaledW, scaledH);
    }
}
