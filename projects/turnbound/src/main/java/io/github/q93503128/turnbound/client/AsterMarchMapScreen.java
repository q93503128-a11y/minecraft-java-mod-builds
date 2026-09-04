package io.github.q93503128.turnbound.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

/** Full 1024x1024 schematic map for Aster March. It shows authored regions, facilities, hunts and bosses. */
final class AsterMarchMapScreen extends Screen {
    private static final int TEXT = 0xFFF4F0E6;
    private static final int SECONDARY = 0xFFB7B2AA;
    private static final int MUTED = 0xFF737B87;
    private static final int BLUE = 0xFF6DC6FF;
    private static final int GOLD = 0xFFFFC857;
    private static final int GREEN = 0xFF62D39A;
    private static final int RED = 0xFFFF6B6B;

    private int left, top, panelWidth, panelHeight;

    AsterMarchMapScreen() {
        super(Component.literal("아스테르 변경 지도"));
    }

    @Override
    protected void init() {
        super.init();
        panelWidth = Math.min(940, Math.max(330, width - 32));
        panelHeight = Math.min(650, Math.max(280, height - 32));
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_M || event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) { }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        TurnboundFrameStyle.frame(graphics, left, top, panelWidth, panelHeight, BLUE);
        graphics.text(font, Component.literal("아스테르 변경 · 지역 지도"), left + 18, top + 17, TEXT, true);
        graphics.text(font, Component.literal("시설 · 사냥터 · 보스 위치 / M 또는 ESC 닫기"), left + 18, top + 34, SECONDARY, false);

        boolean wide = panelWidth >= 650;
        int mapSize = wide
                ? Math.min(panelHeight - 72, panelWidth - 300)
                : Math.min(panelWidth - 36, panelHeight - 142);
        mapSize = Math.max(160, mapSize);
        int mapX = left + 18;
        int mapY = top + 56;
        graphics.fill(mapX, mapY, mapX + mapSize, mapY + mapSize, 0xDD11151A);

        // World axes every 128 blocks make the authored 1024x1024 footprint readable without pretending to be a terrain screenshot.
        for (int value = -384; value <= 384; value += 128) {
            int sx = mapX + worldToMap(value, mapSize);
            int sy = mapY + worldToMap(value, mapSize);
            graphics.fill(sx, mapY, sx + 1, mapY + mapSize, 0x244F5965);
            graphics.fill(mapX, sy, mapX + mapSize, sy + 1, 0x244F5965);
        }

        for (AsterMarchMapData.Region region : AsterMarchMapData.REGIONS) {
            int x1 = mapX + worldToMap(region.minX(), mapSize);
            int x2 = mapX + worldToMap(region.maxX(), mapSize);
            int y1 = mapY + worldToMap(region.minZ(), mapSize);
            int y2 = mapY + worldToMap(region.maxZ(), mapSize);
            outline(graphics, x1, y1, Math.max(2, x2 - x1), Math.max(2, y2 - y1), 0x887D8792);
        }

        AsterMarchMapData.Marker hovered = null;
        double hoveredDistance = Double.MAX_VALUE;
        for (AsterMarchMapData.Marker marker : AsterMarchMapData.MARKERS) {
            int sx = mapX + worldToMap(marker.x(), mapSize);
            int sy = mapY + worldToMap(marker.z(), mapSize);
            int color = markerColor(marker.kind());
            int r = marker.kind() == AsterMarchMapData.Kind.BOSS ? 3 : 2;
            graphics.fill(sx - r, sy - r, sx + r + 1, sy + r + 1, color);
            double dx = mouseX - sx;
            double dy = mouseY - sy;
            double d = dx * dx + dy * dy;
            if (d <= 49.0 && d < hoveredDistance) {
                hoveredDistance = d;
                hovered = marker;
            }
        }

        double px = minecraft.player == null ? 0.0 : minecraft.player.position().x;
        double pz = minecraft.player == null ? 0.0 : minecraft.player.position().z;
        int psx = mapX + worldToMap(px, mapSize);
        int psy = mapY + worldToMap(pz, mapSize);
        if (psx >= mapX && psx <= mapX + mapSize && psy >= mapY && psy <= mapY + mapSize) {
            graphics.fill(psx - 4, psy, psx + 5, psy + 1, 0xFFFFFFFF);
            graphics.fill(psx, psy - 4, psx + 1, psy + 5, 0xFFFFFFFF);
        }

        AsterMarchMapData.Marker focus = hovered != null ? hovered : AsterMarchMapData.nearest(px, pz);
        int infoX = wide ? mapX + mapSize + 18 : mapX;
        int infoY = wide ? mapY : mapY + mapSize + 12;
        int infoW = wide ? left + panelWidth - 18 - infoX : mapSize;
        int infoH = wide ? Math.min(164, panelHeight - 72) : Math.max(56, top + panelHeight - 18 - infoY);
        TurnboundFrameStyle.inset(graphics, infoX, infoY, infoW, infoH);
        graphics.text(font, Component.literal("현재 위치  X " + (int)Math.round(px) + " · Z " + (int)Math.round(pz)), infoX + 10, infoY + 10, TEXT, true);
        if (focus != null) {
            graphics.text(font, Component.literal(focus.label()), infoX + 10, infoY + 31, markerColor(focus.kind()), true);
            graphics.text(font, Component.literal(fit(focus.info(), infoW - 20)), infoX + 10, infoY + 47, SECONDARY, false);
            int distance = (int)Math.round(Math.hypot(focus.x() - px, focus.z() - pz));
            graphics.text(font, Component.literal("X " + (int)Math.round(focus.x()) + " · Z " + (int)Math.round(focus.z()) + " · 약 " + distance + "블록"),
                    infoX + 10, infoY + 63, MUTED, false);
        }
        if (wide && infoH >= 125) {
            graphics.text(font, Component.literal("■ 시설"), infoX + 10, infoY + 91, BLUE, false);
            graphics.text(font, Component.literal("■ 계전소"), infoX + 74, infoY + 91, GOLD, false);
            graphics.text(font, Component.literal("■ 사냥터"), infoX + 10, infoY + 108, GREEN, false);
            graphics.text(font, Component.literal("■ 보스"), infoX + 74, infoY + 108, RED, false);
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private int worldToMap(double value, int mapSize) {
        double t = (value - AsterMarchMapData.MIN) / (double)(AsterMarchMapData.MAX - AsterMarchMapData.MIN);
        return (int)Math.round(t * mapSize);
    }

    private void outline(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    private static int markerColor(AsterMarchMapData.Kind kind) {
        return switch (kind) {
            case FACILITY -> BLUE;
            case HUNT -> GREEN;
            case BOSS -> RED;
            case RELAY -> GOLD;
        };
    }

    private String fit(String value, int maxWidth) {
        if (font.width(value) <= maxWidth) return value;
        int end = value.length();
        while (end > 1 && font.width(value.substring(0, end) + "…") > maxWidth) end--;
        return value.substring(0, Math.max(1, end)) + "…";
    }
}
