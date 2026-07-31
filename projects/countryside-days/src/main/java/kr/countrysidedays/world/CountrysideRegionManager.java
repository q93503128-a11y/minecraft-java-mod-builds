package kr.countrysidedays.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.flat.FlatLevelSource;

/**
 * Lazily reshapes loaded superflat chunks into one broad countryside region.
 * The region is generated a few loaded chunks at a time, so entering the world
 * does not freeze while still producing a 1,024 block wide rural landscape.
 */
public final class CountrysideRegionManager {
    public static final int REGION_RADIUS = 512;
    private static final int INITIAL_CHUNK_RADIUS = 4;
    private static final int EXPLORATION_CHUNK_RADIUS = 2;
    private static final int UPDATE_FLAGS = Block.UPDATE_CLIENTS;

    private CountrysideRegionManager() {
    }

    public static boolean isFlatWorld(ServerLevel level) {
        return level.getChunkSource().getGenerator() instanceof FlatLevelSource;
    }

    public static void prepareInitialArea(ServerLevel level, BlockPos center) {
        prepareAround(level, center, INITIAL_CHUNK_RADIUS);
    }

    public static void prepareAroundPlayer(ServerLevel level, BlockPos playerPos) {
        CountrysideWorldData.get(level.getServer()).homesteadOrigin()
                .ifPresent(center -> prepareAround(level, playerPos, EXPLORATION_CHUNK_RADIUS));
    }

    public static boolean isInsideCountryside(ServerLevel level, BlockPos pos) {
        return CountrysideWorldData.get(level.getServer()).homesteadOrigin()
                .map(center -> isInsideCountryside(center, pos))
                .orElse(false);
    }

    public static boolean isInsideCountryside(BlockPos center, BlockPos pos) {
        long dx = (long) pos.getX() - center.getX();
        long dz = (long) pos.getZ() - center.getZ();
        return dx * dx + dz * dz <= (long) REGION_RADIUS * REGION_RADIUS;
    }

    public static boolean isRiverCoordinate(BlockPos center, int x, int z) {
        int localX = x - center.getX();
        double riverCenter = center.getZ() + 105.0
                + 45.0 * Math.sin(localX / 70.0)
                + 18.0 * Math.sin(localX / 27.0);
        double width = 11.0 + 2.5 * Math.sin(localX / 31.0);
        return Math.abs(z - riverCenter) <= width;
    }

    private static void prepareAround(ServerLevel level, BlockPos around, int chunkRadius) {
        if (level.dimension() != Level.OVERWORLD || !isFlatWorld(level)) {
            return;
        }

        CountrysideWorldData data = CountrysideWorldData.get(level.getServer());
        BlockPos center = data.homesteadOrigin().orElse(around);
        int centerChunkX = around.getX() >> 4;
        int centerChunkZ = around.getZ() >> 4;
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                int chunkX = centerChunkX + dx;
                int chunkZ = centerChunkZ + dz;
                if (level.hasChunk(chunkX, chunkZ)) {
                    prepareChunk(level, center, chunkX, chunkZ, data);
                }
            }
        }
    }

    private static void prepareChunk(
            ServerLevel level,
            BlockPos center,
            int chunkX,
            int chunkZ,
            CountrysideWorldData data
    ) {
        if (data.isTerrainChunkPrepared(chunkX, chunkZ)) {
            return;
        }

        int chunkCenterX = (chunkX << 4) + 8;
        int chunkCenterZ = (chunkZ << 4) + 8;
        if (!isInsideCountryside(center, new BlockPos(chunkCenterX, center.getY(), chunkCenterZ))
                && !isInsideCountryside(center, new BlockPos(chunkCenterX - 16, center.getY(), chunkCenterZ - 16))
                && !isInsideCountryside(center, new BlockPos(chunkCenterX + 16, center.getY(), chunkCenterZ + 16))) {
            return;
        }

        int groundY = center.getY() - 1;
        boolean protectExistingHomestead = data.homesteadOrigin().isPresent();
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;

        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                BlockPos column = new BlockPos(x, groundY, z);
                if (!isInsideCountryside(center, column)) {
                    continue;
                }

                int localX = x - center.getX();
                int localZ = z - center.getZ();
                if (protectExistingHomestead && Math.abs(localX) <= 36 && Math.abs(localZ) <= 32) {
                    continue;
                }

                clearAbove(level, x, groundY, z);
                restoreSoil(level, x, groundY, z);

                if (isRiverCoordinate(center, x, z)) {
                    buildRiverColumn(level, center, x, groundY, z);
                } else if (isRiverBank(center, x, z)) {
                    set(level, new BlockPos(x, groundY, z), ((x + z) & 1) == 0
                            ? Blocks.COARSE_DIRT.defaultBlockState()
                            : Blocks.GRAVEL.defaultBlockState());
                } else if (isRoadCoordinate(center, x, z)) {
                    set(level, new BlockPos(x, groundY, z), Math.floorMod(x + z, 5) == 0
                            ? Blocks.GRAVEL.defaultBlockState()
                            : Blocks.DIRT_PATH.defaultBlockState());
                } else {
                    int field = fieldAt(localX, localZ);
                    if (field != 0) {
                        buildFieldColumn(level, x, groundY, z, field);
                    } else {
                        buildMeadowColumn(level, x, groundY, z);
                    }
                }
            }
        }

        for (int x = minX + 2; x < minX + 14; x++) {
            for (int z = minZ + 2; z < minZ + 14; z++) {
                int localX = x - center.getX();
                int localZ = z - center.getZ();
                if ((!protectExistingHomestead || Math.abs(localX) > 36 || Math.abs(localZ) > 32)
                        && isMeadowTreeAnchor(center, x, z)) {
                    buildTree(level, new BlockPos(x, groundY + 1, z));
                }
            }
        }

        data.markTerrainChunkPrepared(chunkX, chunkZ);
    }

    private static void clearAbove(ServerLevel level, int x, int groundY, int z) {
        for (int y = groundY + 1; y <= groundY + 10; y++) {
            set(level, new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
        }
    }

    private static void restoreSoil(ServerLevel level, int x, int groundY, int z) {
        set(level, new BlockPos(x, groundY, z), Blocks.GRASS_BLOCK.defaultBlockState());
        for (int depth = 1; depth <= 4; depth++) {
            set(level, new BlockPos(x, groundY - depth, z), Blocks.DIRT.defaultBlockState());
        }
    }

    private static void buildRiverColumn(ServerLevel level, BlockPos center, int x, int groundY, int z) {
        int localX = x - center.getX();
        double riverCenter = center.getZ() + 105.0
                + 45.0 * Math.sin(localX / 70.0)
                + 18.0 * Math.sin(localX / 27.0);
        double width = 11.0 + 2.5 * Math.sin(localX / 31.0);
        double distance = Math.abs(z - riverCenter);
        int depth = distance < width * 0.52 ? 4 : distance < width * 0.8 ? 3 : 2;

        set(level, new BlockPos(x, groundY, z), Blocks.AIR.defaultBlockState());
        for (int y = groundY - 1; y >= groundY - depth; y--) {
            set(level, new BlockPos(x, y, z), Blocks.WATER.defaultBlockState());
        }
        BlockState bottom = Math.floorMod(x * 31 + z * 17, 4) == 0
                ? Blocks.CLAY.defaultBlockState()
                : Blocks.GRAVEL.defaultBlockState();
        set(level, new BlockPos(x, groundY - depth - 1, z), bottom);
    }

    private static boolean isRiverBank(BlockPos center, int x, int z) {
        int localX = x - center.getX();
        double riverCenter = center.getZ() + 105.0
                + 45.0 * Math.sin(localX / 70.0)
                + 18.0 * Math.sin(localX / 27.0);
        double width = 11.0 + 2.5 * Math.sin(localX / 31.0);
        double distance = Math.abs(z - riverCenter);
        return distance > width && distance <= width + 4.0;
    }

    private static boolean isRoadCoordinate(BlockPos center, int x, int z) {
        int localX = x - center.getX();
        double roadCenter = center.getZ() + 18.0 + 16.0 * Math.sin((localX + 30.0) / 55.0);
        return Math.abs(z - roadCenter) <= 2.4;
    }

    private static int fieldAt(int x, int z) {
        if (x >= 58 && x <= 172 && z >= -170 && z <= -82) {
            return 1;
        }
        if (x >= -188 && x <= -82 && z >= 68 && z <= 160) {
            return 2;
        }
        if (x >= 92 && x <= 205 && z >= 86 && z <= 174) {
            return 3;
        }
        if (x >= -185 && x <= -78 && z >= -182 && z <= -104) {
            return 1;
        }
        return 0;
    }

    private static void buildFieldColumn(ServerLevel level, int x, int groundY, int z, int field) {
        if (Math.floorMod(x, 12) == 0) {
            set(level, new BlockPos(x, groundY, z), Blocks.WATER.defaultBlockState());
            return;
        }
        if (Math.floorMod(x, 24) == 1 || Math.floorMod(z, 24) == 1) {
            set(level, new BlockPos(x, groundY, z), Blocks.DIRT_PATH.defaultBlockState());
            return;
        }

        set(level, new BlockPos(x, groundY, z), Blocks.FARMLAND.defaultBlockState());
        BlockState crop = switch (field) {
            case 1 -> Blocks.WHEAT.defaultBlockState();
            case 2 -> Blocks.CARROTS.defaultBlockState();
            default -> Blocks.POTATOES.defaultBlockState();
        };
        set(level, new BlockPos(x, groundY + 1, z), crop);
    }

    private static void buildMeadowColumn(ServerLevel level, int x, int groundY, int z) {
        int decoration = Math.floorMod(x * 73428767 ^ z * 912931, 113);
        BlockState plant = switch (decoration) {
            case 0 -> Blocks.DANDELION.defaultBlockState();
            case 1 -> Blocks.POPPY.defaultBlockState();
            case 2, 3 -> Blocks.SHORT_GRASS.defaultBlockState();
            default -> null;
        };
        if (plant != null) {
            set(level, new BlockPos(x, groundY + 1, z), plant);
        }
    }

    private static boolean isMeadowTreeAnchor(BlockPos center, int x, int z) {
        int localX = x - center.getX();
        int localZ = z - center.getZ();
        if (Math.abs(localX) < 50 && Math.abs(localZ) < 48) {
            return false;
        }
        if (isRiverCoordinate(center, x, z) || isRiverBank(center, x, z) || isRoadCoordinate(center, x, z)) {
            return false;
        }
        if (fieldAt(localX, localZ) != 0) {
            return false;
        }
        return Math.floorMod(x, 32) == 8
                && Math.floorMod(z, 32) == 8
                && Math.floorMod(x * 31 + z * 17, 5) <= 1;
    }

    private static void buildTree(ServerLevel level, BlockPos base) {
        for (int y = 0; y <= 4; y++) {
            set(level, base.above(y), Blocks.OAK_LOG.defaultBlockState());
        }
        for (int y = 3; y <= 5; y++) {
            int radius = y == 5 ? 1 : 2;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) <= radius + 1) {
                        set(level, base.offset(dx, y, dz), Blocks.OAK_LEAVES.defaultBlockState());
                    }
                }
            }
        }
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        if (!level.getBlockState(pos).equals(state)) {
            level.setBlock(pos, state, UPDATE_FLAGS);
        }
    }
}
