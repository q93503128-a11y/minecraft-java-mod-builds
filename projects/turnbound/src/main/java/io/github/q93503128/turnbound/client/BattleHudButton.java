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
        super(x, y, width, height, playerFacingLabel(message), onPress, DEFAULT_NARRATION);
        this.accent = accent;
    }

    void setSelected(boolean selected) { this.selected = selected; }

    @Override
    protected void extractContents(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        TurnboundUiSkin.button(graphics, getX(), getY(), getWidth(), getHeight(), active,
                isHoveredOrFocused() || selected, accent);
        extractDefaultLabel(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
    }

    /** Only exact menu vocabulary is translated here; gameplay strings and canonical names remain untouched. */
    private static Component playerFacingLabel(Component source) {
        if (source == null) return Component.empty();
        return switch (source.getString()) {
            case "PARTY" -> Component.literal("파티");
            case "CHARACTERS" -> Component.literal("캐릭터");
            case "EQUIPMENT" -> Component.literal("장비");
            case "ARCHIVE" -> Component.literal("소환");
            case "QUESTS" -> Component.literal("퀘스트");
            case "CODEX" -> Component.literal("도감");
            case "SYSTEM" -> Component.literal("도전");
            case "Status" -> Component.literal("능력치");
            case "Skills" -> Component.literal("스킬");
            case "Equipment" -> Component.literal("장비");
            case "Awakening" -> Component.literal("각성");
            case "Profile" -> Component.literal("프로필");
            default -> source;
        };
    }
}
