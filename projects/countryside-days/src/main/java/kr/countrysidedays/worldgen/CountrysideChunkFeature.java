package kr.countrysidedays.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Builds a quiet meadow. Water and trees are kept far outside all village and estate slots. */
public final class CountrysideChunkFeature extends Feature<NoneFeatureConfiguration> {
    private static final int VEGETATION_CLEAR_HEIGHT = 10;
    private static final int SETTLEMENT_RESERVE = 560;

    public CountrysideChunkFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!(context.chunkGenerator() instanceof FlatLevelSource)) return false;

        WorldGenLevel level = context.level();
        int minX = context.origin().getX() & ~15;
        int minZ = context.origin().getZ() & ~15;
        int groundY = findGroundY(level, minX, minZ);

        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                clearVanillaVegetation(level, x, groundY, z);
                decorateColumn(level, x, groundY, z);
            }
        }

        placeChunkTrees(level, minX, groundY, minZ);
        return true;
    }

    private static int findGroundY(WorldGenLevel level, int minX, int minZ) {
        int groundY = Integer.MAX_VALUE;
        for (int dx = 1; dx <= 14; dx += 4) {
            for (int dz = 1; dz <= 14; dz += 4) {
                int sample = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, minX + dx, minZ + dz) - 1;
                groundY = Math.min(groundY, sample);
            }
        }
        return groundY == Integer.MAX_VALUE ? level.getMinY() + 9 : groundY;
    }

    private static void clearVanillaVegetation(WorldGenLevel level, int x, int groundY, int z) {
        for (int y = groundY + 1; y <= groundY + VEGETATION_CLEAR_HEIGHT; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
        level.setBlock(new BlockPos(x, groundY, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
    }

    private static void decorateColumn(WorldGenLevel level, int x, int groundY, int z) {
        if (!isSettlementReserve(x, z) && isRiver(x, z)) {
            buildRiverColumn(level, x, groundY, z);
            return;
        }
        if (!isSettlementReserve(x, z) && isRiverBank(x, z)) {
            BlockState bank = Math.floorMod(x * 17 + z * 31, 5) == 0
                    ? Blocks.GRAVEL.defaultBlockState()
                    : Blocks.COARSE_DIRT.defaultBlockState();
            level.setBlock(new BlockPos(x, groundY, z), bank, 2);
            return;
        }

        int decoration = Math.floorMod(x * 73428767 ^ z * 912931, 181);
        BlockState plant = switch (decoration) {
            case 0 -> Blocks.DANDELION.defaultBlockState();
            case 1 -> Blocks.POPPY.defaultBlockState();
            case 2 -> Blocks.CORNFLOWER.defaultBlockState();
            case 3, 4, 5 -> Blocks.SHORT_GRASS.defaultBlockState();
            default -> null;
        };
        if (plant != null) level.setBlock(new BlockPos(x, groundY + 1, z), plant, 2);
    }

    private static boolean isSettlementReserve(int x, int z) {
        return Math.abs(x) <= SETTLEMENT_RESERVE && Math.abs(z) <= SETTLEMENT_RESERVE;
    }

    private static boolean isRiver(int x, int z) {
        double center = 118.0 + 42.0 * Math.sin(x / 78.0) + 13.0 * Math.sin(x / 29.0);
        double width = 13.0 + 2.0 * Math.sin(x / 41.0);
        return Math.abs(z - center) <= width;
    }

    private static boolean isRiverBank(int x, int z) {
        double center = 118.0 + 42.0 * Math.sin(x / 78.0) + 13.0 * Math.sin(x / 29.0);
        double width = 13.0 + 2.0 * Math.sin(x / 41.0);
        double distance = Math.abs(z - center);
        return distance > width && distance <= width + 5.0;
    }

    private static void buildRiverColumn(WorldGenLevel level, int x, int groundY, int z) {
        double center = 118.0 + 42.0 * Math.sin(x / 78.0) + 13.0 * Math.sin(x / 29.0);
        double width = 13.0 + 2.0 * Math.sin(x / 41.0);
        double distance = Math.abs(z - center);
        int depth = distance < width * 0.5 ? 4 : distance < width * 0.82 ? 3 : 2;

        for (int y = groundY; y > groundY - depth; y--) {
            level.setBlock(new BlockPos(x, y, z), Blocks.WATER.defaultBlockState(), 2);
        }
        BlockState bottom = Math.floorMod(x * 31 + z * 17, 5) == 0
                ? Blocks.CLAY.defaultBlockState()
                : Blocks.GRAVEL.defaultBlockState();
        level.setBlock(new BlockPos(x, groundY - depth, z), bottom, 2);
    }

    private static void placeChunkTrees(WorldGenLevel level, int minX, int groundY, int minZ) {
        for (int x = minX + 3; x < minX + 14; x++) {
            for (int z = minZ + 3; z < minZ + 14; z++) {
                if (isSettlementReserve(x, z) || isRiver(x, z) || isRiverBank(x, z)) continue;
                if (Math.floorMod(x, 37) == 11
                        && Math.floorMod(z, 37) == 11
                        && Math.floorMod(x * 31 + z * 17, 7) <= 1) {
                    buildTree(level, new BlockPos(x, groundY + 1, z));
                }
            }
        }
    }

    private static void buildTree(WorldGenLevel level, BlockPos base) {
        for (int y = 0; y <= 4; y++) level.setBlock(base.above(y), Blocks.OAK_LOG.defaultBlockState(), 2);
        for (int y = 3; y <= 5; y++) {
            int radius = y == 5 ? 1 : 2;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) <= radius + 1) {
                        level.setBlock(base.offset(dx, y, dz), Blocks.OAK_LEAVES.defaultBlockState(), 2);
                    }
                }
            }
        }
    }
}
