package io.github.q93503128.turnbound.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/** Root-menu action rendered with Foozle's external circular RPG button artwork. */
final class FoozleOrbButton extends Button {
    private final String label;

    FoozleOrbButton(int x, int y, int size, String label, OnPress onPress) {
        super(x, y, size, size, Component.empty(), onPress, DEFAULT_NARRATION);
        this.label = label == null ? "" : label;
    }

    @Override
    protected void extractContents(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int size = Math.min(getWidth(), getHeight());
        TurnboundUiSkin.orbBase(graphics, getX(), getY(), size);
        TurnboundUiSkin.orbOverlay(graphics, getX(), getY(), size, active, isHoveredOrFocused(), false);

        var font = Minecraft.getInstance().font;
        int textWidth = font.width(label);
        int tx = getX() + (size - textWidth) / 2;
        int ty = getY() + (size - font.lineHeight) / 2 + 1;
        graphics.text(font, Component.literal(label), tx, ty,
                active ? TurnboundUiTokens.TEXT_PRIMARY : TurnboundUiTokens.TEXT_MUTED, true);
    }
}
