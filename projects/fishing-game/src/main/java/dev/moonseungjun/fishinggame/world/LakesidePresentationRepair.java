package dev.moonseungjun.fishinggame.world;

import dev.moonseungjun.fishinggame.FishingGameMod;
import dev.moonseungjun.fishinggame.entity.EncounterFishEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class LakesidePresentationRepair {
    private static final BlockPos BASE_BUILD_MARKER = new BlockPos(0, 58, 27);
    private static final BlockPos QUALITY_MARKER_ALPHA20 = new BlockPos(0, 40, 27);
    private static final BlockPos QUALITY_MARKER_ALPHA21 = new BlockPos(1, 40, 27);
    private static final BlockPos QUALITY_MARKER_ALPHA22 = new BlockPos(2, 40, 27);
    private static final int WATER_Y = 63;
    private static final int LAND_Y = 64;
    private static final int ORIGINAL_OUTER_X = 44;
    private static final int ORIGINAL_OUTER_Z = 38;
    private static final int LAKE_X = 21;
    private static final int LAKE_Z = 17;
    private static final int SCENIC_X = 76;
    private static final int SCENIC_Z = 68;
    private static final AABB AMBIENT_MOB_BOUNDS = new AABB(-78.0, 48.0, -70.0, 78.0, 100.0, 70.0);
    private static final AABB LAKE_DEBRIS_BOUNDS = new AABB(-24.0, 54.0, -20.0, 24.0, 72.0, 20.0);
    private static int maintenanceTicks;

    private LakesidePresentationRepair() {
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            maintenanceTicks++;
            if (maintenanceTicks < 20) return;
            maintenanceTicks = 0;

            ServerLevel lakeside = server.getLevel(FishingWorldManager.LAKESIDE_LEVEL);
            if (lakeside == null) return;

            if (lakeside.getBlockState(BASE_BUILD_MARKER).is(Blocks.LODESTONE)
                    && !lakeside.getBlockState(QUALITY_MARKER_ALPHA20).is(Blocks.EMERALD_BLOCK)) {
                FishingGameMod.LOGGER.info("Applying alpha.20 Cheongram Lakeside presentation repair");
                applyAlpha20(lakeside);
                set(lakeside, QUALITY_MARKER_ALPHA20, Blocks.EMERALD_BLOCK);
            }

            if (lakeside.getBlockState(BASE_BUILD_MARKER).is(Blocks.LODESTONE)
                    && !lakeside.getBlockState(QUALITY_MARKER_ALPHA21).is(Blocks.DIAMOND_BLOCK)) {
                FishingGameMod.LOGGER.info("Applying alpha.21 Cheongram Lakeside water cleanup");
                cleanupLakeDebris(lakeside);
                set(lakeside, QUALITY_MARKER_ALPHA21, Blocks.DIAMOND_BLOCK);
            }

            if (lakeside.getBlockState(BASE_BUILD_MARKER).is(Blocks.LODESTONE)
                    && !lakeside.getBlockState(QUALITY_MARKER_ALPHA22).is(Blocks.GOLD_BLOCK)) {
                FishingGameMod.LOGGER.info("Applying alpha.22 Cheongram Lakeside debris cleanup");
                cleanupLakeDebris(lakeside);
                removeFloatingLakeDebris(lakeside);
                set(lakeside, QUALITY_MARKER_ALPHA22, Blocks.GOLD_BLOCK);
            }

            removeFloatingLakeDebris(lakeside);
            removeAmbientMobs(lakeside);
        });
    }

    static void applyAlpha20(ServerLevel level) {
        authorLakeBed(level);
        authorInlandBasin(level);
        buildScenicTreeBelt(level);
        buildRidgeRockwork(level);
    }

    private static void cleanupLakeDebris(ServerLevel level) {
        for (int x = -LAKE_X; x <= LAKE_X; x++) {
            for (int z = -LAKE_Z; z <= LAKE_Z; z++) {
                double lake = ellipse(x, z, LAKE_X, LAKE_Z);
                if (lake > 0.97) continue;

                int bedY = lakeBedY(lake);
                for (int y = bedY + 1; y <= WATER_Y; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir() || isLakeDebris(state)) {
                        set(level, pos, Blocks.WATER);
                    }
                }

                for (int y = WATER_Y + 1; y <= LAND_Y + 6; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (isLakeDebris(level.getBlockState(pos))) {
                        set(level, pos, Blocks.AIR);
                    }
                }
            }
        }
    }

    private static void removeFloatingLakeDebris(ServerLevel level) {
        for (ItemEntity itemEntity : level.getEntitiesOfClass(
                ItemEntity.class,
                LAKE_DEBRIS_BOUNDS,
                itemEntity -> itemEntity.getItem().getItem() instanceof BlockItem
        )) {
            itemEntity.discard();
        }
        for (FallingBlockEntity fallingBlock : level.getEntitiesOfClass(
                FallingBlockEntity.class,
                LAKE_DEBRIS_BOUNDS
        )) {
            fallingBlock.discard();
        }
    }

    private static void authorLakeBed(ServerLevel level) {
        for (int x = -LAKE_X; x <= LAKE_X; x++) {
            for (int z = -LAKE_Z; z <= LAKE_Z; z++) {
                double lake = ellipse(x, z, LAKE_X, LAKE_Z);
                if (lake > 1.0) continue;

                int bedY = lakeBedY(lake);
                for (int y = 56; y <= bedY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (!isLakeNatural(state)) continue;

                    Block block;
                    if (y == bedY) {
                        block = lakeBedTop(x, z);
                    } else if (y == bedY - 1) {
                        block = Blocks.CLAY;
                    } else {
                        block = Blocks.STONE;
                    }
                    set(level, pos, block);
                }

                for (int y = bedY + 1; y <= WATER_Y; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (isLakeNatural(state) || state.isAir()) set(level, pos, Blocks.WATER);
                }
            }
        }
    }

    private static int lakeBedY(double lake) {
        double distance = Math.sqrt(lake);
        return 56 + (int) Math.round(Math.pow(distance, 1.65) * 4.0);
    }

    private static Block lakeBedTop(int x, int z) {
        int variant = Math.floorMod(x * 17 + z * 31, 11);
        if (variant <= 2) return Blocks.GRAVEL;
        if (variant == 3) return Blocks.CLAY;
        return Blocks.SAND;
    }

    private static boolean isLakeNatural(BlockState state) {
        return state.is(Blocks.WATER)
                || state.is(Blocks.STONE)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.SAND)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY);
    }

    private static boolean isLakeDebris(BlockState state) {
        return state.is(Blocks.STONE)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.SAND)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.MOSSY_COBBLESTONE)
                || state.is(Blocks.ANDESITE)
                || state.is(Blocks.DANDELION)
                || state.is(Blocks.FERN)
                || state.is(Blocks.POPPY);
    }

    private static void authorInlandBasin(ServerLevel level) {
        for (int x = -SCENIC_X; x <= SCENIC_X; x++) {
            for (int z = -SCENIC_Z; z <= SCENIC_Z; z++) {
                double scenic = ellipse(x, z, SCENIC_X, SCENIC_Z);
                if (scenic > 1.0) continue;
                if (ellipse(x, z, ORIGINAL_OUTER_X, ORIGINAL_OUTER_Z) <= 1.0) continue;

                int surfaceY = scenicSurfaceY(x, z);
                for (int y = 56; y <= surfaceY; y++) {
                    Block block;
                    if (y <= surfaceY - 3) {
                        block = Blocks.STONE;
                    } else if (y < surfaceY) {
                        block = Blocks.DIRT;
                    } else {
                        block = scenicTopBlock(x, z, scenic);
                    }
                    set(level, new BlockPos(x, y, z), block);
                }
            }
        }
    }

    private static int scenicSurfaceY(int x, int z) {
        double radial = Math.sqrt(ellipse(x, z, SCENIC_X, SCENIC_Z));
        double t = clamp((radial - 0.55) / 0.45);
        double eased = t * t * (3.0 - 2.0 * t);
        double ripple = Math.sin(x * 0.21) * 0.7 + Math.cos(z * 0.17) * 0.6;
        return LAND_Y + 1 + (int) Math.round(eased * 10.0 + ripple);
    }

    private static Block scenicTopBlock(int x, int z, double scenic) {
        double radial = Math.sqrt(scenic);
        int variant = Math.floorMod(x * 19 + z * 23, 13);
        if (radial > 0.88) {
            if (variant <= 2) return Blocks.MOSS_BLOCK;
            if (variant <= 5) return Blocks.COARSE_DIRT;
            return Blocks.PODZOL;
        }
        if (radial < 0.68 && variant <= 2) return Blocks.GRAVEL;
        if (variant == 3) return Blocks.COARSE_DIRT;
        return Blocks.GRASS_BLOCK;
    }

    private static void buildScenicTreeBelt(ServerLevel level) {
        int[][] trees = {
                {-62, -11}, {-60, 16}, {-55, 33}, {-46, 46}, {-29, 54}, {-8, 58},
                {14, 57}, {34, 52}, {51, 40}, {62, 23}, {65, -2}, {59, -27},
                {44, -45}, {25, -55}, {1, -59}, {-22, -56}, {-42, -47}, {-56, -31}
        };
        for (int i = 0; i < trees.length; i++) {
            int x = trees[i][0];
            int z = trees[i][1];
            int groundY = scenicSurfaceY(x, z);
            buildConifer(level, x, groundY, z, 5 + (i % 3));
        }
    }

    private static void buildConifer(ServerLevel level, int x, int groundY, int z, int trunkHeight) {
        for (int y = 1; y <= trunkHeight; y++) {
            set(level, new BlockPos(x, groundY + y, z), Blocks.SPRUCE_LOG);
        }

        int top = groundY + trunkHeight;
        for (int dy = -3; dy <= 2; dy++) {
            int radius = dy <= -1 ? 2 : 1;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) > radius + 1) continue;
                    if (dx == 0 && dz == 0 && dy <= 0) continue;
                    set(level, new BlockPos(x + dx, top + dy, z + dz), Blocks.SPRUCE_LEAVES);
                }
            }
        }
        set(level, new BlockPos(x, top + 3, z), Blocks.SPRUCE_LEAVES);
    }

    private static void buildRidgeRockwork(ServerLevel level) {
        int[][] rocks = {
                {-68, 4, 2}, {-52, -39, 2}, {-18, -61, 2}, {24, -60, 2},
                {57, -33, 2}, {69, 8, 2}, {52, 43, 2}, {17, 62, 2}, {-24, 59, 2}, {-57, 37, 2}
        };
        for (int i = 0; i < rocks.length; i++) {
            int x = rocks[i][0];
            int z = rocks[i][1];
            int radius = rocks[i][2];
            int groundY = scenicSurfaceY(x, z);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz > radius * radius + 1) continue;
                    int height = 1 + Math.floorMod(dx * 11 + dz * 7 + i, 2);
                    for (int dy = 1; dy <= height; dy++) {
                        Block block = ((dx + dz + dy + i) & 3) == 0 ? Blocks.MOSSY_COBBLESTONE : Blocks.ANDESITE;
                        set(level, new BlockPos(x + dx, groundY + dy, z + dz), block);
                    }
                }
            }
        }
    }

    private static void removeAmbientMobs(ServerLevel level) {
        for (Mob mob : level.getEntitiesOfClass(
                Mob.class,
                AMBIENT_MOB_BOUNDS,
                mob -> !(mob instanceof EncounterFishEntity)
        )) {
            mob.discard();
        }
    }

    private static double ellipse(int x, int z, double radiusX, double radiusZ) {
        double nx = x / radiusX;
        double nz = z / radiusZ;
        return nx * nx + nz * nz;
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static void set(ServerLevel level, BlockPos pos, Block block) {
        level.setBlock(pos, block.defaultBlockState(), 2);
    }
}
