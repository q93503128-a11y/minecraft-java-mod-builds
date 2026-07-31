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

/**
 * Builds the rural landscape while a superflat chunk is being generated.
 * Unlike the old runtime painter, blocks are complete before the chunk is sent
 * to the client, so there is no approach-triggered pop-in or stale black light.
 */
public final class CountrysideChunkFeature extends Feature<NoneFeatureConfiguration> {
    private static final int CLEAR_HEIGHT = 24;

    public CountrysideChunkFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!(context.chunkGenerator() instanceof FlatLevelSource)) {
            return false;
        }

        WorldGenLevel level = context.level();
        int minX = context.origin().getX() & ~15;
        int minZ = context.origin().getZ() & ~15;
        int groundY = findGroundY(level, minX, minZ);

        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                clearSurface(level, x, groundY, z);
                restoreSoil(level, x, groundY, z);
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
                int sample = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, minX + dx, minZ + dz) - 1;
                groundY = Math.min(groundY, sample);
            }
        }
        return groundY == Integer.MAX_VALUE ? level.getMinY() + 4 : groundY;
    }

    private static void clearSurface(WorldGenLevel level, int x, int groundY, int z) {
        for (int y = groundY + 1; y <= groundY + CLEAR_HEIGHT; y++) {
            level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        }
    }

    private static void restoreSoil(WorldGenLevel level, int x, int groundY, int z) {
        level.setBlock(new BlockPos(x, groundY, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        for (int depth = 1; depth <= 4; depth++) {
            level.setBlock(new BlockPos(x, groundY - depth, z), Blocks.DIRT.defaultBlockState(), 2);
        }
    }

    private static void decorateColumn(WorldGenLevel level, int x, int groundY, int z) {
        boolean river = isRiver(x, z);
        boolean road = isMainRoad(x, z) || isCrossRoad(x, z);

        if (river && road) {
            buildBridgeColumn(level, x, groundY, z);
            return;
        }
        if (river) {
            buildRiverColumn(level, x, groundY, z);
            return;
        }
        if (isRiverBank(x, z)) {
            BlockState bank = Math.floorMod(x * 17 + z * 31, 5) == 0
                    ? Blocks.GRAVEL.defaultBlockState()
                    : Blocks.COARSE_DIRT.defaultBlockState();
            level.setBlock(new BlockPos(x, groundY, z), bank, 2);
            return;
        }
        if (road) {
            BlockState path = Math.floorMod(x * 13 + z * 7, 7) == 0
                    ? Blocks.GRAVEL.defaultBlockState()
                    : Blocks.PACKED_MUD.defaultBlockState();
            level.setBlock(new BlockPos(x, groundY, z), path, 2);
            return;
        }

        int field = fieldType(x, z);
        if (field != 0) {
            buildFieldColumn(level, x, groundY, z, field);
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
        if (plant != null) {
            level.setBlock(new BlockPos(x, groundY + 1, z), plant, 2);
        }
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

    private static boolean isMainRoad(int x, int z) {
        double center = 18.0 + 10.0 * Math.sin((x + 24.0) / 72.0);
        return Math.abs(z - center) <= 2.4;
    }

    private static boolean isCrossRoad(int x, int z) {
        double center = -46.0 + 11.0 * Math.sin((z - 15.0) / 93.0);
        return Math.abs(x - center) <= 2.4;
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

    private static void buildBridgeColumn(WorldGenLevel level, int x, int groundY, int z) {
        buildRiverColumn(level, x, groundY, z);
        level.setBlock(new BlockPos(x, groundY + 1, z), Blocks.SPRUCE_PLANKS.defaultBlockState(), 2);
        if ((isCrossRoad(x, z) && Math.floorMod(x, 5) == 0)
                || (isMainRoad(x, z) && Math.floorMod(z, 5) == 0)) {
            level.setBlock(new BlockPos(x, groundY + 2, z), Blocks.SPRUCE_FENCE.defaultBlockState(), 2);
        }
    }

    private static int fieldType(int x, int z) {
        if (Math.abs(x) < 72 && Math.abs(z) < 68) {
            return 0;
        }
        int localX = Math.floorMod(x + 96, 192);
        int localZ = Math.floorMod(z + 96, 192);
        if (localX >= 18 && localX <= 78 && localZ >= 20 && localZ <= 74) {
            return 1;
        }
        if (localX >= 108 && localX <= 172 && localZ >= 24 && localZ <= 84) {
            return 2;
        }
        if (localX >= 24 && localX <= 88 && localZ >= 112 && localZ <= 174) {
            return 3;
        }
        return 0;
    }

    private static void buildFieldColumn(WorldGenLevel level, int x, int groundY, int z, int field) {
        int localX = Math.floorMod(x + 96, 192);
        int localZ = Math.floorMod(z + 96, 192);
        boolean border = localX == 18 || localX == 78 || localX == 108 || localX == 172
                || localZ == 20 || localZ == 74 || localZ == 24 || localZ == 84
                || localX == 24 || localX == 88 || localZ == 112 || localZ == 174;
        if (border && Math.floorMod(x + z, 3) != 0) {
            level.setBlock(new BlockPos(x, groundY, z), Blocks.COARSE_DIRT.defaultBlockState(), 2);
            level.setBlock(new BlockPos(x, groundY + 1, z), Blocks.OAK_LEAVES.defaultBlockState(), 2);
            return;
        }
        if (Math.floorMod(x, 12) == 0) {
            level.setBlock(new BlockPos(x, groundY, z), Blocks.WATER.defaultBlockState(), 2);
            return;
        }
        if (Math.floorMod(x, 24) == 1 || Math.floorMod(z, 24) == 1) {
            level.setBlock(new BlockPos(x, groundY, z), Blocks.PACKED_MUD.defaultBlockState(), 2);
            return;
        }

        level.setBlock(new BlockPos(x, groundY, z), Blocks.FARMLAND.defaultBlockState(), 2);
        BlockState crop = switch (field) {
            case 1 -> Blocks.WHEAT.defaultBlockState();
            case 2 -> Blocks.CARROTS.defaultBlockState();
            default -> Blocks.POTATOES.defaultBlockState();
        };
        level.setBlock(new BlockPos(x, groundY + 1, z), crop, 2);
    }

    private static void placeChunkTrees(WorldGenLevel level, int minX, int groundY, int minZ) {
        for (int x = minX + 3; x < minX + 14; x++) {
            for (int z = minZ + 3; z < minZ + 14; z++) {
                if (Math.abs(x) < 80 && Math.abs(z) < 76) {
                    continue;
                }
                if (isRiver(x, z) || isRiverBank(x, z) || isMainRoad(x, z) || isCrossRoad(x, z)) {
                    continue;
                }
                if (fieldType(x, z) != 0) {
                    continue;
                }
                if (Math.floorMod(x, 37) == 11
                        && Math.floorMod(z, 37) == 11
                        && Math.floorMod(x * 31 + z * 17, 7) <= 1) {
                    buildTree(level, new BlockPos(x, groundY + 1, z));
                }
            }
        }
    }

    private static void buildTree(WorldGenLevel level, BlockPos base) {
        for (int y = 0; y <= 4; y++) {
            level.setBlock(base.above(y), Blocks.OAK_LOG.defaultBlockState(), 2);
        }
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
