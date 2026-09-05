package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.world.FieldUiSnapshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/** Non-dismissible authored-world loading cover, shown before the first field snapshot arrives. */
public final class WorldLoadingScreen extends Screen {
    private static final int BACKGROUND = 0xF410131A;
    private static final int PANEL = 0xE8171C26;
    private static final int PANEL_LIGHT = 0xFF222A38;
    private static final int TEXT = 0xFFF4F0E6;
    private static final int SECONDARY = 0xFFAEB7C6;
    private static final int GAUGE = 0xFF6DC6FF;

    public WorldLoadingScreen() { super(Component.literal("TURNBOUND World Loading")); }

    @Override
    public void tick() {
        super.tick();
        // The old implementation closed itself on the first client tick because the server snapshot had not arrived yet.
        // Keep the cover up until networking has positively told us what the field state is.
        if (!ClientFieldState.initialSnapshotReceived()) return;
        FieldUiSnapshot snapshot = ClientFieldState.snapshot();
        if (!snapshot.active() || snapshot.mode() != FieldUiSnapshot.Mode.LOADING) {
            if (minecraft != null && minecraft.gui.screen() == this) minecraft.gui.setScreen(null);
        }
    }

    @Override public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) { }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        FieldUiSnapshot snapshot = ClientFieldState.snapshot();
        boolean waitingForSnapshot = !ClientFieldState.initialSnapshotReceived();
        graphics.fill(0, 0, width, height, BACKGROUND);

        int w = Math.min(360, Math.max(240, width - 48));
        int h = 112;
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        graphics.fill(x, y, x + w, y + h, PANEL);
        graphics.fill(x, y, x + 3, y + h, GAUGE);

        graphics.text(font, Component.literal("TURNBOUND"), x + 16, y + 14, TEXT, true);
        graphics.text(font, Component.literal("ASTER MARCH · 월드 준비"), x + 16, y + 31, SECONDARY, true);
        String stage = waitingForSnapshot ? "세션 연결 중" : snapshot.loadingStage().isBlank() ? "월드 준비" : snapshot.loadingStage();
        graphics.text(font, Component.literal(stage), x + 16, y + 55, TEXT, true);

        int percent = waitingForSnapshot ? 0 : Math.max(0, Math.min(100, snapshot.loadingPercent()));
        int barX = x + 16;
        int barY = y + 77;
        int barW = w - 32;
        graphics.fill(barX, barY, barX + barW, barY + 7, PANEL_LIGHT);
        int fill = (int)Math.round(barW * percent / 100.0);
        if (fill > 0) graphics.fill(barX, barY, barX + fill, barY + 7, GAUGE);
        String pct = waitingForSnapshot ? "연결" : percent + "%";
        graphics.text(font, Component.literal(pct), x + w - 16 - font.width(pct), y + 91, SECONDARY, true);
    }

    @Override public boolean keyPressed(KeyEvent event) { return true; }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) { return true; }
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { }
}
