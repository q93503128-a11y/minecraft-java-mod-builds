package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.world.FieldUiSnapshot;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.client.gui.GuiLayer;
import org.jetbrains.annotations.NotNull;

/**
 * Terrain-readable Drehmal exploration minimap.
 *
 * <p>Unlike the retired Aster map, this layer never projects authored source anchors as exact local positions.
 * The only non-player marker is the server-authored navigation target, whose production rule already requires
 * verified 26.2 coordinates.</p>
 */
public final class DrehmalMinimapLayer implements GuiLayer {
    private static final int TEXT = TurnboundUiTokens.TEXT_PRIMARY;
    private static final int MUTED = TurnboundUiTokens.TEXT_SECONDARY;
    private static final int TARGET = TurnboundUiTokens.ACCENT;

    private static final int GRID = 40;
    private static final int CELL = 2;
    private static final int STEP = 4;
    private static final int MAP_SIZE = GRID * CELL;
    private static final int[][] TERRAIN = new int[GRID][GRID];
    private static boolean visible = true;
    private static int cachedCenterX = Integer.MIN_VALUE;
    private static int cachedCenterZ = Integer.MIN_VALUE;
    private static int cachedTick = Integer.MIN_VALUE;

    public static void toggleVisible() { visible = !visible; }
    public static boolean visible() { return visible; }

    @Override
    public void render(@NotNull GuiGraphicsExtractor graphics, DeltaTracker tracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!visible || minecraft.player == null || minecraft.level == null || minecraft.gui.screen() != null) return;
        if (ClientPresentationTransition.fieldPresentationSuppressed()) return;

        FieldUiSnapshot field = ClientFieldState.snapshot();
        if (!field.active() || field.mode() == FieldUiSnapshot.Mode.LOADING) return;

        int panelW = MAP_SIZE + 22;
        int panelH = MAP_SIZE + 46;
        int x = TurnboundUiTokens.S;
        int y = TurnboundUiTokens.S;
        TurnboundUiSkin.panel(graphics, x, y, panelW, panelH);
        graphics.text(minecraft.font, Component.literal("M 지도 · N 숨김"), x + 7, y + 7, TEXT, false);

        int mapX = x + (panelW - MAP_SIZE) / 2;
        int mapY = y + 20;
        refreshTerrain(minecraft);
        for (int gz = 0; gz < GRID; gz++) {
            for (int gx = 0; gx < GRID; gx++) {
                int sx = mapX + gx * CELL;
                int sy = mapY + gz * CELL;
                graphics.fill(sx, sy, sx + CELL, sy + CELL, TERRAIN[gx][gz]);
            }
        }

        double px = minecraft.player.position().x;
        double pz = minecraft.player.position().z;
        double radius = GRID * STEP / 2.0;
        DrehmalMinimapProjection.Marker target = DrehmalMinimapProjection.navigation(
                field.navigation(), px, pz, Math.max(STEP, radius - STEP * 4.0));
        if (target != null) {
            int sx = mapX + MAP_SIZE / 2 + (int)Math.round(target.dx() / STEP * CELL);
            int sy = mapY + MAP_SIZE / 2 + (int)Math.round(target.dz() / STEP * CELL);
            if (target.clamped()) {
                float targetYaw = (float)Math.toDegrees(Math.atan2(-target.dx(), target.dz()));
                drawArrow(graphics, sx, sy, targetYaw, TARGET, false);
            } else {
                drawTarget(graphics, sx, sy);
            }
        }

        drawArrow(graphics, mapX + MAP_SIZE / 2, mapY + MAP_SIZE / 2, minecraft.player.getYRot(), 0xFFFFFFFF, true);
        graphics.text(minecraft.font, Component.literal("N"), mapX + MAP_SIZE - 9, mapY + 3, 0xEFFFFFFF, true);

        int legendY = mapY + MAP_SIZE + 4;
        drawTarget(graphics, mapX + 3, legendY + 4);
        graphics.text(minecraft.font, Component.literal("추적 목표"), mapX + 10, legendY, MUTED, false);

        String footer;
        if (target != null) {
            footer = target.label() + " · " + target.distance() + "m";
        } else if (!field.locationTitle().isBlank()) {
            footer = field.locationTitle();
        } else if (field.navigation().active()) {
            int distance = (int)Math.round(Math.hypot(field.navigation().x() - px, field.navigation().z() - pz));
            footer = field.navigation().label() + " · " + distance + "m";
        } else {
            footer = "주변 탐색";
        }
        graphics.text(minecraft.font, Component.literal(fit(minecraft, footer, panelW - 14)),
                x + 7, y + panelH - 11, MUTED, false);
    }

    private static void refreshTerrain(Minecraft minecraft) {
        int centerX = floorToStep(minecraft.player.getX());
        int centerZ = floorToStep(minecraft.player.getZ());
        int tick = minecraft.player.tickCount;
        if (centerX == cachedCenterX && centerZ == cachedCenterZ && tick - cachedTick < 12) return;
        cachedCenterX = centerX;
        cachedCenterZ = centerZ;
        cachedTick = tick;
        int startX = centerX - (GRID / 2) * STEP;
        int startZ = centerZ - (GRID / 2) * STEP;
        int playerY = (int)Math.floor(minecraft.player.getY());
        for (int gz = 0; gz < GRID; gz++) for (int gx = 0; gx < GRID; gx++) {
            int worldX = startX + gx * STEP;
            int worldZ = startZ + gz * STEP;
            TERRAIN[gx][gz] = terrainColor(minecraft, worldX, worldZ, playerY);
        }
    }

    private static int terrainColor(Minecraft minecraft, int x, int z, int playerY) {
        if (!minecraft.level.hasChunk(x >> 4, z >> 4)) return 0xFF171A1F;
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

    private static void drawTarget(GuiGraphicsExtractor graphics, int cx, int cy) {
        graphics.fill(cx - 3, cy - 3, cx + 4, cy + 4, 0xDD111317);
        graphics.fill(cx - 1, cy - 4, cx + 2, cy + 5, TARGET);
        graphics.fill(cx - 4, cy - 1, cx + 5, cy + 2, TARGET);
    }

    private static void drawArrow(GuiGraphicsExtractor graphics, int cx, int cy, float yaw, int color, boolean backdrop) {
        int dir = Math.floorMod(Math.round(yaw / 45.0F), 8);
        int[] vx = {0, -1, -1, -1, 0, 1, 1, 1};
        int[] vy = {1, 1, 0, -1, -1, -1, 0, 1};
        int dx = vx[dir], dy = vy[dir];
        int px = -dy, py = dx;

        // Minimal pointer: thin shaft + two head pixels. No black backdrop tile.
        for (int t = -1; t <= 3; t++) drawArrowPixel(graphics, cx + dx * t, cy + dy * t, color);
        int wingX = cx + dx;
        int wingY = cy + dy;
        drawArrowPixel(graphics, wingX + px, wingY + py, color);
        drawArrowPixel(graphics, wingX - px, wingY - py, color);
    }

    private static void drawArrowPixel(GuiGraphicsExtractor graphics, int x, int y, int color) {
        graphics.fill(x, y, x + 2, y + 2, color);
    }

    private static int floorToStep(double value) { return Math.floorDiv((int)Math.floor(value), STEP) * STEP; }

    private static String fit(Minecraft minecraft, String value, int maxWidth) {
        if (minecraft.font.width(value) <= maxWidth) return value;
        int end = value.length();
        while (end > 1 && minecraft.font.width(value.substring(0, end) + "…") > maxWidth) end--;
        return value.substring(0, Math.max(1, end)) + "…";
    }
}
