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
 * Creates Aster March on its own level playfield before authored content is placed.
 * The whole canonical 1024x1024 footprint is flattened so vanilla hills, oceans and trees cannot intersect routes.
 */
public final class AsterMarchFoundationBuilder {
    private static final int MIN = -520;
    private static final int MAX = 520;
    private static final int GROUND_Y = 65;
    private static final int COLUMNS_PER_TICK = 1400;
    private static final BlockPos MARKER_A = new BlockPos(-510, 54, -510);
    private static final BlockPos MARKER_B = new BlockPos(-509, 54, -510);
    private static final BlockPos MARKER_C = new BlockPos(-508, 54, -510);
    private static final BlockPos MARKER_D = new BlockPos(-507, 54, -510);
    private static final Map<ServerLevel, State> STATES = new WeakHashMap<>();

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
        int width = MAX - MIN + 1;
        int total = width * width;
        int processed = 0;
        while (processed < COLUMNS_PER_TICK && state.index < total) {
            int x = MIN + state.index % width;
            int z = MIN + state.index / width;
            flattenColumn(level, x, z);
            state.index++;
            processed++;
        }
        int percent = Math.min(99, (int)Math.floor(state.index * 100.0 / total));
        if (percent != state.lastPercent && (percent == 0 || percent >= state.lastPercent + 2)) {
            state.lastPercent = percent;
            FieldNetwork.sync(player, FieldUiSnapshot.loading("아스테르 변경 지형 생성", percent));
        }
        if (state.index < total) return false;

        level.setBlock(MARKER_A, Blocks.LODESTONE.defaultBlockState(), 2);
        level.setBlock(MARKER_B, Blocks.EMERALD_BLOCK.defaultBlockState(), 2);
        level.setBlock(MARKER_C, Blocks.GOLD_BLOCK.defaultBlockState(), 2);
        level.setBlock(MARKER_D, Blocks.DIAMOND_BLOCK.defaultBlockState(), 2);
        STATES.remove(level);
        FieldNetwork.sync(player, FieldUiSnapshot.loading("아스테르 변경 배치", 100));
        return true;
    }

    private static void flattenColumn(ServerLevel level, int x, int z) {
        int originalSurface = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        for (int y = GROUND_Y - 4; y < GROUND_Y; y++) {
            level.setBlock(new BlockPos(x, y, z), Blocks.DIRT.defaultBlockState(), 2);
        }
        level.setBlock(new BlockPos(x, GROUND_Y, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);

        int top = Math.max(GROUND_Y + 1, originalSurface + 2);
        for (int y = GROUND_Y + 1; y <= top; y++) {
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
        private int lastPercent = -2;
    }
}
