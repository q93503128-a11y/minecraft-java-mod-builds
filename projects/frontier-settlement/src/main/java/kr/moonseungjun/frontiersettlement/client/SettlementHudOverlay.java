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
    }
}
