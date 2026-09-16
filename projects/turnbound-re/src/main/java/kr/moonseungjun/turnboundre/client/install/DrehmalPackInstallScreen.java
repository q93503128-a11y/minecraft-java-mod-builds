package kr.moonseungjun.turnboundre.client.install;

import kr.moonseungjun.turnboundre.client.ui.UiLayoutMetrics;
import kr.moonseungjun.turnboundre.client.ui.UiVisualLanguage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;

/** Player-facing first-run progress surface for the Modrinth pack's external world installation. */
public final class DrehmalPackInstallScreen extends Screen {
    private final Screen parent;
    private final Path gameDirectory;

    public DrehmalPackInstallScreen(Screen parent, Path gameDirectory) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.literal("TURNBOUND: RE"));
        this.parent = parent;
        this.gameDirectory = gameDirectory;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // The already-initialized title screen remains the visual scene behind the compact install surface.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (parent != null) parent.extractRenderState(graphics, mouseX, mouseY, partialTick);

        DrehmalPackInstaller.Snapshot state = DrehmalPackInstaller.snapshot();
        UiLayoutMetrics.Rect panel = panel();
        UiVisualLanguage.FrameState panelState = state.phase() == DrehmalPackInstaller.Phase.FAILED
                ? UiVisualLanguage.FrameState.WARNING
                : state.phase() == DrehmalPackInstaller.Phase.READY
                        ? UiVisualLanguage.FrameState.SUCCESS
                        : UiVisualLanguage.FrameState.IDLE;
        UiVisualLanguage.frame(graphics, panel.x(), panel.y(), panel.width(), panel.height(), panelState);
        UiVisualLanguage.titleBand(
                graphics,
                this.font,
                panel.x() + UiLayoutMetrics.SPACE_4,
                panel.y() + UiLayoutMetrics.SPACE_4,
                panel.width() - UiLayoutMetrics.SPACE_8,
                22,
                Component.literal("TURNBOUND: RE · 월드 준비"),
                UiVisualLanguage.TEXT_FOCUS,
                true);

        int textX = panel.x() + UiLayoutMetrics.SPACE_12;
        int textY = panel.y() + 38;
        graphics.text(
                this.font,
                Component.literal(phaseLabel(state)),
                textX,
                textY,
                state.phase() == DrehmalPackInstaller.Phase.FAILED
                        ? UiVisualLanguage.TEXT_WARNING
                        : state.phase() == DrehmalPackInstaller.Phase.READY
                                ? UiVisualLanguage.TEXT_SUCCESS
                                : UiVisualLanguage.TEXT_PRIMARY,
                true);

        String detail = detailLabel(state);
        if (!detail.isBlank()) {
            graphics.text(
                    this.font,
                    Component.literal(fit(detail, panel.width() - UiLayoutMetrics.SPACE_24)),
                    textX,
                    textY + 15,
                    UiVisualLanguage.TEXT_SECONDARY,
                    true);
        }

        if (state.phase() != DrehmalPackInstaller.Phase.FAILED) {
            int meterX = panel.x() + UiLayoutMetrics.SPACE_12;
            int meterY = panel.y() + 74;
            int meterWidth = panel.width() - UiLayoutMetrics.SPACE_24;
            UiVisualLanguage.meter(
                    graphics,
                    meterX,
                    meterY,
                    meterWidth,
                    10,
                    (int) Math.round(state.progress() * 1000.0D),
                    1000,
                    UiVisualLanguage.ENERGY_PROGRESS);
            graphics.text(
                    this.font,
                    Component.literal(Math.round(state.progress() * 100.0D) + "%"),
                    meterX,
                    meterY + 14,
                    UiVisualLanguage.TEXT_SECONDARY,
                    true);
        } else if (!state.failureMessage().isBlank()) {
            graphics.text(
                    this.font,
                    Component.literal(fit(state.failureMessage(), panel.width() - UiLayoutMetrics.SPACE_24)),
                    textX,
                    panel.y() + 74,
                    UiVisualLanguage.TEXT_WARNING,
                    true);
        }

        if (state.phase() == DrehmalPackInstaller.Phase.READY || state.phase() == DrehmalPackInstaller.Phase.FAILED) {
            renderActionButton(graphics, mouseX, mouseY, state.phase());
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            DrehmalPackInstaller.Snapshot state = DrehmalPackInstaller.snapshot();
            if ((state.phase() == DrehmalPackInstaller.Phase.READY
                    || state.phase() == DrehmalPackInstaller.Phase.FAILED)
                    && contains(actionButton(), (int) Math.floor(event.x()), (int) Math.floor(event.y()))) {
                if (state.phase() == DrehmalPackInstaller.Phase.FAILED) {
                    DrehmalPackInstaller.retry(gameDirectory);
                } else {
                    this.minecraft.gui.setScreen(parent);
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        DrehmalPackInstaller.Phase phase = DrehmalPackInstaller.snapshot().phase();
        if (phase == DrehmalPackInstaller.Phase.READY || phase == DrehmalPackInstaller.Phase.FAILED) {
            this.minecraft.gui.setScreen(parent);
        }
        // During download/assembly, ESC is intentionally ignored so the player cannot enter an incomplete world.
    }

    private void renderActionButton(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            DrehmalPackInstaller.Phase phase
    ) {
        UiLayoutMetrics.Rect bounds = actionButton();
        UiVisualLanguage.FrameState state = contains(bounds, mouseX, mouseY)
                ? UiVisualLanguage.FrameState.FOCUS
                : phase == DrehmalPackInstaller.Phase.READY
                        ? UiVisualLanguage.FrameState.SUCCESS
                        : UiVisualLanguage.FrameState.WARNING;
        UiVisualLanguage.frame(graphics, bounds.x(), bounds.y(), bounds.width(), bounds.height(), state);
        Component label = Component.literal(phase == DrehmalPackInstaller.Phase.READY ? "메인 화면으로" : "다시 시도");
        int x = bounds.x() + Math.max(UiLayoutMetrics.SPACE_4, (bounds.width() - this.font.width(label)) / 2);
        int y = bounds.y() + Math.max(UiLayoutMetrics.SPACE_2, (bounds.height() - this.font.lineHeight) / 2);
        graphics.text(this.font, label, x, y, UiVisualLanguage.textColor(state), true);
    }

    private String phaseLabel(DrehmalPackInstaller.Snapshot state) {
        return switch (state.phase()) {
            case IDLE, CHECKING -> "설치 상태를 확인하는 중...";
            case DOWNLOADING_MAP -> "공식 Drehmal 지도를 내려받는 중...";
            case ASSEMBLING_WORLD -> "TURNBOUND 월드를 구성하는 중...";
            case VERIFYING_WORLD -> "월드 파일을 검증하는 중...";
            case DOWNLOADING_RESOURCES -> "월드 리소스를 준비하는 중...";
            case FINALIZING -> "첫 원정을 준비하는 중...";
            case READY -> "준비 완료. TURNBOUND 월드를 열 수 있습니다.";
            case FAILED -> "월드 준비를 완료하지 못했습니다.";
        };
    }

    private String detailLabel(DrehmalPackInstaller.Snapshot state) {
        if (state.phase() == DrehmalPackInstaller.Phase.FAILED || state.detail().isBlank()) return "";
        if (state.total() > 1) return state.detail();
        return state.detail();
    }

    private UiLayoutMetrics.Rect panel() {
        int width = Math.min(440, Math.max(280, this.width - UiLayoutMetrics.SPACE_16 * 2));
        int height = 132;
        return new UiLayoutMetrics.Rect(
                Math.max(0, (this.width - width) / 2),
                Math.max(UiLayoutMetrics.SPACE_8, (this.height - height) / 2),
                width,
                height);
    }

    private UiLayoutMetrics.Rect actionButton() {
        UiLayoutMetrics.Rect panel = panel();
        int width = Math.min(126, panel.width() - UiLayoutMetrics.SPACE_24);
        return new UiLayoutMetrics.Rect(
                panel.x() + (panel.width() - width) / 2,
                panel.bottom() - 30,
                width,
                22);
    }

    private String fit(String text, int maxWidth) {
        if (text == null || text.isBlank() || maxWidth <= 0) return "";
        if (this.font.width(text) <= maxWidth) return text;
        String ellipsis = "...";
        int limit = Math.max(0, maxWidth - this.font.width(ellipsis));
        String value = text;
        while (!value.isEmpty() && this.font.width(value) > limit) {
            value = value.substring(0, value.length() - 1);
        }
        return value + ellipsis;
    }

    private static boolean contains(UiLayoutMetrics.Rect rect, int x, int y) {
        return x >= rect.x() && x < rect.right() && y >= rect.y() && y < rect.bottom();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
