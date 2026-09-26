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
final class DrehmalWorldMapScreen extends Screen {
    private static final int TEXT = TurnboundUiTokens.TEXT_PRIMARY;
    private static final int SECONDARY = TurnboundUiTokens.TEXT_SECONDARY;
    private static final int MUTED = TurnboundUiTokens.TEXT_MUTED;
    private static final int BLUE = TurnboundUiTokens.PRIMARY;
    private static final int GOLD = TurnboundUiTokens.ACCENT;
    private static final int GREEN = TurnboundUiTokens.SUCCESS;

    private int left, top, panelWidth, panelHeight;
    private double zoom = 1.0;

    DrehmalWorldMapScreen() { super(Component.literal("월드 지도")); }

    @Override protected void init() {
        super.init();
        boolean compact = height < 330 || width < 520;
        int margin = compact ? 6 : 11;
        panelWidth = compact
                ? Math.min(340, Math.max(1, width - margin * 2))
                : Math.min(980, Math.max(1, width - margin * 2));
        panelHeight = Math.min(680, Math.max(1, height - margin * 2));
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
        boolean compact = panelHeight < 300 || panelWidth < 520;
        graphics.text(font, Component.literal("월드 지도"), left + 12, top + 11, TEXT, true);
        String help = compact
                ? String.format(java.util.Locale.ROOT, "×%.1f · M/ESC", zoom)
                : "휠 확대/축소 · " + String.format(java.util.Locale.ROOT, "×%.1f", zoom) + " · M 또는 ESC 닫기";
        int helpX = left + panelWidth - 12 - font.width(help);
        if (helpX > left + 90) {
            graphics.text(font, Component.literal(help), helpX, top + 11, SECONDARY, false);
        }
        boolean showSubtitle = !compact || panelHeight >= 285;
        if (showSubtitle) {
            graphics.text(font, Component.literal("알려진 거점과 랜드마크 · 세부 길은 탐험하며 확인"), left + 12, top + 27, SECONDARY, false);
        }

        boolean wide = panelWidth >= 320 && panelHeight >= 190;
        int mapY = top + (showSubtitle ? 43 : 30);
        int mapX = left + 12;
        int infoReserve = wide ? Math.min(190, Math.max(150, panelWidth / 3)) : 0;
        int bottomReserve = wide ? 12 : compact ? 68 : 108;
        int availableW = panelWidth - 24 - infoReserve - (wide ? 10 : 0);
        int availableH = top + panelHeight - bottomReserve - mapY;
        int mapSize = Math.max(64, Math.min(availableW, availableH));

        drawMapSurface(graphics, mapX, mapY, mapSize);

        Bounds bounds = bounds(anchors, px, pz);
        Viewport view = viewport(bounds, px, pz);
        drawRouteNetwork(graphics, mapX, mapY, mapSize, view, anchors);

        DrehmalWorldProfile.Anchor hovered = null;
        double hoveredDistance = Double.MAX_VALUE;
        for (DrehmalWorldProfile.Anchor anchor : anchors) {
            if (!inside(anchor.x(), anchor.z(), view)) continue;
            int sx = mapX + worldToMap(anchor.x(), view.minX, view.span, mapSize);
            int sy = mapY + worldToMap(anchor.z(), view.minZ, view.span, mapSize);
            drawMarker(graphics, sx, sy, anchor.kind());
            if (mapSize >= 220 && ("HUB".equals(anchor.kind()) || "REGION".equals(anchor.kind()))) {
                String shortLabel = UiTextLayout.fit(label(anchor), 82);
                graphics.text(font, Component.literal(shortLabel), sx + 7, sy - 4, markerColor(anchor.kind()), false);
            }
            double dx = mouseX - sx, dy = mouseY - sy, distance = dx * dx + dy * dy;
            if (distance <= 100.0 && distance < hoveredDistance) {
                hoveredDistance = distance;
                hovered = anchor;
            }
        }

        if (inside(px, pz, view)) {
            int psx = mapX + worldToMap(px, view.minX, view.span, mapSize);
            int psy = mapY + worldToMap(pz, view.minZ, view.span, mapSize);
            float yaw = minecraft.player == null ? 0.0F : minecraft.player.getYRot();
            drawMapArrow(graphics, psx, psy, yaw, 0xFFFFFFFF, true);
        }

        var navigation = ClientFieldState.snapshot().navigation();
        if (navigation != null && navigation.active() && inside(navigation.x(), navigation.z(), view)) {
            int nsx = mapX + worldToMap(navigation.x(), view.minX, view.span, mapSize);
            int nsy = mapY + worldToMap(navigation.z(), view.minZ, view.span, mapSize);
            drawObjectiveMarker(graphics, nsx, nsy);
        }

        DrehmalWorldProfile.Anchor focus = hovered != null ? hovered : nearest(anchors, px, pz);
        int infoX = wide ? mapX + mapSize + 10 : mapX;
        int infoY = wide ? mapY : mapY + mapSize + 6;
        int infoW = wide ? left + panelWidth - 12 - infoX : mapSize;
        int infoH = wide ? mapSize : Math.max(40, top + panelHeight - 8 - infoY);
        TurnboundFrameStyle.inset(graphics, infoX, infoY, infoW, infoH);

        String coordinates = "현재  X " + (int)Math.round(px) + " · Z " + (int)Math.round(pz);
        graphics.text(font, Component.literal(UiTextLayout.fit(coordinates, infoW - 14)),
                infoX + 7, infoY + 7, TEXT, true);
        if (focus != null && infoH >= 38) {
            graphics.text(font, Component.literal(UiTextLayout.fit(label(focus), infoW - 14)),
                    infoX + 7, infoY + 23, markerColor(focus.kind()), true);
            if (infoH >= 55) {
                graphics.text(font, Component.literal(UiTextLayout.fit(description(focus), infoW - 14)),
                        infoX + 7, infoY + 39, SECONDARY, false);
            }
            if (infoH >= 72) {
                int distance = (int)Math.round(Math.hypot(focus.x() - px, focus.z() - pz));
                String distanceLine = "약 " + distance + "m · " + kindLabel(focus.kind());
                graphics.text(font, Component.literal(UiTextLayout.fit(distanceLine, infoW - 14)),
                        infoX + 7, infoY + 55, MUTED, false);
            }
        }

        if (wide && infoH > 145) {
            graphics.text(font, Component.literal("◆ 거점"), infoX + 9, infoY + 92, GOLD, false);
            graphics.text(font, Component.literal("■ 지역"), infoX + 9, infoY + 109, BLUE, false);
            graphics.text(font, Component.literal("● 랜드마크"), infoX + 9, infoY + 126, GREEN, false);
        }
        if (wide && infoH > 190) {
            String note = "표시는 알려진 장소의 기준점입니다. 전투 위치는 탐험하기 전에는 숨겨집니다.";
            int noteY = infoY + 153;
            for (String line : UiTextLayout.wrap(note, infoW - 18, 4)) {
                if (noteY + font.lineHeight >= infoY + infoH - 6) break;
                graphics.text(font, Component.literal(line), infoX + 9, noteY, SECONDARY, false);
                noteY += font.lineHeight + 2;
            }
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private static Bounds bounds(List<DrehmalWorldProfile.Anchor> anchors, double px, double pz) {
        if (anchors.isEmpty()) return new Bounds(px - 350.0, pz - 350.0, 700.0);
        double minX = anchors.getFirst().x(), maxX = minX;
        double minZ = anchors.getFirst().z(), maxZ = minZ;
        double nearestPlayerDistance = Double.MAX_VALUE;
        for (DrehmalWorldProfile.Anchor anchor : anchors) {
            minX = Math.min(minX, anchor.x());
            maxX = Math.max(maxX, anchor.x());
            minZ = Math.min(minZ, anchor.z());
            maxZ = Math.max(maxZ, anchor.z());
            nearestPlayerDistance = Math.min(nearestPlayerDistance, Math.hypot(anchor.x() - px, anchor.z() - pz));
        }
        // The original Drehmal setup terminal is ~25k blocks away from the actual campaign geography.
        // Never let that staging room collapse the whole-world overview into an unreadable dot.
        if (nearestPlayerDistance <= 3000.0) {
            minX = Math.min(minX, px);
            maxX = Math.max(maxX, px);
            minZ = Math.min(minZ, pz);
            maxZ = Math.max(maxZ, pz);
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
            case "turnbound:hub/new_drabyel" -> "뉴 드라비엘";
            case "turnbound:region/stasis_facility" -> "스테이시스 시설";
            case "turnbound:landmark/primal_caverns" -> "프라이멀 동굴";
            case "turnbound:landmark/capital_valley_tower" -> "캐피털 밸리 탑";
            case "turnbound:landmark/warning_cave" -> "경고 동굴";
            case "turnbound:landmark/explorers_guide_camp" -> "탐험가 안내서 야영지";
            case "turnbound:region/avsal" -> "아브살";
            default -> "알려진 장소";
        };
    }

    private static String description(DrehmalWorldProfile.Anchor anchor) {
        return switch (anchor.locator()) {
            case "turnbound:hub/new_drabyel" -> "캐피털 밸리의 첫 안전 거점";
            case "turnbound:region/stasis_facility" -> "Capital Valley 동쪽의 오래된 시설";
            case "turnbound:landmark/primal_caverns" -> "첫 여정의 지형 기준점";
            case "turnbound:landmark/capital_valley_tower" -> "Drabyel로 향하는 길의 큰 랜드마크";
            case "turnbound:landmark/warning_cave" -> "길에서 벗어난 위험한 동굴";
            case "turnbound:landmark/explorers_guide_camp" -> "Drabyel 전 마지막 휴식 지점";
            case "turnbound:region/avsal" -> "Drabyel 이후 이어지는 거대한 폐허";
            default -> "지도에 기록된 장소";
        };
    }

    private static void drawMapSurface(GuiGraphicsExtractor g, int x, int y, int size) {
        g.fill(x - 2, y - 2, x + size + 2, y + size + 2, 0xFF8B7B60);
        g.fill(x, y, x + size, y + size, 0xFF24251F);
        for (int i = 1; i < 6; i++) {
            int at = i * size / 6;
            g.fill(x + at, y, x + at + 1, y + size, 0x553F4037);
            g.fill(x, y + at, x + size, y + at + 1, 0x553F4037);
        }
        g.fill(x + size - 15, y + 5, x + size - 14, y + 17, 0xFFB6AA8B);
        g.fill(x + size - 18, y + 8, x + size - 11, y + 9, 0xFFB6AA8B);
    }

    private static void drawRouteNetwork(
            GuiGraphicsExtractor g,
            int mapX,
            int mapY,
            int mapSize,
            Viewport view,
            List<DrehmalWorldProfile.Anchor> anchors
    ) {
        drawRoute(g, mapX, mapY, mapSize, view, anchors,
                "turnbound:region/stasis_facility", "turnbound:landmark/primal_caverns", 0x887F745A);
        drawRoute(g, mapX, mapY, mapSize, view, anchors,
                "turnbound:landmark/primal_caverns", "turnbound:landmark/capital_valley_tower", 0xAA9D8458);
        drawRoute(g, mapX, mapY, mapSize, view, anchors,
                "turnbound:landmark/capital_valley_tower", "turnbound:landmark/explorers_guide_camp", 0xAA9D8458);
        drawRoute(g, mapX, mapY, mapSize, view, anchors,
                "turnbound:landmark/explorers_guide_camp", "turnbound:hub/new_drabyel", 0xAA9D8458);
        drawRoute(g, mapX, mapY, mapSize, view, anchors,
                "turnbound:landmark/capital_valley_tower", "turnbound:landmark/warning_cave", 0x777A604C);
        drawRoute(g, mapX, mapY, mapSize, view, anchors,
                "turnbound:hub/new_drabyel", "turnbound:region/avsal", 0x887F745A);
    }

    private static void drawRoute(
            GuiGraphicsExtractor g,
            int mapX,
            int mapY,
            int mapSize,
            Viewport view,
            List<DrehmalWorldProfile.Anchor> anchors,
            String fromId,
            String toId,
            int color
    ) {
        DrehmalWorldProfile.Anchor from = anchor(anchors, fromId);
        DrehmalWorldProfile.Anchor to = anchor(anchors, toId);
        if (from == null || to == null || !inside(from.x(), from.z(), view) || !inside(to.x(), to.z(), view)) return;
        int x0 = mapX + worldToMap(from.x(), view.minX, view.span, mapSize);
        int y0 = mapY + worldToMap(from.z(), view.minZ, view.span, mapSize);
        int x1 = mapX + worldToMap(to.x(), view.minX, view.span, mapSize);
        int y1 = mapY + worldToMap(to.z(), view.minZ, view.span, mapSize);
        int steps = Math.max(1, Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)));
        for (int i = 0; i <= steps; i++) {
            double t = i / (double)steps;
            int x = (int)Math.round(x0 + (x1 - x0) * t);
            int y = (int)Math.round(y0 + (y1 - y0) * t);
            g.fill(x, y, x + 2, y + 2, color);
        }
    }

    private static DrehmalWorldProfile.Anchor anchor(List<DrehmalWorldProfile.Anchor> anchors, String locator) {
        for (DrehmalWorldProfile.Anchor anchor : anchors) if (locator.equals(anchor.locator())) return anchor;
        return null;
    }

    private static void drawObjectiveMarker(GuiGraphicsExtractor g, int cx, int cy) {
        g.fill(cx - 6, cy - 1, cx + 7, cy + 2, 0xEEFFC857);
        g.fill(cx - 1, cy - 6, cx + 2, cy + 7, 0xEEFFC857);
        g.fill(cx - 2, cy - 2, cx + 3, cy + 3, 0xFF24251F);
    }

    private static void drawMapArrow(GuiGraphicsExtractor g, int cx, int cy, float yaw, int color, boolean backdrop) {
        if (backdrop) g.fill(cx - 5, cy - 5, cx + 6, cy + 6, 0xB8111317);
        int dir = Math.floorMod(Math.round(yaw / 45.0F), 8);
        int[] vx = {0, -1, -1, -1, 0, 1, 1, 1};
        int[] vy = {1, 1, 0, -1, -1, -1, 0, 1};
        int dx = vx[dir], dy = vy[dir];
        for (int t = -2; t <= 2; t++) {
            int x = cx + dx * t, y = cy + dy * t;
            g.fill(x - 1, y - 1, x + 2, y + 2, color);
        }
        int hx = cx + dx * 4, hy = cy + dy * 4;
        g.fill(hx - 1, hy - 1, hx + 2, hy + 2, color);
        int px = -dy, py = dx;
        int wingX = hx - dx * 2, wingY = hy - dy * 2;
        g.fill(wingX + px - 1, wingY + py - 1, wingX + px + 2, wingY + py + 2, color);
        g.fill(wingX - px - 1, wingY - py - 1, wingX - px + 2, wingY - py + 2, color);
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
