package io.github.q93503128.turnbound.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Presentation-only readability pass for the first Southgate walk.
 * Canonical encounter positions and combat data remain owned by SouthgateChapterWorld/FieldSessionManager.
 */
public final class SouthgateOpeningReadabilityWorld {
    private static final BlockPos MARKER_A = new BlockPos(18, 58, 130);
    private static final BlockPos MARKER_B = new BlockPos(19, 58, 130);

    private SouthgateOpeningReadabilityWorld() {}

    public static void build(ServerLevel level) {
        if (level.getBlockState(MARKER_A).is(Blocks.GOLD_BLOCK)
                && level.getBlockState(MARKER_B).is(Blocks.COPPER_BLOCK)) return;

        // Breadcrumbs follow the authored M01/M02 curve instead of drawing a second arbitrary road.
        mark(level, 0, 127);
        mark(level, -2, 134);
        mark(level, -6, 142);
        mark(level, -10, 149);
        mark(level, -7, 156);
        mark(level, -2, 162);
        mark(level, 5, 168);
        mark(level, 11, 173);

        // Two small sightline posts frame the first patrol without becoming collision-heavy scenery.
        lantern(level, -18, 145);
        lantern(level, 3, 153);

        // South Gate threshold gets an unmistakable forward arrow made from floor accents.
        for (int z = 121; z <= 133; z += 3) mark(level, 0, z);
        mark(level, -2, 133);
        mark(level, 2, 133);

        level.setBlock(MARKER_A, Blocks.GOLD_BLOCK.defaultBlockState(), 2);
        level.setBlock(MARKER_B, Blocks.COPPER_BLOCK.defaultBlockState(), 2);
    }

    private static void mark(ServerLevel level, int x, int z) {
        int y = ground(level, x, z);
        if (y < 58 || y > 76) return;
        level.setBlock(new BlockPos(x, y, z), ((x + z) & 1) == 0
                ? Blocks.GOLD_BLOCK.defaultBlockState()
                : Blocks.CUT_COPPER.defaultBlockState(), 2);
    }

    private static void lantern(ServerLevel level, int x, int z) {
        int y = ground(level, x, z);
        if (y < 58 || y > 76) return;
        level.setBlock(new BlockPos(x, y + 1, z), Blocks.COBBLESTONE_WALL.defaultBlockState(), 2);
        level.setBlock(new BlockPos(x, y + 2, z), Blocks.LANTERN.defaultBlockState(), 2);
    }

    private static int ground(ServerLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
    }
}
