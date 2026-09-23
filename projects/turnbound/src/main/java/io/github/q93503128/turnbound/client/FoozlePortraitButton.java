package io.github.q93503128.turnbound.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Large Foozle-framed live character portrait used by the root RPG menu.
 *
 * <p>The previous narrow row cards forced 3D actors into tiny 28-36px crops. This keeps a proper bust-sized
 * portrait and uses the external ornamental frame as the actual profile surface.</p>
 */
final class FoozlePortraitButton extends Button {
    private final String combatantId;
    private final String name;
    private final String detail;
    private final boolean unavailable;

    FoozlePortraitButton(int x, int y, int width, int height,
                         String combatantId, String name, String detail,
                         boolean unavailable, OnPress onPress) {
        super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        this.combatantId = combatantId == null ? "" : combatantId;
        this.name = name == null ? "" : name;
        this.detail = detail == null ? "" : detail;
        this.unavailable = unavailable;
    }

    @Override
    protected void extractContents(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        var font = Minecraft.getInstance().font;
        int orb = Math.min(getWidth() - 4, Math.max(42, getHeight() - 24));
        int ox = getX() + (getWidth() - orb) / 2;
        int oy = getY();

        TurnboundUiSkin.orbBase(graphics, ox, oy, orb);
        int inset = Math.max(7, orb / 7);
        boolean rendered = TurnboundPortraitRenderer.extractBust(
                graphics, combatantId,
                ox + inset, oy + inset,
                ox + orb - inset, oy + orb - inset,
                unavailable);
        if (!rendered) {
            String initial = name.isBlank() ? "?" : name.substring(0, 1);
            graphics.text(font, Component.literal(initial),
                    ox + (orb - font.width(initial)) / 2,
                    oy + (orb - font.lineHeight) / 2,
                    TurnboundUiTokens.TEXT_PRIMARY, true);
        }
        TurnboundUiSkin.orbOverlay(graphics, ox, oy, orb, active, isHoveredOrFocused(), false);

        int nameY = oy + orb - 1;
        String fitted = UiTextLayout.fit(name, getWidth() - 4);
        graphics.text(font, Component.literal(fitted),
                getX() + (getWidth() - font.width(fitted)) / 2,
                nameY, unavailable ? TurnboundUiTokens.TEXT_MUTED : TurnboundUiTokens.TEXT_PRIMARY, true);
        if (!detail.isBlank() && getHeight() >= orb + 18) {
            String detailFit = UiTextLayout.fit(detail, getWidth() - 4);
            graphics.text(font, Component.literal(detailFit),
                    getX() + (getWidth() - font.width(detailFit)) / 2,
                    nameY + 11, TurnboundUiTokens.TEXT_SECONDARY, false);
        }
    }
}
