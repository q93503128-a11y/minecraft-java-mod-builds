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
    private final boolean selected;

    FoozlePortraitButton(int x, int y, int width, int height,
                         String combatantId, String name, String detail,
                         boolean unavailable, OnPress onPress) {
        this(x, y, width, height, combatantId, name, detail, unavailable, false, onPress);
    }

    FoozlePortraitButton(int x, int y, int width, int height,
                         String combatantId, String name, String detail,
                         boolean unavailable, boolean selected, OnPress onPress) {
        super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        this.combatantId = combatantId == null ? "" : combatantId;
        this.name = name == null ? "" : name;
        this.detail = detail == null ? "" : detail;
        this.unavailable = unavailable;
        this.selected = selected;
    }

    @Override
    protected void extractContents(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        var font = Minecraft.getInstance().font;
        boolean compact = getHeight() < 58;

        int orb = compact
                ? Math.max(24, Math.min(getHeight() - 4, 36))
                : Math.min(getWidth() - 4, Math.max(42, getHeight() - 24));
        int ox = compact ? getX() + 2 : getX() + (getWidth() - orb) / 2;
        int oy = compact ? getY() + (getHeight() - orb) / 2 : getY();

        TurnboundUiSkin.orbBase(graphics, ox, oy, orb);
        int inset = Math.max(5, orb / 7);
        boolean rendered = compact
                ? TurnboundPortraitRenderer.extract(
                        graphics, combatantId,
                        ox + inset, oy + inset,
                        ox + orb - inset, oy + orb - inset,
                        unavailable)
                : TurnboundPortraitRenderer.extractBust(
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
        TurnboundUiSkin.orbOverlay(graphics, ox, oy, orb, active, isHoveredOrFocused(), selected);

        if (compact) {
            int tx = ox + orb + 5;
            int tw = Math.max(12, getX() + getWidth() - 4 - tx);
            String fitted = UiTextLayout.fit(name, tw);
            graphics.text(font, Component.literal(fitted), tx, getY() + 6,
                    unavailable ? TurnboundUiTokens.TEXT_MUTED : TurnboundUiTokens.TEXT_PRIMARY, true);
            if (!detail.isBlank()) {
                String detailFit = UiTextLayout.fit(detail, tw);
                graphics.text(font, Component.literal(detailFit), tx, getY() + 20,
                        TurnboundUiTokens.TEXT_SECONDARY, false);
            }
            return;
        }

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
