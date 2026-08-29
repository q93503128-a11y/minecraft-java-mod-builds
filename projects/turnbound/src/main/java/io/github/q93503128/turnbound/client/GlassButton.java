package io.github.q93503128.turnbound.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Compact battle control used by the world-first HUD.
 * It deliberately avoids the stretched vanilla/Kenney framed-button look that dominated alpha.4.
 */
final class GlassButton extends Button {
    private static final int BG = 0xD9171C26;
    private static final int BG_HOVER = 0xE8222A38;
    private static final int BG_DISABLED = 0x9910131A;
    private static final int DISABLED_ACCENT = 0xFF7F8796;

    private final int accent;

    GlassButton(int x, int y, int width, int height, Component message, int accent, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.accent = accent;
    }

    @Override
    protected void extractContents(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        int background = !active ? BG_DISABLED : isHoveredOrFocused() ? BG_HOVER : BG;
        int edge = active ? accent : DISABLED_ACCENT;

        graphics.fill(x, y, x + w, y + h, background);
        graphics.fill(x, y, x + 2, y + h, edge);
        if (isHoveredOrFocused() && active) {
            graphics.fill(x + 2, y + h - 1, x + w, y + h, edge);
        }
        extractDefaultLabel(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
    }
}
