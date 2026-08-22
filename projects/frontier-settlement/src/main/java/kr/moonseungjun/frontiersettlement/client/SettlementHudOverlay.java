package kr.moonseungjun.frontiersettlement.client;

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

        String line = "마을   목재 " + data.wood()
                + "   석재 " + data.stone()
                + "   금속 " + data.metal()
                + "   식량 " + data.food()
                + "   인구 " + data.population();
        int x = 8;
        int y = 8;
        int width = minecraft.font.width(line) + 12;
        graphics.fill(x, y, x + width, y + 18, 0xA8000000);
        graphics.text(minecraft.font, line, x + 6, y + 5, 0xFFFFFFFF, true);

        if (BuildingPlacementClient.active()) {
            drawModePanel(graphics, minecraft, x, y + 23,
                    BuildingPlacementClient.statusLine(),
                    "B 종료   N 건물   R 회전   Enter 건설");
        } else if (RoadPlacementClient.active()) {
            String controls = RoadPlacementClient.start() == null
                    ? "J 종료   Enter 시작점"
                    : "J 종료   Backspace 시작점 재선택   Enter 확정";
            drawModePanel(graphics, minecraft, x, y + 23,
                    RoadPlacementClient.statusLine(), controls);
        } else if (OutpostPlacementClient.active()) {
            drawModePanel(graphics, minecraft, x, y + 23,
                    OutpostPlacementClient.statusLine(),
                    "K 종료   도로 끝을 가리키기   Enter 전초기지 건설");
        }
    }

    private static void drawModePanel(GuiGraphicsExtractor graphics, Minecraft minecraft,
                                      int x, int y, String status, String controls) {
        int panelWidth = Math.max(minecraft.font.width(status), minecraft.font.width(controls)) + 12;
        graphics.fill(x, y, x + panelWidth, y + 30, 0xB0000000);
        graphics.text(minecraft.font, status, x + 6, y + 4, 0xFFFFFFFF, true);
        graphics.text(minecraft.font, controls, x + 6, y + 17, 0xFFD7D7D7, false);
    }
}
