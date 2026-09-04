package io.github.q93503128.turnbound.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/** Small migration-safe visual cleanup for authored Aster March landmarks. */
public final class AsterMarchWorldPolish {
    private static final BlockPos MARKER = new BlockPos(476, 44, 476);

    private AsterMarchWorldPolish() {}

    public static void build(ServerLevel level) {
        if (level.getBlockState(MARKER).is(Blocks.LODESTONE)) return;
        collapseOversizedAqueductBoundary(level);
        buildLowAqueductRemains(level);
        level.setBlock(MARKER, Blocks.LODESTONE.defaultBlockState(), 2);
    }

    /** Removes the old 28-block-high repeated T silhouettes on the west boundary. */
    private static void collapseOversizedAqueductBoundary(ServerLevel level) {
        for (int z = -140; z <= 150; z += 28) {
            if (z > -20 && z < 28) continue;
            int top = 86 + Math.floorMod(z, 3);
            for (int y = 58; y <= top; y++) {
                setAir(level, -474, y, z);
                setAir(level, -470, y, z);
            }
            for (int dz = -9; dz <= 9; dz++) setAir(level, -472, top, z + dz);
        }
    }

    /** Low, irregular stubs read as a collapsed aqueduct without becoming a skyline-sized fence. */
    private static void buildLowAqueductRemains(ServerLevel level) {
        int[] zs = {-118, -67, 63, 119};
        for (int i = 0; i < zs.length; i++) {
            int z = zs[i];
            int leftH = 3 + i % 3;
            int rightH = 5 - i % 2;
            for (int dy = 0; dy <= leftH; dy++) {
                level.setBlock(new BlockPos(-474, 63 + dy, z),
                        (dy == leftH ? Blocks.CHISELED_STONE_BRICKS : Blocks.MOSSY_STONE_BRICKS).defaultBlockState(), 2);
            }
            for (int dy = 0; dy <= rightH; dy++) {
                level.setBlock(new BlockPos(-469, 63 + dy, z + 2),
                        (dy == rightH ? Blocks.CRACKED_STONE_BRICKS : Blocks.STONE_BRICKS).defaultBlockState(), 2);
            }
            if ((i & 1) == 0) {
                level.setBlock(new BlockPos(-473, 64, z + 1), Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), 2);
                level.setBlock(new BlockPos(-472, 64, z + 1), Blocks.CRACKED_STONE_BRICKS.defaultBlockState(), 2);
            }
        }
    }

    private static void setAir(ServerLevel level, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        if (!level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
    }
}
