package io.github.q93503128.turnbound.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Kenney-backed roster card that owns its portrait and text render order.
 *
 * <p>Rendering content inside the widget prevents the button skin from painting over the live 3D portrait,
 * which was the cause of the blank grey party cards seen during the first 26.2 client survey.</p>
 */
final class PortraitCardButton extends Button {
    private final String combatantId;
    private final String title;
    private final String detail;
    private final int accent;
    private final boolean selected;
    private final boolean unavailable;

    PortraitCardButton(
            int x, int y, int width, int height,
            String combatantId, String title, String detail,
            int accent, boolean selected, boolean unavailable,
            OnPress onPress
    ) {
        super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        this.combatantId = combatantId == null ? "" : combatantId;
        this.title = title == null ? "" : title;
        this.detail = detail == null ? "" : detail;
        this.accent = accent;
        this.selected = selected;
        this.unavailable = unavailable;
    }

    @Override
    protected void extractContents(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        TurnboundUiSkin.button(graphics, getX(), getY(), getWidth(), getHeight(),
                active, isHoveredOrFocused(), selected, accent);

        var font = Minecraft.getInstance().font;
        int portrait = Math.max(18, Math.min(getHeight() - 4, getWidth() >= 120 ? 36 : 28));
        int px0 = getX() + 3;
        int py0 = getY() + Math.max(2, (getHeight() - portrait) / 2);
        boolean rendered = TurnboundPortraitRenderer.extract(
                graphics, combatantId, px0, py0, px0 + portrait, py0 + portrait, unavailable);
        if (!rendered) {
            TurnboundUiSkin.inset(graphics, px0, py0, portrait, portrait);
            String initial = title.isBlank() ? "?" : title.substring(0, 1);
            graphics.text(font, Component.literal(initial),
                    px0 + Math.max(3, (portrait - font.width(initial)) / 2),
                    py0 + Math.max(2, (portrait - font.lineHeight) / 2),
                    unavailable ? TurnboundUiTokens.TEXT_MUTED : TurnboundUiTokens.TEXT_PRIMARY, true);
        }

        int tx = px0 + portrait + 5;
        int tw = Math.max(8, getX() + getWidth() - 5 - tx);
        int titleY = getY() + (getHeight() >= 34 ? 6 : 4);
        graphics.text(font, Component.literal(UiTextLayout.fit(title, tw)), tx, titleY,
                unavailable ? TurnboundUiTokens.TEXT_MUTED : TurnboundUiTokens.TEXT_PRIMARY, true);
        if (!detail.isBlank() && getHeight() >= 30) {
            graphics.text(font, Component.literal(UiTextLayout.fit(detail, tw)), tx, titleY + 14,
                    TurnboundUiTokens.TEXT_SECONDARY, false);
        }
    }
}
