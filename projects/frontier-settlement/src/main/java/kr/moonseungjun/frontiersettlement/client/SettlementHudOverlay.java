package kr.moonseungjun.frontiersettlement.client;

import kr.moonseungjun.frontiersettlement.network.SettlementContextPayload;
import kr.moonseungjun.frontiersettlement.network.SettlementSnapshotPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class SettlementHudOverlay {
    private static final int PANEL_BG = 0xB8121418;
    private static final int PANEL_EDGE = 0xFFD0A45C;
    private static final int TEXT_PRIMARY = 0xFFF4F1EA;
    private static final int TEXT_SECONDARY = 0xFFBDB7AC;
    private static final int TEXT_ACCENT = 0xFFFFD58A;
    private static final int DIVIDER = 0x665A5144;

    private SettlementHudOverlay() {}

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        SettlementSnapshotPayload data = ClientSettlementState.snapshot();
        int x = 8;
        int y = ClientCompanionLayout.resourceHudY();

        if (!data.founded()) {
            String title = "FRONTIER SETTLEMENT";
            String hint = "공동 개척지 없음 · M으로 개척 시작";
            int width = Math.max(minecraft.font.width(title), minecraft.font.width(hint)) + 18;
            graphics.fill(x, y, x + width, y + 34, PANEL_BG);
            graphics.fill(x, y, x + 3, y + 34, PANEL_EDGE);
            graphics.text(minecraft.font, Component.literal(title), x + 8, y + 5, TEXT_PRIMARY, true);
            graphics.text(minecraft.font, Component.literal(hint), x + 8, y + 19, TEXT_ACCENT, false);
            SettlementNoticeQueue.render(graphics, minecraft);
            return;
        }

        SettlementContextPayload context = ClientSettlementState.context();
        String header = data.tier() + "  ·  인구 " + data.population();
        String resources = "목재 " + data.wood() + "   석재 " + data.stone()
                + "   금속 " + data.metal() + "   식량 " + data.food();
        String goal = data.nextGoal().isBlank() ? "" : "다음 · " + data.nextGoal();
        String project = context.projectLabel().isBlank() ? ""
                : context.projectLabel() + (context.projectProgress() >= 0 ? "   " + context.projectProgress() + "%" : "");

        int width = Math.max(minecraft.font.width(header), minecraft.font.width(resources));
        if (!goal.isBlank()) width = Math.max(width, minecraft.font.width(goal));
        if (!project.isBlank()) width = Math.max(width, minecraft.font.width(project));
        width += 18;

        int height = 34;
        if (!goal.isBlank()) height += 13;
        if (!project.isBlank()) height += 17;

        graphics.fill(x, y, x + width, y + height, PANEL_BG);
        graphics.fill(x, y, x + 3, y + height, PANEL_EDGE);
        graphics.text(minecraft.font, Component.literal(header), x + 8, y + 5, TEXT_PRIMARY, true);
        graphics.text(minecraft.font, Component.literal(resources), x + 8, y + 19, TEXT_SECONDARY, false);

        int cursorY = y + 32;
        if (!goal.isBlank()) {
            graphics.fill(x + 8, cursorY - 2, x + width - 7, cursorY - 1, DIVIDER);
            graphics.text(minecraft.font, Component.literal(goal), x + 8, cursorY + 2, TEXT_ACCENT, false);
            cursorY += 13;
        }
        if (!project.isBlank()) {
            graphics.fill(x + 8, cursorY - 2, x + width - 7, cursorY - 1, DIVIDER);
            graphics.text(minecraft.font, Component.literal("공사 · " + project), x + 8, cursorY + 2, 0xFFD7D7D7, false);
            if (context.projectProgress() >= 0) {
                int barX = x + 8;
                int barY = cursorY + 13;
                int barWidth = width - 16;
                int fill = Math.round(barWidth * Math.min(100, Math.max(0, context.projectProgress())) / 100.0F);
                graphics.fill(barX, barY, barX + barWidth, barY + 2, 0xFF34383D);
                if (fill > 0) graphics.fill(barX, barY, barX + fill, barY + 2, PANEL_EDGE);
            }
        }

        int modeY = y + height + 5;
        if (BuildingPlacementClient.active()) {
            drawModePanel(graphics, minecraft, x, modeY, BuildingPlacementClient.statusLine(), "R 회전   Enter 건설   M 메뉴");
        } else if (RoadPlacementClient.active()) {
            String controls = RoadPlacementClient.start() == null ? "Enter 시작점   M 메뉴" : "Enter 확정   Backspace 재선택   M 메뉴";
            drawModePanel(graphics, minecraft, x, modeY, RoadPlacementClient.statusLine(), controls);
        } else if (OutpostPlacementClient.active()) {
            drawModePanel(graphics, minecraft, x, modeY, OutpostPlacementClient.statusLine(), "도로 끝 조준   Enter 건설   M 메뉴");
        } else if (CivilWorkPlacementClient.active()) {
            String controls = CivilWorkPlacementClient.first() == null
                    ? "Enter 첫 모서리   M 메뉴"
                    : "Enter 착공   Backspace 재선택   M 메뉴";
            drawModePanel(graphics, minecraft, x, modeY, CivilWorkPlacementClient.statusLine(), controls);
        }

        SettlementNoticeQueue.render(graphics, minecraft);
    }

    private static void drawModePanel(GuiGraphicsExtractor graphics, Minecraft minecraft, int x, int y, String status, String controls) {
        int panelWidth = Math.max(minecraft.font.width(status), minecraft.font.width(controls)) + 18;
        graphics.fill(x, y, x + panelWidth, y + 32, 0xC0101215);
        graphics.fill(x, y, x + 3, y + 32, 0xFFC58E43);
        graphics.text(minecraft.font, status, x + 8, y + 5, TEXT_PRIMARY, true);
        graphics.text(minecraft.font, controls, x + 8, y + 19, TEXT_SECONDARY, false);
    }
}
