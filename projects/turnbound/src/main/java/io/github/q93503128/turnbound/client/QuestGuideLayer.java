package io.github.q93503128.turnbound.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.gui.GuiLayer;
import org.jetbrains.annotations.NotNull;

/** Persistent, non-modal current-objective guide. It intentionally shows only the next useful action. */
public final class QuestGuideLayer implements GuiLayer {
    private static final int TEXT = 0xFFF6F0E4;
    private static final int MUTED = 0xFFC9BDAA;
    private static final int GOLD = 0xFFFFC857;
    private static final int GREEN = 0xFF80D49A;

    @Override
    public void render(@NotNull GuiGraphicsExtractor graphics, DeltaTracker tracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.gui.screen() != null) return;
        if (ClientBattleState.snapshot().active()) return;
        var snapshot = ClientFieldState.snapshot();
        if (!snapshot.active() || snapshot.mode() == io.github.q93503128.turnbound.world.FieldUiSnapshot.Mode.LOADING) return;
        if (snapshot.objective().isBlank()) return;

        int width = Math.min(286, Math.max(210, graphics.guiWidth() / 4));
        int x = graphics.guiWidth() - width - 12;
        int y = 12;
        int height = snapshot.dialogue().isBlank() ? 59 : 76;
        TurnboundUiSkin.panel(graphics, x, y, width, height);
        graphics.text(minecraft.font, Component.literal("현재 목표"), x + 16, y + 13, GOLD, true);
        graphics.text(minecraft.font, Component.literal(fit(minecraft, snapshot.objective(), width - 32)), x + 16, y + 31, TEXT, true);

        if (!snapshot.dialogue().isBlank()) {
            graphics.text(minecraft.font, Component.literal(fit(minecraft, snapshot.dialogue(), width - 32)), x + 16, y + 47, MUTED, false);
        }
        if (snapshot.patrolGoal() > 0 && snapshot.patrolsCleared() < snapshot.patrolGoal()) {
            String progress = snapshot.patrolsCleared() + " / " + snapshot.patrolGoal();
            int tw = minecraft.font.width(progress);
            graphics.text(minecraft.font, Component.literal(progress), x + width - tw - 16, y + 13, GREEN, true);
        }
    }

    private static String fit(Minecraft minecraft, String text, int maxWidth) {
        if (minecraft.font.width(text) <= maxWidth) return text;
        String suffix = "…";
        int end = text.length();
        while (end > 1 && minecraft.font.width(text.substring(0, end) + suffix) > maxWidth) end--;
        return text.substring(0, Math.max(1, end)) + suffix;
    }
}
