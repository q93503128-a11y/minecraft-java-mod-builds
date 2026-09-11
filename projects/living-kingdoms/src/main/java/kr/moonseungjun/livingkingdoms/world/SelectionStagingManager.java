package kr.moonseungjun.livingkingdoms.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Set;

/**
 * First-time selection no longer relocates players onto a visible developer platform. The modal
 * origin/loading screens are the staging boundary; actual realm entry happens only after an authored
 * Erden residence has been verified.
 */
public final class SelectionStagingManager {
    private static final int LEGACY_CENTER_X = 0;
    private static final int LEGACY_CENTER_Z = 24_000;
    private static final int LEGACY_FLOOR_Y = 220;
    private static final int UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS;
    private static final BlockPos LEGACY_MARKER =
            new BlockPos(LEGACY_CENTER_X, LEGACY_FLOOR_Y - 5, LEGACY_CENTER_Z);
    private static final Set<Block> LEGACY_BLOCKS = Set.of(
            Blocks.STONE_BRICKS,
            Blocks.POLISHED_ANDESITE,
            Blocks.SEA_LANTERN,
            Blocks.STONE_BRICK_WALL,
            Blocks.SPRUCE_FENCE,
            Blocks.LANTERN,
            Blocks.CHISELED_STONE_BRICKS,
            Blocks.LODESTONE
    );

    private SelectionStagingManager() {
    }

    /** Selection screens themselves hold input; no world mutation or teleport is permitted here. */
    public static void ensure(ServerPlayer player) {
        player.fallDistance = 0.0F;
    }

    /** Removes the old 19x19 CI/player staging pad after the player is safely inside a real home. */
    public static void cleanupLegacyPlatform(ServerLevel level) {
        if (!level.hasChunkAt(LEGACY_MARKER)
                || !level.getBlockState(LEGACY_MARKER).is(Blocks.LODESTONE)) return;
        int removed = 0;
        for (int x = LEGACY_CENTER_X - 10; x <= LEGACY_CENTER_X + 10; x++) {
            for (int z = LEGACY_CENTER_Z - 10; z <= LEGACY_CENTER_Z + 10; z++) {
                for (int y = LEGACY_FLOOR_Y - 6; y <= LEGACY_FLOOR_Y + 7; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!LEGACY_BLOCKS.contains(level.getBlockState(pos).getBlock())) continue;
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
                    removed++;
                }
            }
        }
        if (removed > 0) {
            kr.moonseungjun.livingkingdoms.LivingKingdoms.LOGGER.info(
                    "Removed legacy visible selection staging platform blocks={} synthetic_staging=false",
                    removed);
        }
    }
}
