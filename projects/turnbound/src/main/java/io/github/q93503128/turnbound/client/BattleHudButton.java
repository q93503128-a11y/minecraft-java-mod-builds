package io.github.q93503128.turnbound.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/** Small, flat contextual control for TURNBOUND battle HUD. */
final class BattleHudButton extends Button {
    private static final int BG = 0xB8171C26;
    private static final int BG_HOVER = 0xE0222A38;
    private static final int BG_DISABLED = 0x8010131A;
    private static final int DISABLED = 0xFF687181;
    private final int accent;

    BattleHudButton(int x, int y, int width, int height, Component message, int accent, OnPress onPress) {
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
        int edge = active ? accent : DISABLED;
        graphics.fill(x, y, x + w, y + h, background);
        graphics.fill(x, y, x + 2, y + h, edge);
        if (active && isHoveredOrFocused()) graphics.fill(x + 2, y + h - 1, x + w, y + h, edge);
        extractDefaultLabel(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
    }
}
