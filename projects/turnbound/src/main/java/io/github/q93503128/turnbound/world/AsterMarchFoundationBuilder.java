package io.github.q93503128.turnbound.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Preset-independent authored foundation for Aster March.
 *
 * Only canonical playable regions are normalized. Radia uses the shared coastal terrain plan;
 * legacy field chapters retain WORLD_BASE_Y until their individual authored-terrain migrations.
 * This replaces the old 1041x1041 blanket grass plane and avoids doing work in unreachable space.
 */
public final class AsterMarchFoundationBuilder {
    private static final int COLUMNS_PER_TICK = 3200;
    private static final BlockPos MARKER_A = new BlockPos(-510, 54, -510);
    private static final BlockPos MARKER_B = new BlockPos(-509, 54, -510);
    private static final BlockPos MARKER_C = new BlockPos(-508, 54, -510);
    private static final BlockPos MARKER_D = new BlockPos(-507, 54, -510);
    private static final Map<ServerLevel, State> STATES = new WeakHashMap<>();

    private record Rect(int minX, int maxX, int minZ, int maxZ) {
        int width() { return maxX - minX + 1; }
        int height() { return maxZ - minZ + 1; }
        int size() { return width() * height(); }
    }

    private static final List<Rect> REGIONS = List.of(
            rect(AsterMarchRegionCatalog.RADIA),
            rect(AsterMarchRegionCatalog.SOUTHGATE),
            rect(AsterMarchRegionCatalog.GLOAMWOOD),
            rect(AsterMarchRegionCatalog.AQUEDUCT),
            rect(AsterMarchRegionCatalog.QUARRY),
            rect(AsterMarchRegionCatalog.OLD_RELAY)
    );
    private static final int TOTAL_COLUMNS = REGIONS.stream().mapToInt(Rect::size).sum();

    private AsterMarchFoundationBuilder() {}

    public static boolean ready(ServerLevel level) {
        return level.getBlockState(MARKER_A).is(Blocks.LODESTONE)
                && level.getBlockState(MARKER_B).is(Blocks.EMERALD_BLOCK)
                && level.getBlockState(MARKER_C).is(Blocks.GOLD_BLOCK)
                && level.getBlockState(MARKER_D).is(Blocks.DIAMOND_BLOCK);
    }

    public static boolean step(ServerLevel level, ServerPlayer player) {
        if (ready(level)) return true;
        State state = STATES.computeIfAbsent(level, ignored -> new State());
        holdPlayer(player);

        int processed = 0;
        while (processed < COLUMNS_PER_TICK && state.regionIndex < REGIONS.size()) {
            Rect region = REGIONS.get(state.regionIndex);
            int x = region.minX + state.offset % region.width();
            int z = region.minZ + state.offset / region.width();
            authorColumn(level, x, z);

            state.offset++;
            state.completed++;
            processed++;
            if (state.offset >= region.size()) {
                state.regionIndex++;
                state.offset = 0;
            }
        }

        int percent = Math.min(99, (int)Math.floor(state.completed * 100.0 / TOTAL_COLUMNS));
        if (percent != state.lastPercent && (percent == 0 || percent >= state.lastPercent + 2)) {
            state.lastPercent = percent;
            FieldNetwork.sync(player, FieldUiSnapshot.loading("아스테르 지형 생성", percent));
        }
        if (state.regionIndex < REGIONS.size()) return false;

        level.setBlock(MARKER_A, Blocks.LODESTONE.defaultBlockState(), 2);
        level.setBlock(MARKER_B, Blocks.EMERALD_BLOCK.defaultBlockState(), 2);
        level.setBlock(MARKER_C, Blocks.GOLD_BLOCK.defaultBlockState(), 2);
        level.setBlock(MARKER_D, Blocks.DIAMOND_BLOCK.defaultBlockState(), 2);
        STATES.remove(level);
        FieldNetwork.sync(player, FieldUiSnapshot.loading("아스테르 배치", 100));
        return true;
    }

    private static void authorColumn(ServerLevel level, int x, int z) {
        AsterMarchTerrainPlan.Column plan = AsterMarchTerrainPlan.column(x, z);
        int originalSurface = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;

        switch (plan.kind()) {
            case RADIA_WATER -> waterColumn(level, x, z, originalSurface);
            case RADIA_LAND -> landColumn(level, x, z, plan.surfaceY(), originalSurface);
            case FIELD -> fieldColumn(level, x, z, originalSurface);
        }
    }

    private static void fieldColumn(ServerLevel level, int x, int z, int originalSurface) {
        int y = AsterMarchTerrainPlan.WORLD_BASE_Y;
        for (int fillY = y - 4; fillY < y; fillY++) {
            level.setBlock(new BlockPos(x, fillY, z), Blocks.DIRT.defaultBlockState(), 2);
        }
        level.setBlock(new BlockPos(x, y, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        clearAbove(level, x, z, y + 1, Math.max(y + 1, originalSurface + 2));
    }

    private static void landColumn(ServerLevel level, int x, int z, int surfaceY, int originalSurface) {
        int stoneBottom = AsterMarchTerrainPlan.RADIA_FLOOR_Y;
        for (int y = stoneBottom; y <= surfaceY - 4; y++) {
            level.setBlock(new BlockPos(x, y, z),
                    ((x * 31 + z * 17 + y) & 7) == 0 ? Blocks.ANDESITE.defaultBlockState() : Blocks.STONE.defaultBlockState(), 2);
        }
        for (int y = Math.max(stoneBottom, surfaceY - 3); y < surfaceY; y++) {
            level.setBlock(new BlockPos(x, y, z), Blocks.DIRT.defaultBlockState(), 2);
        }
        level.setBlock(new BlockPos(x, surfaceY, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        clearAbove(level, x, z, surfaceY + 1, Math.max(surfaceY + 6, originalSurface + 2));
    }

    private static void waterColumn(ServerLevel level, int x, int z, int originalSurface) {
        int floor = AsterMarchTerrainPlan.RADIA_FLOOR_Y;
        level.setBlock(new BlockPos(x, floor - 1, z), Blocks.STONE.defaultBlockState(), 2);
        level.setBlock(new BlockPos(x, floor, z),
                ((x + z) & 3) == 0 ? Blocks.GRAVEL.defaultBlockState() : Blocks.SAND.defaultBlockState(), 2);
        clearAbove(level, x, z, floor + 1, Math.max(AsterMarchTerrainPlan.RADIA_SEA_Y + 1, originalSurface + 2));
        for (int y = floor + 1; y <= AsterMarchTerrainPlan.RADIA_SEA_Y; y++) {
            level.setBlock(new BlockPos(x, y, z), Blocks.WATER.defaultBlockState(), 2);
        }
    }

    private static void clearAbove(ServerLevel level, int x, int z, int fromY, int toY) {
        for (int y = fromY; y <= toY; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
    }

    private static Rect rect(AsterMarchRegionCatalog.Region region) {
        return new Rect(region.minX(), region.maxX(), region.minZ(), region.maxZ());
    }

    private static void holdPlayer(ServerPlayer player) {
        player.setNoGravity(true);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        player.setPos(0.5, 250.0, 20.5);
    }

    private static final class State {
        private int regionIndex;
        private int offset;
        private int completed;
        private int lastPercent = -2;
    }
}
