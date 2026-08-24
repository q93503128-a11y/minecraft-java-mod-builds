package kr.moonseungjun.frontiersettlement.client;

import kr.moonseungjun.frontiersettlement.network.SettlementContextPayload;
import kr.moonseungjun.frontiersettlement.network.SettlementSnapshotPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class SettlementHudOverlay {
    private SettlementHudOverlay() {}

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        SettlementSnapshotPayload data = ClientSettlementState.snapshot();
        if (!data.founded()) return;
        SettlementContextPayload context = ClientSettlementState.context();

        String line = data.tier() + "   목재 " + data.wood() + "   석재 " + data.stone()
                + "   금속 " + data.metal() + "   식량 " + data.food() + "   인구 " + data.population();
        int x = 8;
        int y = ClientCompanionLayout.resourceHudY();
        String project = context.projectLabel().isBlank() ? ""
                : context.projectLabel() + (context.projectProgress() >= 0 ? "   " + context.projectProgress() + "%" : "");
        int width = Math.max(minecraft.font.width(line), minecraft.font.width(data.nextGoal()));
        if (!project.isBlank()) width = Math.max(width, minecraft.font.width(project));
        width += 12;
        int height = project.isBlank() ? 32 : 45;
        graphics.fill(x, y, x + width, y + height, 0xA8000000);
        graphics.text(minecraft.font, line, x + 6, y + 4, 0xFFFFFFFF, true);
        graphics.text(minecraft.font, data.nextGoal(), x + 6, y + 18, 0xFFFFD58A, false);
        if (!project.isBlank()) graphics.text(minecraft.font, project, x + 6, y + 31, 0xFFD7D7D7, false);

        int modeY = y + height + 5;
        if (BuildingPlacementClient.active()) {
            drawModePanel(graphics, minecraft, x, modeY, BuildingPlacementClient.statusLine(), "B 팔레트   R 회전   Enter 건설");
        } else if (RoadPlacementClient.active()) {
            String controls = RoadPlacementClient.start() == null ? "B 팔레트   Enter 시작점" : "B 팔레트   Backspace 시작점 재선택   Enter 확정";
            drawModePanel(graphics, minecraft, x, modeY, RoadPlacementClient.statusLine(), controls);
        } else if (OutpostPlacementClient.active()) {
            drawModePanel(graphics, minecraft, x, modeY, OutpostPlacementClient.statusLine(), "B 팔레트   도로 끝 조준   Enter 건설");
        } else if (CivilWorkPlacementClient.active()) {
            String controls = CivilWorkPlacementClient.first() == null
                    ? "B 팔레트   Enter 첫 모서리"
                    : "B 팔레트   Backspace 첫 모서리 재선택   Enter 착공";
            drawModePanel(graphics, minecraft, x, modeY, CivilWorkPlacementClient.statusLine(), controls);
        }

        SettlementNoticeQueue.render(graphics, minecraft);
    }

    private static void drawModePanel(GuiGraphicsExtractor graphics, Minecraft minecraft, int x, int y, String status, String controls) {
        int panelWidth = Math.max(minecraft.font.width(status), minecraft.font.width(controls)) + 12;
        graphics.fill(x, y, x + panelWidth, y + 30, 0xB0000000);
        graphics.text(minecraft.font, status, x + 6, y + 4, 0xFFFFFFFF, true);
        graphics.text(minecraft.font, controls, x + 6, y + 17, 0xFFD7D7D7, false);
    }
}
