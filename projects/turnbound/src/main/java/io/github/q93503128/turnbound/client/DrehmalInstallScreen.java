package io.github.q93503128.turnbound.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/** First-run world preparation surface for the one-click TURNBOUND pack. */
public final class DrehmalInstallScreen extends Screen {
    private static final int BACKGROUND = 0xF410131A;
    private static final int PANEL = 0xEF171C26;
    private static final int PANEL_LIGHT = 0xFF222A38;
    private static final int TEXT = 0xFFF4F0E6;
    private static final int SECONDARY = 0xFFAEB7C6;
    private static final int GAUGE = 0xFF6DC6FF;
    private static final int GOLD = 0xFFF4C96A;
    private static final int DANGER = 0xFFFF8278;

    private final Screen parent;
    private int completeTicks;

    public DrehmalInstallScreen(Screen parent) {
        super(Component.literal("TURNBOUND 첫 실행 준비"));
        this.parent = parent;
    }

    @Override
    public void tick() {
        super.tick();
        var state = DrehmalAutoInstaller.snapshot();
        if (state.phase() == DrehmalAutoInstaller.Phase.COMPLETE) {
            completeTicks++;
            if (completeTicks >= 24 && minecraft != null && minecraft.gui.screen() == this) {
                minecraft.gui.setScreen(parent);
            }
        } else {
            completeTicks = 0;
        }
    }

    @Override public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) { }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        var state = DrehmalAutoInstaller.snapshot();
        graphics.fill(0, 0, width, height, BACKGROUND);

        int w = Math.min(460, Math.max(280, width - 56));
        int h = 156;
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        graphics.fill(x, y, x + w, y + h, PANEL);
        graphics.fill(x, y, x + 3, y + h, state.phase() == DrehmalAutoInstaller.Phase.FAILED ? DANGER : GAUGE);

        graphics.text(font, Component.literal("TURNBOUND"), x + 18, y + 15, TEXT, true);
        graphics.text(font, Component.literal("첫 실행에서만 Drehmal 2.2.2f 공식 파일을 준비합니다."),
                x + 18, y + 34, SECONDARY, true);
        graphics.text(font, Component.literal(state.title()), x + 18, y + 60,
                state.phase() == DrehmalAutoInstaller.Phase.FAILED ? DANGER : TEXT, true);

        String detail = state.phase() == DrehmalAutoInstaller.Phase.FAILED && !state.error().isBlank()
                ? state.error() : state.detail();
        if (detail.length() > 64) detail = detail.substring(0, 61) + "...";
        graphics.text(font, Component.literal(detail), x + 18, y + 78, SECONDARY, true);

        int barX = x + 18;
        int barY = y + 105;
        int barW = w - 36;
        graphics.fill(barX, barY, barX + barW, barY + 8, PANEL_LIGHT);
        int fill = (int)Math.round(barW * state.percent() / 100.0);
        if (fill > 0) graphics.fill(barX, barY, barX + fill, barY + 8, GAUGE);

        String percent = state.percent() + "%";
        graphics.text(font, Component.literal(percent), x + w - 18 - font.width(percent), y + 120, TEXT, true);

        String footer = state.phase() == DrehmalAutoInstaller.Phase.COMPLETE
                ? "완료 · 다음 TURNBOUND 업데이트부터는 JAR만 바꾸면 됩니다."
                : state.phase() == DrehmalAutoInstaller.Phase.FAILED
                ? "기존 파일은 건드리지 않았습니다. ESC로 돌아갈 수 있습니다."
                : "설치 중에는 게임을 종료하지 마세요. 중단된 다운로드는 다음 실행에서 이어받습니다.";
        graphics.text(font, Component.literal(footer), x + 18, y + 137,
                state.phase() == DrehmalAutoInstaller.Phase.COMPLETE ? GOLD : SECONDARY, true);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (DrehmalAutoInstaller.snapshot().phase() == DrehmalAutoInstaller.Phase.FAILED) {
            return super.keyPressed(event);
        }
        return true;
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) { return true; }
    @Override public boolean shouldCloseOnEsc() {
        return DrehmalAutoInstaller.snapshot().phase() == DrehmalAutoInstaller.Phase.FAILED;
    }
    @Override public boolean isPauseScreen() { return false; }
}
