package io.github.q93503128.turnbound.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/** Shared TURNBOUND button whose pixel design is supplied by Kenney UI Adventure Pack (CC0). */
final class BattleHudButton extends Button {
    private final int accent;
    private boolean selected;

    BattleHudButton(int x, int y, int width, int height, Component message, int accent, OnPress onPress) {
        super(x, y, width, height, TurnboundUiText.playerFacingLabel(message), onPress, DEFAULT_NARRATION);
        this.accent = accent;
        String raw = message == null ? "" : message.getString();
        boolean hiddenService = ("소환".equals(raw) && !FacilityUiAccess.archive())
                || (("INVENTORY".equals(raw) || "MARKET".equals(raw)) && !FacilityUiAccess.market())
                || (("강화 +1".equals(raw) || "+20 완료".equals(raw)) && !FacilityUiAccess.forge());
        if (hiddenService) {
            visible = false;
            active = false;
        }
    }

    void setSelected(boolean selected) { this.selected = selected; }

    @Override
    public void setMessage(Component message) {
        super.setMessage(TurnboundUiText.playerFacingLabel(message));
    }

    @Override
    protected void extractContents(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        TurnboundUiSkin.button(graphics, getX(), getY(), getWidth(), getHeight(), active,
                isHoveredOrFocused(), selected, accent);
        extractDefaultLabel(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
    }
}
