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
 * Work is staged across ticks so a normal/default world is converted before TURNBOUND places the player in Radia.
 */
public final class AsterMarchFoundationBuilder {
    private static final int MIN_X = AsterMarchRegionCatalog.RADIA.minX();
    private static final int MAX_X = AsterMarchRegionCatalog.RADIA.maxX();
    private static final int MIN_Z = AsterMarchRegionCatalog.RADIA.minZ();
    private static final int MAX_Z = AsterMarchRegionCatalog.RADIA.maxZ();
    private static final int GROUND_Y = 65;
    private static final int COLUMNS_PER_TICK = 600;
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

    /** @return true when the foundation is complete and normal world entry can continue. */
    public static boolean step(ServerLevel level, ServerPlayer player) {
        if (ready(level)) return true;
        State state = STATES.computeIfAbsent(level, ignored -> new State());
        holdPlayer(player);
        int total = (MAX_X - MIN_X + 1) * (MAX_Z - MIN_Z + 1);
        int processed = 0;
        while (processed < COLUMNS_PER_TICK && state.index < total) {
            int width = MAX_X - MIN_X + 1;
            int x = MIN_X + (state.index % width);
            int z = MIN_Z + (state.index / width);
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
        int clearTop = Math.max(GROUND_Y, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z));
        level.setBlock(new BlockPos(x, GROUND_Y - 3, z), Blocks.DIRT.defaultBlockState(), 2);
        level.setBlock(new BlockPos(x, GROUND_Y - 2, z), Blocks.DIRT.defaultBlockState(), 2);
        level.setBlock(new BlockPos(x, GROUND_Y - 1, z), Blocks.DIRT.defaultBlockState(), 2);
        level.setBlock(new BlockPos(x, GROUND_Y, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        for (int y = GROUND_Y + 1; y <= clearTop; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
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
