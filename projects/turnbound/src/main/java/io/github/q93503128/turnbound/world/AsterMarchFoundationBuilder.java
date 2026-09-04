package io.github.q93503128.turnbound.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Makes the opening hub independent from the vanilla world preset.
 * Radia stays authored and level while a soft 32-block transition band prevents the old vertical world-cut walls.
 */
public final class AsterMarchFoundationBuilder {
    private static final int MIN_X = AsterMarchRegionCatalog.RADIA.minX();
    private static final int MAX_X = AsterMarchRegionCatalog.RADIA.maxX();
    private static final int MIN_Z = AsterMarchRegionCatalog.RADIA.minZ();
    private static final int MAX_Z = AsterMarchRegionCatalog.RADIA.maxZ();
    private static final int BLEND = 32;
    private static final int OUTER_MIN_X = MIN_X - BLEND;
    private static final int OUTER_MAX_X = MAX_X + BLEND;
    private static final int OUTER_MIN_Z = MIN_Z - BLEND;
    private static final int OUTER_MAX_Z = MAX_Z + BLEND;
    private static final int GROUND_Y = 65;
    private static final int COLUMNS_PER_TICK = 900;
    private static final BlockPos MARKER_A = new BlockPos(-127, 58, -111);
    private static final BlockPos MARKER_B = new BlockPos(-126, 58, -111);
    private static final BlockPos MARKER_C = new BlockPos(-125, 58, -111);
    private static final Map<ServerLevel, State> STATES = new WeakHashMap<>();

    private AsterMarchFoundationBuilder() {}

    public static boolean ready(ServerLevel level) {
        return level.getBlockState(MARKER_A).is(Blocks.LODESTONE)
                && level.getBlockState(MARKER_B).is(Blocks.EMERALD_BLOCK)
                && level.getBlockState(MARKER_C).is(Blocks.GOLD_BLOCK);
    }

    public static boolean step(ServerLevel level, ServerPlayer player) {
        if (ready(level)) return true;
        State state = STATES.computeIfAbsent(level, ignored -> new State());
        holdPlayer(player);
        int width = OUTER_MAX_X - OUTER_MIN_X + 1;
        int total = width * (OUTER_MAX_Z - OUTER_MIN_Z + 1);
        int processed = 0;
        while (processed < COLUMNS_PER_TICK && state.index < total) {
            int x = OUTER_MIN_X + (state.index % width);
            int z = OUTER_MIN_Z + (state.index / width);
            prepareColumn(level, x, z);
            state.index++;
            processed++;
        }
        int percent = Math.min(99, (int)Math.floor(state.index * 100.0 / total));
        if (percent != state.lastPercent && (percent == 0 || percent >= state.lastPercent + 4)) {
            state.lastPercent = percent;
            FieldNetwork.sync(player, FieldUiSnapshot.loading("라디아 지형 정리", percent));
        }
        if (state.index < total) return false;

        level.setBlock(MARKER_A, Blocks.LODESTONE.defaultBlockState(), 2);
        level.setBlock(MARKER_B, Blocks.EMERALD_BLOCK.defaultBlockState(), 2);
        level.setBlock(MARKER_C, Blocks.GOLD_BLOCK.defaultBlockState(), 2);
        STATES.remove(level);
        FieldNetwork.sync(player, FieldUiSnapshot.loading("아스테르 변경 배치", 100));
        return true;
    }

    private static void prepareColumn(ServerLevel level, int x, int z) {
        int originalSurface = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        int outside = outsideDistance(x, z);
        int targetY;
        if (outside <= 0) {
            targetY = GROUND_Y;
        } else {
            int clampedOriginal = Math.max(58, Math.min(84, originalSurface));
            double t = Math.min(1.0, outside / (double)(BLEND + 1));
            t = t * t * (3.0 - 2.0 * t);
            targetY = (int)Math.round(GROUND_Y + (clampedOriginal - GROUND_Y) * t);
        }

        int clearTop = Math.max(targetY, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z));
        if (originalSurface < targetY) {
            for (int y = Math.max(originalSurface, targetY - 7); y < targetY; y++) {
                level.setBlock(new BlockPos(x, y, z), Blocks.DIRT.defaultBlockState(), 2);
            }
        } else {
            for (int y = targetY - 3; y < targetY; y++) {
                level.setBlock(new BlockPos(x, y, z), Blocks.DIRT.defaultBlockState(), 2);
            }
        }
        level.setBlock(new BlockPos(x, targetY, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        for (int y = targetY + 1; y <= clearTop; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
    }

    private static int outsideDistance(int x, int z) {
        int dx = x < MIN_X ? MIN_X - x : x > MAX_X ? x - MAX_X : 0;
        int dz = z < MIN_Z ? MIN_Z - z : z > MAX_Z ? z - MAX_Z : 0;
        return Math.max(dx, dz);
    }

    private static void holdPlayer(ServerPlayer player) {
        player.setNoGravity(true);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        player.setPos(0.5, 250.0, 20.5);
    }

    private static final class State {
        private int index;
        private int lastPercent = -4;
    }
}
