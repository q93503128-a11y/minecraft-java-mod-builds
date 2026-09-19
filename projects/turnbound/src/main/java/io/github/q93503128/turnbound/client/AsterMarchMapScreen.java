package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.world.DrehmalWorldProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;

/**
 * TURNBOUND world overview bound to the current Drehmal production profile.
 *
 * <p>Only source-backed enabled landmarks are drawn. Exact encounter footprints and unverified runtime positions
 * are deliberately not projected onto the player map.</p>
 */
final class AsterMarchMapScreen extends Screen {
    private static final int TEXT = TurnboundUiTokens.TEXT_PRIMARY;
    private static final int SECONDARY = TurnboundUiTokens.TEXT_SECONDARY;
    private static final int MUTED = TurnboundUiTokens.TEXT_MUTED;
    private static final int BLUE = TurnboundUiTokens.PRIMARY;
    private static final int GOLD = TurnboundUiTokens.ACCENT;
    private static final int GREEN = TurnboundUiTokens.SUCCESS;

    private int left, top, panelWidth, panelHeight;
    private double zoom = 1.0;

    AsterMarchMapScreen() { super(Component.literal("월드 지도")); }

    @Override protected void init() {
        super.init();
        panelWidth = Math.min(980, Math.max(360, width - 22));
        panelHeight = Math.min(680, Math.max(300, height - 22));
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
    }

    @Override public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_M || event.key() == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        return super.keyPressed(event);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0.0) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        zoom = Math.max(1.0, Math.min(4.0, zoom + (scrollY > 0 ? 0.35 : -0.35)));
        return true;
    }

    @Override public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) { }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        double px = minecraft.player == null ? 0.0 : minecraft.player.position().x;
        double pz = minecraft.player == null ? 0.0 : minecraft.player.position().z;
        List<DrehmalWorldProfile.Anchor> anchors = DrehmalWorldProfile.profile().anchors().stream()
                .filter(DrehmalWorldProfile.Anchor::enabled)
                .toList();

        TurnboundFrameStyle.frame(graphics, left, top, panelWidth, panelHeight, BLUE);
        graphics.text(font, Component.literal("월드 지도"), left + 16, top + 14, TEXT, true);
        String help = "휠 확대/축소 · " + String.format(java.util.Locale.ROOT, "×%.1f", zoom) + "  /  M 또는 ESC 닫기";
        graphics.text(font, Component.literal(help), left + panelWidth - 16 - font.width(help), top + 14, SECONDARY, false);
        graphics.text(font, Component.literal("알려진 거점과 랜드마크 · 세부 길은 탐험하며 확인"), left + 16, top + 30, SECONDARY, false);

        boolean wide = panelWidth >= 500;
        int infoReserve = wide ? Math.min(210, Math.max(165, panelWidth / 3)) : 0;
        int mapSize = Math.min(panelHeight - (wide ? 64 : 126), panelWidth - 30 - infoReserve);
        mapSize = Math.max(170, mapSize);
        int mapX = left + 15;
        int mapY = top + 47;

        graphics.fill(mapX - 2, mapY - 2, mapX + mapSize + 2, mapY + mapSize + 2, 0xFF8B7B60);
        graphics.fill(mapX, mapY, mapX + mapSize, mapY + mapSize, 0xFF20262A);

        Bounds bounds = bounds(anchors, px, pz);
        Viewport view = viewport(bounds, px, pz);

        DrehmalWorldProfile.Anchor hovered = null;
        double hoveredDistance = Double.MAX_VALUE;
        for (DrehmalWorldProfile.Anchor anchor : anchors) {
            if (!inside(anchor.x(), anchor.z(), view)) continue;
            int sx = mapX + worldToMap(anchor.x(), view.minX, view.span, mapSize);
            int sy = mapY + worldToMap(anchor.z(), view.minZ, view.span, mapSize);
            drawMarker(graphics, sx, sy, anchor.kind());
            double dx = mouseX - sx, dy = mouseY - sy, distance = dx * dx + dy * dy;
            if (distance <= 100.0 && distance < hoveredDistance) {
                hoveredDistance = distance;
                hovered = anchor;
            }
        }

        if (inside(px, pz, view)) {
            int psx = mapX + worldToMap(px, view.minX, view.span, mapSize);
            int psy = mapY + worldToMap(pz, view.minZ, view.span, mapSize);
            graphics.fill(psx - 4, psy - 1, psx + 5, psy + 2, 0xFF101317);
            graphics.fill(psx - 1, psy - 4, psx + 2, psy + 5, 0xFF101317);
            graphics.fill(psx - 3, psy, psx + 4, psy + 1, 0xFFFFFFFF);
            graphics.fill(psx, psy - 3, psx + 1, psy + 4, 0xFFFFFFFF);
        }

        DrehmalWorldProfile.Anchor focus = hovered != null ? hovered : nearest(anchors, px, pz);
        int infoX = wide ? mapX + mapSize + 13 : mapX;
        int infoY = wide ? mapY : mapY + mapSize + 10;
        int infoW = wide ? left + panelWidth - 15 - infoX : mapSize;
        int infoH = wide ? mapSize : Math.max(60, top + panelHeight - 13 - infoY);
        TurnboundFrameStyle.inset(graphics, infoX, infoY, infoW, infoH);

        graphics.text(font, Component.literal("현재  X " + (int)Math.round(px) + " · Z " + (int)Math.round(pz)),
                infoX + 9, infoY + 9, TEXT, true);
        if (focus != null) {
            graphics.text(font, Component.literal(UiTextLayout.fit(label(focus), infoW - 18)),
                    infoX + 9, infoY + 29, markerColor(focus.kind()), true);
            graphics.text(font, Component.literal(UiTextLayout.fit(description(focus), infoW - 18)),
                    infoX + 9, infoY + 45, SECONDARY, false);
            int distance = (int)Math.round(Math.hypot(focus.x() - px, focus.z() - pz));
            graphics.text(font, Component.literal("약 " + distance + "m · " + kindLabel(focus.kind())),
                    infoX + 9, infoY + 61, MUTED, false);
        }

        if (infoH > 145) {
            graphics.text(font, Component.literal("◆ 거점"), infoX + 9, infoY + 92, GOLD, false);
            graphics.text(font, Component.literal("■ 지역"), infoX + 9, infoY + 109, BLUE, false);
            graphics.text(font, Component.literal("● 랜드마크"), infoX + 9, infoY + 126, GREEN, false);
        }
        if (infoH > 190) {
            graphics.text(font, Component.literal("표시된 위치는 알려진 장소의 기준점입니다."), infoX + 9, infoY + 153, SECONDARY, false);
            graphics.text(font, Component.literal("전투 지점은 지도에 미리 노출하지 않습니다."), infoX + 9, infoY + 170, MUTED, false);
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private static Bounds bounds(List<DrehmalWorldProfile.Anchor> anchors, double px, double pz) {
        double minX = px, maxX = px, minZ = pz, maxZ = pz;
        for (DrehmalWorldProfile.Anchor anchor : anchors) {
            minX = Math.min(minX, anchor.x());
            maxX = Math.max(maxX, anchor.x());
            minZ = Math.min(minZ, anchor.z());
            maxZ = Math.max(maxZ, anchor.z());
        }
        double centerX = (minX + maxX) * 0.5;
        double centerZ = (minZ + maxZ) * 0.5;
        double span = Math.max(700.0, Math.max(maxX - minX, maxZ - minZ) + 280.0);
        return new Bounds(centerX - span * 0.5, centerZ - span * 0.5, span);
    }

    private Viewport viewport(Bounds full, double px, double pz) {
        double span = full.span / zoom;
        if (zoom <= 1.001) return new Viewport(full.minX, full.minZ, span);
        double minX = clamp(px - span * 0.5, full.minX, full.minX + full.span - span);
        double minZ = clamp(pz - span * 0.5, full.minZ, full.minZ + full.span - span);
        return new Viewport(minX, minZ, span);
    }

    private static DrehmalWorldProfile.Anchor nearest(List<DrehmalWorldProfile.Anchor> anchors, double x, double z) {
        return anchors.stream().min(Comparator.comparingDouble(a -> Math.hypot(a.x() - x, a.z() - z))).orElse(null);
    }

    private static void drawMarker(GuiGraphicsExtractor g, int x, int y, String kind) {
        int color = markerColor(kind);
        if ("HUB".equals(kind)) {
            g.fill(x - 5, y - 1, x + 6, y + 2, color);
            g.fill(x - 1, y - 5, x + 2, y + 6, color);
        } else if ("REGION".equals(kind)) {
            g.fill(x - 4, y - 4, x + 5, y + 5, color);
            g.fill(x - 2, y - 2, x + 3, y + 3, 0xFF20262A);
        } else {
            g.fill(x - 3, y - 3, x + 4, y + 4, color);
        }
    }

    private static int markerColor(String kind) {
        return switch (kind) {
            case "HUB" -> GOLD;
            case "REGION" -> BLUE;
            default -> GREEN;
        };
    }

    private static String kindLabel(String kind) {
        return switch (kind) {
            case "HUB" -> "거점";
            case "REGION" -> "지역";
            default -> "랜드마크";
        };
    }

    private static String label(DrehmalWorldProfile.Anchor anchor) {
        return switch (anchor.locator()) {
            case "turnbound:hub/new_drabyel" -> "New Drabyel";
            case "turnbound:region/stasis_facility" -> "Stasis Facility";
            case "turnbound:landmark/primal_caverns" -> "Primal Caverns";
            case "turnbound:landmark/capital_valley_tower" -> "Capital Valley Tower";
            case "turnbound:landmark/warning_cave" -> "경고 동굴";
            case "turnbound:landmark/explorers_guide_camp" -> "Explorer's Guide 야영지";
            case "turnbound:region/avsal" -> "Av'Sal";
            default -> "알려진 장소";
        };
    }

    private static String description(DrehmalWorldProfile.Anchor anchor) {
        return switch (anchor.locator()) {
            case "turnbound:hub/new_drabyel" -> "Capital Valley의 첫 안전 거점";
            case "turnbound:region/stasis_facility" -> "Capital Valley 동쪽의 오래된 시설";
            case "turnbound:landmark/primal_caverns" -> "첫 여정의 지형 기준점";
            case "turnbound:landmark/capital_valley_tower" -> "Drabyel로 향하는 길의 큰 랜드마크";
            case "turnbound:landmark/warning_cave" -> "길에서 벗어난 위험한 동굴";
            case "turnbound:landmark/explorers_guide_camp" -> "Drabyel 전 마지막 휴식 지점";
            case "turnbound:region/avsal" -> "Drabyel 이후 이어지는 거대한 폐허";
            default -> "지도에 기록된 장소";
        };
    }

    private static int worldToMap(double value, double minimum, double span, int mapSize) {
        return (int)Math.round((value - minimum) / span * mapSize);
    }

    private static boolean inside(double x, double z, Viewport view) {
        return x >= view.minX && x <= view.minX + view.span && z >= view.minZ && z <= view.minZ + view.span;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record Bounds(double minX, double minZ, double span) {}
    private record Viewport(double minX, double minZ, double span) {}
}
