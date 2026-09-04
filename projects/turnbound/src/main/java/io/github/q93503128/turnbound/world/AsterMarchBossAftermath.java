package io.github.q93503128.turnbound.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Persistent physical memory of campaign boss clears.
 *
 * World-common boss progression already lives in TurnboundWorldSavedData. This layer only mirrors that truth into
 * the authored map: once a boss is cleared, a small Relay-remnant site appears beyond the arena so the route no
 * longer looks exactly as it did before the victory. No rewards, quest flags or combat state are owned here.
 */
public final class AsterMarchBossAftermath {
    private static final BlockPos MARKER_B01 = new BlockPos(-490, 43, 490);
    private static final BlockPos MARKER_B02 = new BlockPos(-489, 43, 490);
    private static final BlockPos MARKER_B03 = new BlockPos(-488, 43, 490);
    private static final BlockPos MARKER_B04 = new BlockPos(-487, 43, 490);
    private static final BlockPos MARKER_B05 = new BlockPos(-486, 43, 490);

    private AsterMarchBossAftermath() {}

    public static void sync(ServerLevel level) {
        if (level == null || level.getServer() == null) return;
        TurnboundWorldSavedData data = TurnboundWorldSavedData.get(level.getServer());
        if (data.bossCleared("B01") && !marked(level, MARKER_B01, Blocks.OAK_LOG)) {
            echo(level, 369, 67, 245, Blocks.POLISHED_ANDESITE, Blocks.LODESTONE, Blocks.LANTERN, Blocks.OAK_FENCE);
            mark(level, MARKER_B01, Blocks.OAK_LOG);
        }
        if (data.bossCleared("B02") && !marked(level, MARKER_B02, Blocks.MOSS_BLOCK)) {
            echo(level, -35, 71, -458, Blocks.MOSSY_STONE_BRICKS, Blocks.AMETHYST_BLOCK, Blocks.SOUL_LANTERN, Blocks.DARK_OAK_FENCE);
            mark(level, MARKER_B02, Blocks.MOSS_BLOCK);
        }
        if (data.bossCleared("B03") && !marked(level, MARKER_B03, Blocks.IRON_BLOCK)) {
            echo(level, -448, 63, 35, Blocks.POLISHED_ANDESITE, Blocks.IRON_BLOCK, Blocks.REDSTONE_LAMP, Blocks.IRON_BARS);
            mark(level, MARKER_B03, Blocks.IRON_BLOCK);
        }
        if (data.bossCleared("B04") && !marked(level, MARKER_B04, Blocks.BLACKSTONE)) {
            echo(level, 65, 62, 480, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.CRYING_OBSIDIAN, Blocks.SOUL_LANTERN, Blocks.IRON_BARS);
            mark(level, MARKER_B04, Blocks.BLACKSTONE);
        }
        if (data.bossCleared("B05") && !marked(level, MARKER_B05, Blocks.AMETHYST_BLOCK)) {
            echo(level, 440, 65, -367, Blocks.POLISHED_DEEPSLATE, Blocks.AMETHYST_BLOCK, Blocks.END_ROD, Blocks.IRON_BARS);
            mark(level, MARKER_B05, Blocks.AMETHYST_BLOCK);
        }
    }

    private static void echo(ServerLevel level, int cx, int groundY, int cz, Block floor, Block core, Block light, Block rail) {
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            if (dx * dx + dz * dz > 10) continue;
            Block ground = (Math.abs(dx) == 3 || Math.abs(dz) == 3) ? floor : Blocks.SMOOTH_STONE;
            set(level, cx + dx, groundY, cz + dz, ground);
        }
        for (int[] p : new int[][]{{-3,0},{3,0},{0,-3},{0,3}}) {
            set(level, cx + p[0], groundY + 1, cz + p[1], rail);
        }
        set(level, cx, groundY + 1, cz, core);
        set(level, cx, groundY + 2, cz, light);
    }

    private static boolean marked(ServerLevel level, BlockPos pos, Block marker) {
        return level.getBlockState(pos).is(marker);
    }

    private static void mark(ServerLevel level, BlockPos pos, Block marker) {
        level.setBlock(pos, marker.defaultBlockState(), 2);
    }

    private static void set(ServerLevel level, int x, int y, int z, Block block) {
        level.setBlock(new BlockPos(x, y, z), block.defaultBlockState(), 2);
    }
}
