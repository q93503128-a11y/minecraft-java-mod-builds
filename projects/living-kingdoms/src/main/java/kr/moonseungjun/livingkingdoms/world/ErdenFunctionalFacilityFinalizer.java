package kr.moonseungjun.livingkingdoms.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Restores usable civic fixtures and supported roof details after the structural rebuild. */
public final class ErdenFunctionalFacilityFinalizer {
    private static final int UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS;

    private ErdenFunctionalFacilityFinalizer() {
    }

    public static void ensure(ServerLevel level, RealmSiteLayoutSavedData.RealmSite site) {
        int cx = site.centerX();
        int cz = site.centerZ();
        int y = Math.max(68, Math.min(112, site.baseY()));

        // Citadel hall light: supported instead of a floating lantern.
        set(level, cx - 18, y + 2, cz - 70, Blocks.SPRUCE_FENCE);
        set(level, cx - 18, y + 3, cz - 70, Blocks.LANTERN);

        // Temple, inn, guild, smithy, barracks and granary remain mechanically recognizable.
        set(level, cx - 63, y + 1, cz - 66, Blocks.STONE_BRICKS);
        set(level, cx - 63, y + 2, cz - 66, Blocks.BELL);
        set(level, cx + 57, y + 1, cz + 21, Blocks.STONE_BRICKS);
        set(level, cx + 57, y + 2, cz + 21, Blocks.CAMPFIRE);
        set(level, cx - 72, y + 2, cz + 22, Blocks.CARTOGRAPHY_TABLE);
        set(level, cx + 98, y + 1, cz - 30, Blocks.BLAST_FURNACE);
        set(level, cx - 86, y + 2, cz - 36, Blocks.IRON_BARS);
        set(level, cx + 85, y + 2, cz + 79, Blocks.BARREL);

        // Market beacon at the original readable height.
        for (int py = y + 1; py <= y + 6; py++) {
            set(level, cx, py, cz, Blocks.CHISELED_STONE_BRICKS);
        }
        set(level, cx, y + 7, cz, Blocks.LANTERN);

        // Supported entrance awning on the first residential row. This also prevents the exact
        // floating-roof shape that escaped the old diagnostics.
        for (int py = y + 1; py <= y + 5; py++) {
            set(level, cx - 58, py, cz - 40, Blocks.STRIPPED_SPRUCE_LOG);
        }
        set(level, cx - 58, y + 6, cz - 40, Blocks.DARK_OAK_PLANKS);
    }

    private static void set(ServerLevel level, int x, int y, int z, Block block) {
        BlockPos pos = new BlockPos(x, y, z);
        if (y < level.getMinY() || y >= level.getMaxY()) return;
        if (level.getBlockState(pos).getBlock() == block) return;
        level.setBlock(pos, block.defaultBlockState(), UPDATE_FLAGS);
    }
}
