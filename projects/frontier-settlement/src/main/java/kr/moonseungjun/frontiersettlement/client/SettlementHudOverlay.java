package kr.moonseungjun.frontiersettlement.client;

import kr.moonseungjun.frontiersettlement.network.SettlementContextPayload;
import kr.moonseungjun.frontiersettlement.network.SettlementSnapshotPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Compact world-first HUD. Full growth guidance belongs to the M settlement command palette. */
public final class SettlementHudOverlay {
    private static final int MAX_IDLE_WIDTH = 310;
    private static final int MAX_MODE_WIDTH = 430;

    private SettlementHudOverlay() {}

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        SettlementSnapshotPayload data = ClientSettlementState.snapshot();
        int x = FrontierUiTheme.S;
        int y = ClientCompanionLayout.resourceHudY();

        if (!data.founded()) {
            String title = "FRONTIER SETTLEMENT";
            String hint = "공동 개척지 없음 · M으로 개척 시작";
            int width = Math.min(MAX_IDLE_WIDTH,
                    Math.max(minecraft.font.width(title), minecraft.font.width(hint)) + FrontierUiTheme.L);
            FrontierUiTheme.panel(graphics, x, y, width, 34);
            graphics.fill(x, y, x + 3, y + 34, FrontierUiTheme.PRIMARY);
            graphics.text(minecraft.font, Component.literal(trim(minecraft, title, width - FrontierUiTheme.L)),
                    x + FrontierUiTheme.S, y + 5, FrontierUiTheme.TEXT_PRIMARY, true);
            graphics.text(minecraft.font, Component.literal(trim(minecraft, hint, width - FrontierUiTheme.L)),
                    x + FrontierUiTheme.S, y + 19, FrontierUiTheme.ACCENT, false);
            SettlementNoticeQueue.render(graphics, minecraft);
            return;
        }

        SettlementContextPayload context = ClientSettlementState.context();
        String tier = data.tier() == null || data.tier().isBlank() ? "마을" : data.tier();
        String header = tier + "  ·  인구 " + data.population();
        String resources = "목재 " + data.wood() + "   석재 " + data.stone()
                + "   금속 " + data.metal() + "   식량 " + data.food();
        String project = context.projectLabel().isBlank() ? ""
                : context.projectLabel() + (context.projectProgress() >= 0 ? "  " + context.projectProgress() + "%" : "");

        int width = Math.max(minecraft.font.width(header), minecraft.font.width(resources)) + FrontierUiTheme.L;
        if (!project.isBlank()) width = Math.max(width, minecraft.font.width("공사 · " + project) + FrontierUiTheme.L);
        width = Math.min(MAX_IDLE_WIDTH, width);
        int height = project.isBlank() ? 34 : 51;

        FrontierUiTheme.panel(graphics, x, y, width, height);
        graphics.fill(x, y, x + 3, y + height, FrontierUiTheme.PRIMARY);
        graphics.text(minecraft.font, Component.literal(trim(minecraft, header, width - FrontierUiTheme.L)),
                x + FrontierUiTheme.S, y + 5, FrontierUiTheme.TEXT_PRIMARY, true);
        graphics.text(minecraft.font, Component.literal(trim(minecraft, resources, width - FrontierUiTheme.L)),
                x + FrontierUiTheme.S, y + 19, FrontierUiTheme.TEXT_SECONDARY, false);

        if (!project.isBlank()) {
            FrontierUiTheme.divider(graphics, x + FrontierUiTheme.S, y + 32, width - FrontierUiTheme.L);
            graphics.text(minecraft.font,
                    Component.literal(trim(minecraft, "공사 · " + project, width - FrontierUiTheme.L)),
                    x + FrontierUiTheme.S, y + 35, FrontierUiTheme.TEXT_PRIMARY, false);
            if (context.projectProgress() >= 0) {
                FrontierUiTheme.progress(graphics, x + FrontierUiTheme.S, y + 47,
                        width - FrontierUiTheme.L, 2, context.projectProgress(), 100);
            }
        }

        int modeY = y + height + FrontierUiTheme.XS;
        if (BuildingPlacementClient.active()) {
            drawModePanel(graphics, minecraft, x, modeY,
                    BuildingPlacementClient.statusLine(), "R 회전   ·   Enter 건설   ·   M 메뉴");
        } else if (RoadPlacementClient.active()) {
            String controls = RoadPlacementClient.start() == null
                    ? "Enter 시작점   ·   M 메뉴"
                    : "Enter 확정   ·   Backspace 재선택   ·   M 메뉴";
            drawModePanel(graphics, minecraft, x, modeY, RoadPlacementClient.statusLine(), controls);
        } else if (OutpostPlacementClient.active()) {
            drawModePanel(graphics, minecraft, x, modeY,
                    OutpostPlacementClient.statusLine(), "도로 끝 조준   ·   Enter 건설   ·   M 메뉴");
        } else if (CivilWorkPlacementClient.active()) {
            String controls = CivilWorkPlacementClient.first() == null
                    ? "Enter 첫 모서리   ·   M 메뉴"
                    : "Enter 착공   ·   Backspace 재선택   ·   M 메뉴";
            drawModePanel(graphics, minecraft, x, modeY, CivilWorkPlacementClient.statusLine(), controls);
        }

        SettlementNoticeQueue.render(graphics, minecraft);
    }

    private static void drawModePanel(GuiGraphicsExtractor graphics, Minecraft minecraft,
                                      int x, int y, String status, String controls) {
        String safeStatus = status == null || status.isBlank() ? "상태 확인 중" : status;
        int width = Math.max(minecraft.font.width(safeStatus), minecraft.font.width(controls)) + FrontierUiTheme.L;
        width = Math.min(MAX_MODE_WIDTH, width);
        FrontierUiTheme.panel(graphics, x, y, width, 36);
        graphics.fill(x, y, x + 3, y + 36, modeColor(safeStatus));
        graphics.text(minecraft.font, Component.literal(trim(minecraft, safeStatus, width - FrontierUiTheme.L)),
                x + FrontierUiTheme.S, y + 5, FrontierUiTheme.TEXT_PRIMARY, true);
        graphics.text(minecraft.font, Component.literal(trim(minecraft, controls, width - FrontierUiTheme.L)),
                x + FrontierUiTheme.S, y + 21, FrontierUiTheme.TEXT_SECONDARY, false);
    }

    private static int modeColor(String status) {
        if (status.contains("가능") || status.contains("완료")) return FrontierUiTheme.SUCCESS;
        if (status.contains("확인 중") || status.contains("대기")) return FrontierUiTheme.WARNING;
        if (status.contains("불가") || status.contains("부족") || status.contains("막") || status.contains("초과")) {
            return FrontierUiTheme.DANGER;
        }
        return FrontierUiTheme.PRIMARY;
    }

    private static String trim(Minecraft minecraft, String text, int maxWidth) {
        if (text == null || maxWidth <= 0) return "";
        if (minecraft.font.width(text) <= maxWidth) return text;
        String suffix = "…";
        String out = text;
        while (!out.isEmpty() && minecraft.font.width(out + suffix) > maxWidth) {
            out = out.substring(0, out.length() - 1);
        }
        return out + suffix;
    }
}
