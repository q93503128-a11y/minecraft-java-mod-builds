package kr.moonseungjun.survivalascension.client;

import kr.moonseungjun.survivalascension.expedition.ExpeditionRegion;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class ExpeditionScreen extends Screen {
    private static final int PANEL_MAX_WIDTH = 700;
    private static final int TOP = 54;
    private static final int ROW_HEIGHT = 28;
    private static final int BOTTOM_MARGIN = 42;
    private static final int SCROLL_STEP = 36;

    private double scrollOffset;
    private int maxScroll;

    public ExpeditionScreen() {
        super(Component.literal("원정 기록"));
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(this.width / 2 - 60, this.height - 28, 120, 20).build());
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(null);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0.0D || maxScroll <= 0) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        scrollOffset = Math.max(0.0D, Math.min(maxScroll, scrollOffset - scrollY * SCROLL_STEP));
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, this.width, this.height, 0x88000000);

        int panelWidth = Math.min(PANEL_MAX_WIDTH, Math.max(240, this.width - 28));
        int left = (this.width - panelWidth) / 2;
        int right = left + panelWidth;
        int bottom = this.height - BOTTOM_MARGIN;

        graphics.fill(left, 10, right, Math.max(44, bottom), 0xCC101820);
        graphics.fill(left, 10, right, 12, 0xFF3B82A0);
        graphics.text(this.font, "원정 기록", left + 12, 18, 0xFFFFFFFF, true);

        if (!ClientExpeditionState.loaded()) {
            String waiting = "서버 원정 기록 동기화 중...";
            graphics.text(this.font, waiting, this.width / 2 - this.font.width(waiting) / 2, TOP + 24, 0xFFD0D0D0, false);
            return;
        }

        int completed = Integer.bitCount(ClientExpeditionState.completedMask());
        int discovered = Integer.bitCount(ClientExpeditionState.discoveredMask());
        String header = "완료 " + completed + "/" + ExpeditionRegion.values().length + " · 발견 " + discovered + "/" + ExpeditionRegion.values().length + " · J = 닫기";
        graphics.text(this.font, header, right - 12 - this.font.width(header), 19, 0xFFB8D9E8, false);

        int viewportHeight = Math.max(1, bottom - TOP);
        int contentHeight = ExpeditionRegion.values().length * ROW_HEIGHT;
        maxScroll = Math.max(0, contentHeight - viewportHeight);
        scrollOffset = Math.max(0.0D, Math.min(maxScroll, scrollOffset));

        graphics.enableScissor(left + 6, TOP, right - 6, bottom);
        int y = TOP - (int)Math.round(scrollOffset);
        for (ExpeditionRegion region : ExpeditionRegion.values()) {
            boolean complete = (ClientExpeditionState.completedMask() & region.bit()) != 0;
            boolean discoveredRegion = (ClientExpeditionState.discoveredMask() & region.bit()) != 0;
            int rowTop = y + 1;
            int rowBottom = y + ROW_HEIGHT - 2;
            int bg = complete ? 0x88305A42 : discoveredRegion ? 0x88634E26 : 0x88404850;
            graphics.fill(left + 10, rowTop, right - 10, rowBottom, bg);

            String status = complete ? "✓ 완료" : discoveredRegion ? "◐ 진행" : "· 미발견";
            int statusColor = complete ? 0xFF79E39C : discoveredRegion ? 0xFFFFD166 : 0xFF8A9299;
            graphics.text(this.font, status, left + 18, y + 6, statusColor, true);
            graphics.text(this.font, region.koreanName(), left + 82, y + 6, 0xFFFFFFFF, true);

            String stage = switch (region.requiredWorldStage()) {
                case 0 -> "각성";
                case 1 -> "전설";
                default -> "종말";
            };
            graphics.text(this.font, stage, left + 82, y + 16, 0xFF8FB8C8, false);

            String detail = discoveredRegion ? ClientExpeditionState.directive(region.name()) : "해당 지역을 탐사하면 지령이 공개됩니다.";
            int detailX = left + 132;
            int maxDetailWidth = Math.max(60, right - 18 - detailX);
            if (this.font.width(detail) > maxDetailWidth) {
                String ellipsis = "…";
                while (!detail.isEmpty() && this.font.width(detail + ellipsis) > maxDetailWidth) detail = detail.substring(0, detail.length() - 1);
                detail += ellipsis;
            }
            graphics.text(this.font, detail, detailX, y + 11, complete ? 0xFFC9F2D4 : 0xFFD5D5D5, false);
            y += ROW_HEIGHT;
        }
        graphics.disableScissor();

        if (maxScroll > 0) {
            int trackX = right - 7;
            int barHeight = Math.max(18, viewportHeight * viewportHeight / contentHeight);
            int travel = Math.max(1, viewportHeight - barHeight);
            int barTop = TOP + (int)Math.round(scrollOffset * travel / maxScroll);
            graphics.fill(trackX, TOP, trackX + 2, bottom, 0x55404040);
            graphics.fill(trackX, barTop, trackX + 2, Math.min(bottom, barTop + barHeight), 0xFFC0DCE8);
        }
    }
}
