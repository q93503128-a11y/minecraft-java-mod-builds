package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.world.FieldUiSnapshot;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.gui.GuiLayer;
import org.jetbrains.annotations.NotNull;

/** Compact exploration minimap. M opens the full schematic Aster March map. */
public final class AsterMarchMinimapLayer implements GuiLayer {
    private static final int TEXT = 0xFFF4F0E6;
    private static final int MUTED = 0xFFB7B2AA;
    private static final int BLUE = 0xFF6DC6FF;
    private static final int GOLD = 0xFFFFC857;
    private static final int GREEN = 0xFF62D39A;
    private static final int RED = 0xFFFF6B6B;
    private static final double RADIUS = 170.0;

    @Override
    public void render(@NotNull GuiGraphicsExtractor graphics, DeltaTracker tracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.gui.screen() != null) return;
        if (ClientBattleState.snapshot().active()) return;
        var field = ClientFieldState.snapshot();
        if (!field.active() || field.mode() == FieldUiSnapshot.Mode.LOADING) return;

        int size = Math.min(116, Math.max(92, graphics.guiWidth() / 7));
        int panelW = size + 20;
        int panelH = size + 47;
        int x = 10;
        int y = 10;
        TurnboundUiSkin.panel(graphics, x, y, panelW, panelH);
        graphics.text(minecraft.font, Component.literal("지역 지도 · M"), x + 10, y + 10, TEXT, true);

        int mapX = x + 10;
        int mapY = y + 27;
        graphics.fill(mapX, mapY, mapX + size, mapY + size, 0xC014171B);
        graphics.fill(mapX + size / 2, mapY, mapX + size / 2 + 1, mapY + size, 0x334D5560);
        graphics.fill(mapX, mapY + size / 2, mapX + size, mapY + size / 2 + 1, 0x334D5560);

        double px = minecraft.player.position().x;
        double pz = minecraft.player.position().z;
        double half = size / 2.0;
        for (AsterMarchMapData.Marker marker : AsterMarchMapData.MARKERS) {
            double dx = marker.x() - px;
            double dz = marker.z() - pz;
            if (Math.abs(dx) > RADIUS || Math.abs(dz) > RADIUS) continue;
            int sx = mapX + size / 2 + (int)Math.round(dx / RADIUS * half);
            int sy = mapY + size / 2 + (int)Math.round(dz / RADIUS * half);
            int color = markerColor(marker.kind());
            graphics.fill(sx - 1, sy - 1, sx + 2, sy + 2, color);
        }

        int cx = mapX + size / 2;
        int cy = mapY + size / 2;
        graphics.fill(cx - 3, cy, cx + 4, cy + 1, 0xFFFFFFFF);
        graphics.fill(cx, cy - 3, cx + 1, cy + 4, 0xFFFFFFFF);

        AsterMarchMapData.Marker nearest = AsterMarchMapData.nearest(px, pz);
        String footer = nearest == null ? "M · 전체 지도" : nearest.label() + " · " + nearest.info();
        graphics.text(minecraft.font, Component.literal(fit(minecraft, footer, panelW - 20)), x + 10, y + panelH - 14, MUTED, false);
    }

    private static int markerColor(AsterMarchMapData.Kind kind) {
        return switch (kind) {
            case FACILITY -> BLUE;
            case HUNT -> GREEN;
            case BOSS -> RED;
            case RELAY -> GOLD;
        };
    }

    private static String fit(Minecraft minecraft, String value, int maxWidth) {
        if (minecraft.font.width(value) <= maxWidth) return value;
        int end = value.length();
        while (end > 1 && minecraft.font.width(value.substring(0, end) + "…") > maxWidth) end--;
        return value.substring(0, Math.max(1, end)) + "…";
    }
}
