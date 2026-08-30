package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.world.FieldUiSnapshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/** Non-dismissible initial authored-world build screen. */
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
        graphics.fill(0, 0, width, height, BACKGROUND);

        int w = Math.min(360, Math.max(240, width - 48));
        int h = 112;
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        graphics.fill(x, y, x + w, y + h, PANEL);
        graphics.fill(x, y, x + 3, y + h, GAUGE);

        String title = "TURNBOUND";
        graphics.text(font, Component.literal(title), x + 16, y + 14, TEXT, true);
        String region = "ASTER MARCH · 초기 월드 구성";
        graphics.text(font, Component.literal(region), x + 16, y + 31, SECONDARY, true);
        String stage = snapshot.loadingStage().isBlank() ? "월드 준비" : snapshot.loadingStage();
        graphics.text(font, Component.literal(stage), x + 16, y + 55, TEXT, true);

        int percent = Math.max(0, Math.min(100, snapshot.loadingPercent()));
        int barX = x + 16;
        int barY = y + 77;
        int barW = w - 32;
        graphics.fill(barX, barY, barX + barW, barY + 7, PANEL_LIGHT);
        int fill = (int)Math.round(barW * percent / 100.0);
        if (fill > 0) graphics.fill(barX, barY, barX + fill, barY + 7, GAUGE);
        String pct = percent + "%";
        graphics.text(font, Component.literal(pct), x + w - 16 - font.width(pct), y + 91, SECONDARY, true);
    }

    @Override public boolean keyPressed(KeyEvent event) { return true; }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) { return true; }
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { }
}
