package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.world.FieldUiSnapshot;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.gui.GuiLayer;
import org.jetbrains.annotations.NotNull;

/** Close-range physical NPC interaction cue that keeps the free-roam world as the primary screen. */
public final class FieldInteractionPromptLayer implements GuiLayer {
    @Override
    public void render(@NotNull GuiGraphicsExtractor graphics, DeltaTracker tracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.gui.screen() != null) return;
        if (ClientPresentationTransition.fieldPresentationSuppressed()) return;

        FieldUiSnapshot snapshot = ClientFieldState.snapshot();
        if (!snapshot.active() || snapshot.interactionId().isBlank() || snapshot.interactionLabel().isBlank()) return;

        String key = minecraft.options.keyUse.getTranslatedKeyMessage().getString();
        String action = snapshot.interactionAction().isBlank() ? "상호작용" : snapshot.interactionAction();
        String text = key + "  " + snapshot.interactionLabel() + " · " + action;
        int maxWidth = Math.min(300, Math.max(150, graphics.guiWidth() - 28));
        text = UiTextLayout.fit(text, maxWidth - 22);
        int width = Math.min(maxWidth, minecraft.font.width(text) + 22);
        int height = 24;
        int x = (graphics.guiWidth() - width) / 2;
        int y = graphics.guiHeight() - height - 26;

        TurnboundUiSkin.inset(graphics, x, y, width, height);
        int tx = x + (width - minecraft.font.width(text)) / 2;
        graphics.text(minecraft.font, Component.literal(text), tx, y + 8, TurnboundUiTokens.TEXT_PRIMARY, true);
    }
}
