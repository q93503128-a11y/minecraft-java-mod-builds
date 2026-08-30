package io.github.q93503128.turnbound.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/** BetterQuesting/REI-inspired compact framed control; no external pixels are shipped. */
final class BattleHudButton extends Button {
    private static final int BG = 0xE8151A22;
    private static final int BG_HOVER = 0xF0222B38;
    private static final int BG_DISABLED = 0xC00D1118;
    private static final int BORDER = 0xFF4B5668;
    private static final int DISABLED = 0xFF697383;
    private final int accent;
    private boolean selected;

    BattleHudButton(int x, int y, int width, int height, Component message, int accent, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.accent = accent;
    }

    void setSelected(boolean selected) { this.selected = selected; }

    @Override
    protected void extractContents(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        int background = !active ? BG_DISABLED : isHoveredOrFocused() || selected ? BG_HOVER : BG;
        int edge = active ? accent : DISABLED;
        graphics.fill(x, y, x + w, y + h, 0xF0070A0F);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, BORDER);
        graphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, background);
        graphics.fill(x + 2, y + 2, x + 4, y + h - 2, edge);
        if (selected) graphics.fill(x + 4, y + h - 3, x + w - 2, y + h - 2, edge);
        extractDefaultLabel(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
    }
}
