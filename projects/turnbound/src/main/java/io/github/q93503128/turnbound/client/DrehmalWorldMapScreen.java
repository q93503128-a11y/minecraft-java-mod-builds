package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.world.DrehmalFastTravelCatalog;
import io.github.q93503128.turnbound.world.DrehmalMapPlacementCatalog;
import io.github.q93503128.turnbound.world.DrehmalWorldProfile;
import io.github.q93503128.turnbound.world.FieldUiSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
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

    private static final double MIN_ZOOM = 0.35D;
    private static final double MAX_ZOOM = 256.0D;
    private static final double ZOOM_STEP = 1.35D;

    private int left, top, panelWidth, panelHeight;
    private int mapViewX, mapViewY, mapViewSize;
    private double zoom = 2.4D;
    private double viewCenterX = Double.NaN;
    private double viewCenterZ = Double.NaN;
    private Bounds activeBounds;
    private Viewport activeViewport;
    private int[][] terrainCache = new int[0][0];
    private int terrainSamples;
    private double terrainMinX = Double.NaN;
    private double terrainMinZ = Double.NaN;
    private double terrainSpan = Double.NaN;
    private int terrainTick = Integer.MIN_VALUE;
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

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0.0D) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);

        double nextZoom = clamp(zoom * (scrollY > 0.0D ? ZOOM_STEP : 1.0D / ZOOM_STEP), MIN_ZOOM, MAX_ZOOM);
        if (Math.abs(nextZoom - zoom) < 0.000001D) return true;

        if (activeBounds != null && activeViewport != null && mapViewSize > 0 && insideMap(mouseX, mouseY)) {
            double nx = clamp((mouseX - mapViewX) / mapViewSize, 0.0D, 1.0D);
            double nz = clamp((mouseY - mapViewY) / mapViewSize, 0.0D, 1.0D);
            double worldX = activeViewport.minX + nx * activeViewport.span;
            double worldZ = activeViewport.minZ + nz * activeViewport.span;
            zoom = nextZoom;
            double nextSpan = activeBounds.span / zoom;
            if (zoom <= 1.0D) {
                viewCenterX = activeBounds.minX + activeBounds.span * 0.5D;
                viewCenterZ = activeBounds.minZ + activeBounds.span * 0.5D;
            } else {
                viewCenterX = worldX - (nx - 0.5D) * nextSpan;
                viewCenterZ = worldZ - (nz - 0.5D) * nextSpan;
                clampCenter(activeBounds, nextSpan);
            }
        } else {
            zoom = nextZoom;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT || zoom <= 1.0D
                || activeBounds == null || activeViewport == null || mapViewSize <= 0 || !insideMap(event.x(), event.y())) {
            return super.mouseDragged(event, deltaX, deltaY);
        }
        double worldPerPixel = activeViewport.span / mapViewSize;
        viewCenterX -= deltaX * worldPerPixel;
        viewCenterZ -= deltaY * worldPerPixel;
        clampCenter(activeBounds, activeViewport.span);
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
                ? scaleLabel() + " · 휠/드래그"
                : "휠: 광역 개요 ↔ 블록 단위 · 드래그: 지도 이동 · " + scaleLabel() + " · M/ESC 닫기";
        int helpX = left + panelWidth - 12 - font.width(help);
        if (helpX > left + 90) {
            graphics.text(font, Component.literal(help), helpX, top + 11, SECONDARY, false);
        }
        boolean showSubtitle = !compact || panelHeight >= 285;
        if (showSubtitle) {
            graphics.text(font, Component.literal("축소하면 지역 관계를, 확대하면 블록 격자와 세부 위치를 확인할 수 있습니다."), left + 12, top + 27, SECONDARY, false);
        }

        boolean wide = panelWidth >= 320 && panelHeight >= 190;
        int mapY = top + (showSubtitle ? 43 : 30);
        int mapX = left + 12;
        int infoReserve = wide ? Math.min(210, Math.max(150, panelWidth / 4)) : 0;
        int bottomReserve = wide ? 12 : compact ? 68 : 108;
        int availableW = panelWidth - 24 - infoReserve - (wide ? 10 : 0);
        int availableH = top + panelHeight - bottomReserve - mapY;
        int mapSize = Math.max(64, Math.min(availableW, availableH));
        mapViewX = mapX;
        mapViewY = mapY;
        mapViewSize = mapSize;

        drawMapSurface(graphics, mapX, mapY, mapSize);

        Bounds bounds = bounds(anchors, px, pz);
        Viewport view = viewport(bounds, px, pz);
        activeBounds = bounds;
        activeViewport = view;
        drawLoadedTerrain(graphics, minecraft, mapX, mapY, mapSize, view);
        drawBlockGrid(graphics, mapX, mapY, mapSize, view);
        drawRouteNetwork(graphics, mapX, mapY, mapSize, view);

        DrehmalWorldProfile.Anchor hovered = null;
        double hoveredDistance = Double.MAX_VALUE;
        for (DrehmalWorldProfile.Anchor anchor : anchors) {
            if (!inside(anchor.x(), anchor.z(), view)) continue;
            int sx = mapX + worldToMap(anchor.x(), view.minX, view.span, mapSize);
            int sy = mapY + worldToMap(anchor.z(), view.minZ, view.span, mapSize);
            drawMarker(graphics, sx, sy, anchor.kind());
            if (mapSize >= 180) {
                int labelW = "LANDMARK".equals(anchor.kind()) ? 72 : 92;
                String shortLabel = UiTextLayout.fit(label(anchor), labelW);
                graphics.text(font, Component.literal(shortLabel), sx + 7, sy - 4, markerColor(anchor.kind()), false);
            }
            double dx = mouseX - sx, dy = mouseY - sy, distance = dx * dx + dy * dy;
            if (distance <= 100.0 && distance < hoveredDistance) {
                hoveredDistance = distance;
                hovered = anchor;
            }
        }

        for (FieldUiSnapshot.MapPoint point : ClientFieldState.snapshot().mapPoints()) {
            if (!point.active() || !inside(point.x(), point.z(), view)) continue;
            int sx = mapX + worldToMap(point.x(), view.minX, view.span, mapSize);
            int sy = mapY + worldToMap(point.z(), view.minZ, view.span, mapSize);
            drawMapPoint(graphics, sx, sy, point.kind(), point.objective());
            if (point.objective() || view.span <= 720.0D) {
                graphics.text(font, Component.literal(UiTextLayout.fit(point.label(), point.objective() ? 112 : 88)), sx + 6, sy - 4, mapPointColor(point.kind()), false);
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
            if (mapSize >= 150) graphics.text(font, Component.literal(UiTextLayout.fit(navigation.label(), 112)), nsx + 8, nsy - 4, GOLD, false);
        }

        for (FieldUiSnapshot.Travel travel : ClientFieldState.snapshot().travels()) {
            if (!travel.unlocked()) continue;
            DrehmalFastTravelCatalog.Node node = DrehmalFastTravelCatalog.node(travel.id());
            if (node == null || !inside(node.mapX(), node.mapZ(), view)) continue;
            int tx = mapX + worldToMap(node.mapX(), view.minX, view.span, mapSize);
            int ty = mapY + worldToMap(node.mapZ(), view.minZ, view.span, mapSize);
            drawFastTravelMarker(graphics, tx, ty, travel.current());
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
        int infoCursor = infoY + 24;
        if (infoH >= 70) {
            int visibleBlocks = Math.max(1, (int)Math.round(view.span));
            String scale = "보기 · " + scaleLabel() + " · 화면 폭 약 " + visibleBlocks + "블록";
            graphics.text(font, Component.literal(UiTextLayout.fit(scale, infoW - 14)),
                    infoX + 7, infoCursor, MUTED, false);
            infoCursor += 16;
        }
        if (navigation != null && navigation.active() && infoH >= 64) {
            int objectiveDistance = (int)Math.round(Math.hypot(navigation.x() - px, navigation.z() - pz));
            graphics.text(font, Component.literal("현재 목표"), infoX + 7, infoCursor, GOLD, true);
            infoCursor += 15;
            graphics.text(font, Component.literal(UiTextLayout.fit(navigation.label() + " · " + objectiveDistance + "블록", infoW - 14)),
                    infoX + 7, infoCursor, TEXT, false);
            infoCursor += 20;
        }
        if (focus != null && infoCursor + 44 < infoY + infoH) {
            graphics.text(font, Component.literal(UiTextLayout.fit(label(focus), infoW - 14)),
                    infoX + 7, infoCursor, markerColor(focus.kind()), true);
            infoCursor += 16;
            graphics.text(font, Component.literal(UiTextLayout.fit(description(focus), infoW - 14)),
                    infoX + 7, infoCursor, SECONDARY, false);
            infoCursor += 16;
            int distance = (int)Math.round(Math.hypot(focus.x() - px, focus.z() - pz));
            String distanceLine = "약 " + distance + "블록 · " + kindLabel(focus.kind());
            graphics.text(font, Component.literal(UiTextLayout.fit(distanceLine, infoW - 14)),
                    infoX + 7, infoCursor, MUTED, false);
            infoCursor += 23;
        }

        if (wide && infoCursor + 66 < infoY + infoH) {
            graphics.text(font, Component.literal("◆ 거점"), infoX + 9, infoCursor, GOLD, false);
            graphics.text(font, Component.literal("■ 지역"), infoX + 9, infoCursor + 17, BLUE, false);
            graphics.text(font, Component.literal("● 랜드마크"), infoX + 9, infoCursor + 34, GREEN, false);
            graphics.text(font, Component.literal("◇ 빠른 이동"), infoX + 9, infoCursor + 51, GOLD, false);
            infoCursor += 76;
        }
        if (wide && infoCursor + 30 < infoY + infoH) {
            String note = "발견한 ◇ 거점은 역참에서 목적지로 선택할 수 있습니다. 노선은 지역 단계에서 대략 표시되며, 블록 단계에서는 좌표와 격자를 기준으로 세부 위치를 확인합니다.";
            int noteY = infoCursor;
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
        double fullCenterX = full.minX + full.span * 0.5D;
        double fullCenterZ = full.minZ + full.span * 0.5D;

        if (Double.isNaN(viewCenterX) || Double.isNaN(viewCenterZ)) {
            boolean playerNearMap = px >= full.minX && px <= full.minX + full.span
                    && pz >= full.minZ && pz <= full.minZ + full.span;
            viewCenterX = playerNearMap ? px : fullCenterX;
            viewCenterZ = playerNearMap ? pz : fullCenterZ;
        }

        if (zoom <= 1.0D) {
            viewCenterX = fullCenterX;
            viewCenterZ = fullCenterZ;
            return new Viewport(fullCenterX - span * 0.5D, fullCenterZ - span * 0.5D, span);
        }

        clampCenter(full, span);
        return new Viewport(viewCenterX - span * 0.5D, viewCenterZ - span * 0.5D, span);
    }

    private void clampCenter(Bounds full, double span) {
        double half = span * 0.5D;
        if (span >= full.span) {
            viewCenterX = full.minX + full.span * 0.5D;
            viewCenterZ = full.minZ + full.span * 0.5D;
            return;
        }
        viewCenterX = clamp(viewCenterX, full.minX + half, full.minX + full.span - half);
        viewCenterZ = clamp(viewCenterZ, full.minZ + half, full.minZ + full.span - half);
    }

    private boolean insideMap(double x, double y) {
        return mapViewSize > 0 && x >= mapViewX && x <= mapViewX + mapViewSize
                && y >= mapViewY && y <= mapViewY + mapViewSize;
    }

    private String scaleLabel() {
        if (zoom < 0.75D) return "광역 개요";
        if (zoom < 2.5D) return "지역";
        if (zoom < 18.0D) return "세부";
        return "블록 단위";
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
            case "turnbound:hub/new_drabyel" -> "캐피털 밸리 북쪽의 안전 거점. 정비와 다음 여정 준비를 할 수 있습니다.";
            case "turnbound:region/stasis_facility" -> "캐피털 밸리 동쪽에 남은 오래된 시설 지대입니다.";
            case "turnbound:landmark/primal_caverns" -> "캐피털 밸리 동부의 큰 동굴 지형으로, 길을 잡는 기준점이 됩니다.";
            case "turnbound:landmark/capital_valley_tower" -> "뉴 드라비엘로 향하는 길과 주변 계곡을 굽어보는 높은 탑입니다.";
            case "turnbound:landmark/warning_cave" -> "주요 길에서 벗어난 위험 지역입니다. 강한 적의 흔적이 남아 있습니다.";
            case "turnbound:landmark/explorers_guide_camp" -> "뉴 드라비엘에 닿기 전 쉬어 갈 수 있는 탐험가 야영지입니다.";
            case "turnbound:region/avsal" -> "뉴 드라비엘 서쪽에 펼쳐진 대도시 폐허. 외곽부터 조사할 수 있습니다.";
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

    private void drawLoadedTerrain(
            GuiGraphicsExtractor g, Minecraft minecraft,
            int mapX, int mapY, int mapSize, Viewport view
    ) {
        if (minecraft.level == null || minecraft.player == null) return;
        int samples = Math.max(24, Math.min(52, Math.max(1, mapSize / 7)));
        int tick = minecraft.player.tickCount;
        double cellWorld = view.span / samples;
        boolean changed = terrainSamples != samples
                || Double.isNaN(terrainMinX)
                || Math.abs(terrainMinX - view.minX) > cellWorld * 0.35D
                || Math.abs(terrainMinZ - view.minZ) > cellWorld * 0.35D
                || Math.abs(terrainSpan - view.span) > cellWorld * 0.35D;
        if (changed || tick - terrainTick >= 16) {
            terrainSamples = samples;
            terrainMinX = view.minX;
            terrainMinZ = view.minZ;
            terrainSpan = view.span;
            terrainTick = tick;
            terrainCache = new int[samples][samples];
            int playerY = (int)Math.floor(minecraft.player.getY());
            for (int gz = 0; gz < samples; gz++) for (int gx = 0; gx < samples; gx++) {
                int worldX = (int)Math.floor(view.minX + (gx + 0.5D) * cellWorld);
                int worldZ = (int)Math.floor(view.minZ + (gz + 0.5D) * cellWorld);
                terrainCache[gx][gz] = terrainColor(minecraft, worldX, worldZ, playerY);
            }
        }
        int cellPx = Math.max(1, (int)Math.ceil(mapSize / (double)samples));
        for (int gz = 0; gz < samples; gz++) for (int gx = 0; gx < samples; gx++) {
            int color = terrainCache[gx][gz];
            if (color == 0) continue;
            int sx = mapX + gx * mapSize / samples;
            int sy = mapY + gz * mapSize / samples;
            int ex = mapX + Math.min(mapSize, gx * mapSize / samples + cellPx + 1);
            int ey = mapY + Math.min(mapSize, gz * mapSize / samples + cellPx + 1);
            g.fill(sx, sy, ex, ey, color);
        }
    }

    private static int terrainColor(Minecraft minecraft, int x, int z, int playerY) {
        if (!minecraft.level.hasChunk(x >> 4, z >> 4)) return 0;
        int y = minecraft.level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = minecraft.level.getBlockState(pos);
        int base;
        if (!state.getFluidState().isEmpty()) base = 0xFF3979A9;
        else if (state.is(BlockTags.LEAVES)) base = 0xFF315F3D;
        else if (state.is(BlockTags.LOGS)) base = 0xFF694B33;
        else if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.PODZOL)) base = 0xFF5B8748;
        else if (state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.DIRT_PATH) || state.is(Blocks.MUD)) base = 0xFF796047;
        else if (state.is(Blocks.SAND) || state.is(Blocks.SANDSTONE)) base = 0xFFD6C58A;
        else if (state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.SNOW) || state.is(Blocks.ICE)) base = 0xFFE3ECF2;
        else if (state.is(Blocks.DEEPSLATE) || state.is(Blocks.DEEPSLATE_TILES) || state.is(Blocks.BLACKSTONE) || state.is(Blocks.OBSIDIAN)) base = 0xFF343A42;
        else if (state.is(Blocks.STONE) || state.is(Blocks.STONE_BRICKS) || state.is(Blocks.ANDESITE)
                || state.is(Blocks.POLISHED_ANDESITE) || state.is(Blocks.COBBLESTONE) || state.is(Blocks.GRAVEL)) base = 0xFF777D82;
        else if (state.is(Blocks.OAK_PLANKS) || state.is(Blocks.SPRUCE_PLANKS) || state.is(Blocks.DARK_OAK_PLANKS)) base = 0xFF9A7650;
        else base = y <= minecraft.level.getSeaLevel() + 1 ? 0xFF61717A : 0xFF6F7D64;
        int delta = Math.max(-7, Math.min(7, y - playerY));
        return shade(base, delta * 3);
    }

    private static int shade(int color, int delta) {
        int r = Math.max(0, Math.min(255, ((color >>> 16) & 0xFF) + delta));
        int g = Math.max(0, Math.min(255, ((color >>> 8) & 0xFF) + delta));
        int b = Math.max(0, Math.min(255, (color & 0xFF) + delta));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static void drawBlockGrid(GuiGraphicsExtractor g, int mapX, int mapY, int mapSize, Viewport view) {
        double pixelsPerBlock = mapSize / view.span;
        if (pixelsPerBlock < 2.0D) return;

        int minX = (int)Math.ceil(view.minX);
        int maxX = (int)Math.floor(view.minX + view.span);
        int minZ = (int)Math.ceil(view.minZ);
        int maxZ = (int)Math.floor(view.minZ + view.span);
        for (int x = minX; x <= maxX; x++) {
            int sx = mapX + worldToMap(x, view.minX, view.span, mapSize);
            int color = Math.floorMod(x, 16) == 0 ? 0x665F604F : 0x333F4037;
            g.fill(sx, mapY, sx + 1, mapY + mapSize, color);
        }
        for (int z = minZ; z <= maxZ; z++) {
            int sy = mapY + worldToMap(z, view.minZ, view.span, mapSize);
            int color = Math.floorMod(z, 16) == 0 ? 0x665F604F : 0x333F4037;
            g.fill(mapX, sy, mapX + mapSize, sy + 1, color);
        }
    }

    private static void drawRouteNetwork(
            GuiGraphicsExtractor g, int mapX, int mapY, int mapSize, Viewport view
    ) {
        for (DrehmalMapPlacementCatalog.Zone zone : DrehmalMapPlacementCatalog.plan().zones()) {
            int color = switch (zone.role()) {
                case "HUB_APPROACH" -> 0xCCB38A52;
                case "CHOICE_ELITE" -> 0xAA8C7258;
                case "BREATHING" -> 0xAA7F745A;
                default -> 0xBB9D8458;
            };
            List<DrehmalMapPlacementCatalog.Seed> points = zone.corridor();
            for (int i = 1; i < points.size(); i++) {
                drawRouteSegment(g, mapX, mapY, mapSize, view, points.get(i - 1), points.get(i), color);
            }
        }
    }

    private static void drawRouteSegment(
            GuiGraphicsExtractor g, int mapX, int mapY, int mapSize, Viewport view,
            DrehmalMapPlacementCatalog.Seed from, DrehmalMapPlacementCatalog.Seed to, int color
    ) {
        double minX = Math.min(from.x(), to.x()), maxX = Math.max(from.x(), to.x());
        double minZ = Math.min(from.z(), to.z()), maxZ = Math.max(from.z(), to.z());
        if (maxX < view.minX || minX > view.minX + view.span
                || maxZ < view.minZ || minZ > view.minZ + view.span) return;
        double dx = to.x() - from.x(), dz = to.z() - from.z();
        double length = Math.max(1.0D, Math.hypot(dx, dz));
        double worldPerPixel = Math.max(0.25D, view.span / Math.max(1, mapSize));
        int steps = Math.max(1, Math.min(320, (int)Math.ceil(length / Math.max(1.0D, worldPerPixel * 1.5D))));
        for (int i = 0; i <= steps; i++) {
            double t = i / (double)steps;
            double wx = from.x() + dx * t, wz = from.z() + dz * t;
            if (!inside(wx, wz, view)) continue;
            int x = mapX + worldToMap(wx, view.minX, view.span, mapSize);
            int y = mapY + worldToMap(wz, view.minZ, view.span, mapSize);
            g.fill(x - 1, y - 1, x + 3, y + 3, 0x66201D18);
            g.fill(x, y, x + 2, y + 2, color);
        }
    }

    private static DrehmalWorldProfile.Anchor anchor(List<DrehmalWorldProfile.Anchor> anchors, String locator) {
        for (DrehmalWorldProfile.Anchor anchor : anchors) if (locator.equals(anchor.locator())) return anchor;
        return null;
    }

    private static void drawMapPoint(GuiGraphicsExtractor g, int cx, int cy, String kind, boolean objective) {
        int color = mapPointColor(kind);
        if (objective || "QUEST".equals(kind) || "SECRET".equals(kind)) {
            g.fill(cx - 4, cy - 1, cx + 5, cy + 2, 0xDD111317);
            g.fill(cx - 1, cy - 4, cx + 2, cy + 5, color);
            return;
        }
        g.fill(cx - 3, cy - 3, cx + 4, cy + 4, 0xBB111317);
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, color);
    }

    private static int mapPointColor(String kind) {
        return switch (kind) { case "QUEST", "SECRET" -> GOLD; case "SERVICE" -> BLUE; default -> GREEN; };
    }

    private static void drawObjectiveMarker(GuiGraphicsExtractor g, int cx, int cy) {
        g.fill(cx - 6, cy - 1, cx + 7, cy + 2, 0xEEFFC857);
        g.fill(cx - 1, cy - 6, cx + 2, cy + 7, 0xEEFFC857);
        g.fill(cx - 2, cy - 2, cx + 3, cy + 3, 0xFF24251F);
    }

    private static void drawFastTravelMarker(GuiGraphicsExtractor g, int cx, int cy, boolean current) {
        int color = current ? GREEN : GOLD;
        g.fill(cx - 5, cy - 1, cx + 6, cy + 2, color);
        g.fill(cx - 1, cy - 5, cx + 2, cy + 6, color);
        g.fill(cx - 3, cy - 3, cx + 4, cy - 2, color);
        g.fill(cx - 3, cy + 3, cx + 4, cy + 4, color);
        g.fill(cx - 3, cy - 2, cx - 2, cy + 3, color);
        g.fill(cx + 3, cy - 2, cx + 4, cy + 3, color);
    }

    private static void drawMapArrow(GuiGraphicsExtractor g, int cx, int cy, float yaw, int color, boolean backdrop) {
        int dir = Math.floorMod(Math.round(yaw / 45.0F), 8);
        int[] vx = {0, -1, -1, -1, 0, 1, 1, 1};
        int[] vy = {1, 1, 0, -1, -1, -1, 0, 1};
        int dx = vx[dir], dy = vy[dir];
        int px = -dy, py = dx;

        for (int t = -1; t <= 3; t++) drawMapArrowPixel(g, cx + dx * t, cy + dy * t, color);
        int wingX = cx + dx;
        int wingY = cy + dy;
        drawMapArrowPixel(g, wingX + px, wingY + py, color);
        drawMapArrowPixel(g, wingX - px, wingY - py, color);
    }

    private static void drawMapArrowPixel(GuiGraphicsExtractor g, int x, int y, int color) {
        g.fill(x, y, x + 2, y + 2, color);
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
