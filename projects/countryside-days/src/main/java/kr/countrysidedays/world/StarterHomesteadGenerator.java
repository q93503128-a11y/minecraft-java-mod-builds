package kr.countrysidedays.world;

import kr.countrysidedays.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Finds a quiet, naturally flat patch of overworld terrain and builds the first
 * restaurant homestead without relying on a pre-generated world or commands.
 */
public final class StarterHomesteadGenerator {
    public static final int HALF_WIDTH = 14;
    public static final int HALF_DEPTH = 12;
    private static final int SEARCH_RADIUS = 112;
    private static final int SEARCH_STEP = 8;
    private static final int SAMPLE_STEP = 4;
    private static final int MAX_HEIGHT_DELTA = 2;
    private static final int UPDATE_FLAGS = Block.UPDATE_ALL;

    private StarterHomesteadGenerator() {
    }

    public static Optional<BlockPos> ensureGenerated(ServerLevel level, BlockPos searchCenter) {
        CountrysideWorldData data = CountrysideWorldData.get(level.getServer());
        Optional<BlockPos> existing = data.homesteadOrigin();
        if (existing.isPresent()) {
            return existing;
        }

        Optional<BlockPos> candidate = findSafeOrigin(level, searchCenter);
        if (candidate.isEmpty()) {
            return Optional.empty();
        }

        BlockPos origin = candidate.get();
        buildHomestead(level, origin);
        data.claimHomesteadOrigin(origin);
        data.claimRestaurantAnchor(kitchenCounterPos(origin));
        return Optional.of(origin);
    }

    public static Optional<BlockPos> findSafeOrigin(ServerLevel level, BlockPos searchCenter) {
        int steps = SEARCH_RADIUS / SEARCH_STEP;
        return IntStream.rangeClosed(-steps, steps)
                .boxed()
                .flatMap(dx -> IntStream.rangeClosed(-steps, steps)
                        .mapToObj(dz -> new GridOffset(dx * SEARCH_STEP, dz * SEARCH_STEP)))
                .sorted(Comparator.comparingInt(GridOffset::distanceSquared))
                .map(offset -> surfaceAt(level, searchCenter.getX() + offset.x(), searchCenter.getZ() + offset.z()))
                .filter(origin -> isSafeFootprint(level, origin))
                .findFirst();
    }

    public static boolean isSafeFootprint(ServerLevel level, BlockPos origin) {
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (int dx = -HALF_WIDTH; dx <= HALF_WIDTH; dx += SAMPLE_STEP) {
            for (int dz = -HALF_DEPTH; dz <= HALF_DEPTH; dz += SAMPLE_STEP) {
                BlockPos sample = surfaceAt(level, origin.getX() + dx, origin.getZ() + dz);
                BlockPos surface = sample.below();
                BlockState surfaceState = level.getBlockState(surface);

                if (!isNaturalSurface(surfaceState) || !surfaceState.getFluidState().isEmpty()) {
                    return false;
                }
                if (!level.getFluidState(sample).isEmpty()) {
                    return false;
                }
                if (level.getBlockEntity(surface) != null || level.getBlockEntity(sample) != null) {
                    return false;
                }
                for (int depth = 1; depth <= 4; depth++) {
                    BlockState below = level.getBlockState(surface.below(depth));
                    if (below.isAir() || !below.getFluidState().isEmpty()) {
                        return false;
                    }
                }

                minY = Math.min(minY, sample.getY());
                maxY = Math.max(maxY, sample.getY());
                if (maxY - minY > MAX_HEIGHT_DELTA) {
                    return false;
                }
            }
        }

        return origin.getY() > level.getSeaLevel() + 1;
    }

    public static void buildHomestead(ServerLevel level, BlockPos origin) {
        preparePlot(level, origin);
        buildRestaurant(level, origin);
        buildFarm(level, origin);
        buildWell(level, origin);
        buildOrchard(level, origin);
        buildPathsAndYard(level, origin);
        addBoundaryDetails(level, origin);
    }

    public static BlockPos kitchenCounterPos(BlockPos origin) {
        return origin.offset(-10, 1, -6);
    }

    private static BlockPos surfaceAt(ServerLevel level, int x, int z) {
        return level.getHeightmapPos(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                new BlockPos(x, 0, z)
        );
    }

    private static boolean isNaturalSurface(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.MOSS_BLOCK);
    }

    private static void preparePlot(ServerLevel level, BlockPos origin) {
        for (int dx = -HALF_WIDTH; dx <= HALF_WIDTH; dx++) {
            for (int dz = -HALF_DEPTH; dz <= HALF_DEPTH; dz++) {
                for (int y = 0; y <= 10; y++) {
                    set(level, origin.offset(dx, y, dz), Blocks.AIR.defaultBlockState());
                }
                set(level, origin.offset(dx, -1, dz), Blocks.GRASS_BLOCK.defaultBlockState());
                for (int depth = 2; depth <= 4; depth++) {
                    set(level, origin.offset(dx, -depth, dz), Blocks.DIRT.defaultBlockState());
                }
            }
        }
    }

    private static void buildRestaurant(ServerLevel level, BlockPos origin) {
        int minX = -12;
        int maxX = -2;
        int minZ = -8;
        int maxZ = 1;

        fill(level, origin, minX, 0, minZ, maxX, 0, maxZ, Blocks.SPRUCE_PLANKS.defaultBlockState());
        fill(level, origin, minX, -1, minZ, maxX, -1, maxZ, Blocks.COBBLESTONE.defaultBlockState());

        for (int y = 1; y <= 4; y++) {
            for (int x = minX; x <= maxX; x++) {
                placeWallBlock(level, origin, x, y, minZ, minX, maxX, minZ, maxZ);
                placeWallBlock(level, origin, x, y, maxZ, minX, maxX, minZ, maxZ);
            }
            for (int z = minZ + 1; z < maxZ; z++) {
                placeWallBlock(level, origin, minX, y, z, minX, maxX, minZ, maxZ);
                placeWallBlock(level, origin, maxX, y, z, minX, maxX, minZ, maxZ);
            }
        }

        // Broad open doorway facing the yard.
        for (int y = 1; y <= 2; y++) {
            set(level, origin.offset(-8, y, maxZ), Blocks.AIR.defaultBlockState());
            set(level, origin.offset(-7, y, maxZ), Blocks.AIR.defaultBlockState());
        }

        // Low, layered dark roof with deep eaves.
        fill(level, origin, minX - 1, 5, minZ - 1, maxX + 1, 5, maxZ + 1, Blocks.DEEPSLATE_TILES.defaultBlockState());
        fill(level, origin, minX, 6, minZ, maxX, 6, maxZ, Blocks.DEEPSLATE_TILES.defaultBlockState());
        fill(level, origin, minX + 2, 7, minZ + 2, maxX - 2, 7, maxZ - 2, Blocks.POLISHED_DEEPSLATE.defaultBlockState());

        // Kitchen and dining furniture.
        set(level, kitchenCounterPos(origin), ModBlocks.COUNTRY_KITCHEN_COUNTER.get().defaultBlockState());
        set(level, origin.offset(-11, 1, -6), Blocks.FURNACE.defaultBlockState());
        set(level, origin.offset(-11, 1, -5), Blocks.BARREL.defaultBlockState());
        set(level, origin.offset(-11, 1, -4), Blocks.CRAFTING_TABLE.defaultBlockState());
        set(level, origin.offset(-4, 1, -6), Blocks.OAK_FENCE.defaultBlockState());
        set(level, origin.offset(-4, 2, -6), Blocks.OAK_PRESSURE_PLATE.defaultBlockState());
        set(level, origin.offset(-6, 1, -3), Blocks.OAK_FENCE.defaultBlockState());
        set(level, origin.offset(-6, 2, -3), Blocks.OAK_PRESSURE_PLATE.defaultBlockState());
        set(level, origin.offset(-9, 1, -1), Blocks.LANTERN.defaultBlockState());
        set(level, origin.offset(-3, 1, -1), Blocks.LANTERN.defaultBlockState());

        // A real hearth and chimney rather than a decorative particle cloud.
        set(level, origin.offset(-3, 1, -7), Blocks.CAMPFIRE.defaultBlockState());
        for (int y = 2; y <= 8; y++) {
            set(level, origin.offset(-3, y, -7), Blocks.BRICKS.defaultBlockState());
        }
    }

    private static void placeWallBlock(
            ServerLevel level,
            BlockPos origin,
            int x,
            int y,
            int z,
            int minX,
            int maxX,
            int minZ,
            int maxZ
    ) {
        boolean corner = (x == minX || x == maxX) && (z == minZ || z == maxZ);
        boolean window = y >= 2 && y <= 3 && (
                (z == minZ && (x == -9 || x == -5))
                        || (x == minX && (z == -5 || z == -2))
                        || (x == maxX && (z == -5 || z == -2))
        );
        BlockState state = corner
                ? Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState()
                : window ? Blocks.GLASS_PANE.defaultBlockState() : Blocks.SPRUCE_PLANKS.defaultBlockState();
        set(level, origin.offset(x, y, z), state);
    }

    private static void buildFarm(ServerLevel level, BlockPos origin) {
        int minX = 3;
        int maxX = 12;
        int minZ = -8;
        int maxZ = 0;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                boolean edge = x == minX || x == maxX || z == minZ || z == maxZ;
                if (edge) {
                    set(level, origin.offset(x, 0, z), Blocks.OAK_FENCE.defaultBlockState());
                    continue;
                }
                if (x == 7) {
                    set(level, origin.offset(x, -1, z), Blocks.DIRT.defaultBlockState());
                    set(level, origin.offset(x, 0, z), Blocks.WATER.defaultBlockState());
                    continue;
                }

                set(level, origin.offset(x, 0, z), Blocks.FARMLAND.defaultBlockState());
                BlockState crop = switch (Math.floorMod(x + z, 3)) {
                    case 0 -> Blocks.WHEAT.defaultBlockState();
                    case 1 -> Blocks.CARROTS.defaultBlockState();
                    default -> Blocks.POTATOES.defaultBlockState();
                };
                set(level, origin.offset(x, 1, z), crop);
            }
        }
        set(level, origin.offset(3, 0, -4), Blocks.AIR.defaultBlockState());
    }

    private static void buildWell(ServerLevel level, BlockPos origin) {
        int centerX = 7;
        int centerZ = 7;
        for (int x = centerX - 2; x <= centerX + 2; x++) {
            for (int z = centerZ - 2; z <= centerZ + 2; z++) {
                boolean rim = x == centerX - 2 || x == centerX + 2 || z == centerZ - 2 || z == centerZ + 2;
                if (rim) {
                    set(level, origin.offset(x, 0, z), Blocks.STONE_BRICKS.defaultBlockState());
                } else {
                    set(level, origin.offset(x, -1, z), Blocks.WATER.defaultBlockState());
                    set(level, origin.offset(x, 0, z), Blocks.WATER.defaultBlockState());
                }
            }
        }

        int[][] posts = {{centerX - 2, centerZ - 2}, {centerX + 2, centerZ - 2}, {centerX - 2, centerZ + 2}, {centerX + 2, centerZ + 2}};
        for (int[] post : posts) {
            for (int y = 1; y <= 3; y++) {
                set(level, origin.offset(post[0], y, post[1]), Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());
            }
        }
        fill(level, origin, centerX - 3, 4, centerZ - 3, centerX + 3, 4, centerZ + 3, Blocks.SPRUCE_SLAB.defaultBlockState());
        set(level, origin.offset(centerX, 1, centerZ), Blocks.CHAIN.defaultBlockState());
        set(level, origin.offset(centerX, 2, centerZ), Blocks.CHAIN.defaultBlockState());
    }

    private static void buildOrchard(ServerLevel level, BlockPos origin) {
        buildSmallTree(level, origin.offset(-10, 0, 7));
        buildSmallTree(level, origin.offset(-3, 0, 8));
    }

    private static void buildSmallTree(ServerLevel level, BlockPos base) {
        for (int y = 0; y <= 3; y++) {
            set(level, base.offset(0, y, 0), Blocks.OAK_LOG.defaultBlockState());
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) + Math.abs(dz) <= 3) {
                    set(level, base.offset(dx, 3, dz), Blocks.OAK_LEAVES.defaultBlockState());
                }
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                set(level, base.offset(dx, 4, dz), Blocks.OAK_LEAVES.defaultBlockState());
            }
        }
    }

    private static void buildPathsAndYard(ServerLevel level, BlockPos origin) {
        // Restaurant apron and a gently bent lane to the southern edge.
        fill(level, origin, -9, -1, 2, -6, -1, 5, Blocks.GRAVEL.defaultBlockState());
        for (int z = 2; z <= HALF_DEPTH; z++) {
            int x = z < 7 ? -7 : -7 + (z - 6) / 3;
            set(level, origin.offset(x, -1, z), Blocks.DIRT_PATH.defaultBlockState());
            set(level, origin.offset(x + 1, -1, z), Blocks.DIRT_PATH.defaultBlockState());
        }

        fill(level, origin, 3, -1, 3, 11, -1, 11, Blocks.COARSE_DIRT.defaultBlockState());
        for (int x = 3; x <= 11; x += 2) {
            set(level, origin.offset(x, 0, 3), Blocks.LANTERN.defaultBlockState());
        }
    }

    private static void addBoundaryDetails(ServerLevel level, BlockPos origin) {
        for (int x = -HALF_WIDTH; x <= HALF_WIDTH; x++) {
            if (x % 4 == 0) {
                set(level, origin.offset(x, 0, -HALF_DEPTH), Blocks.OAK_FENCE.defaultBlockState());
                set(level, origin.offset(x, 0, HALF_DEPTH), Blocks.OAK_FENCE.defaultBlockState());
            }
        }
        for (int z = -HALF_DEPTH; z <= HALF_DEPTH; z++) {
            if (z % 4 == 0) {
                set(level, origin.offset(-HALF_WIDTH, 0, z), Blocks.OAK_FENCE.defaultBlockState());
                set(level, origin.offset(HALF_WIDTH, 0, z), Blocks.OAK_FENCE.defaultBlockState());
            }
        }

        set(level, origin.offset(-1, 0, 5), Blocks.HAY_BLOCK.defaultBlockState());
        set(level, origin.offset(0, 0, 5), Blocks.HAY_BLOCK.defaultBlockState());
        set(level, origin.offset(-1, 1, 5), Blocks.PUMPKIN.defaultBlockState());
        set(level, origin.offset(1, 0, 6), Blocks.COMPOSTER.defaultBlockState());
    }

    private static void fill(
            ServerLevel level,
            BlockPos origin,
            int minX,
            int minY,
            int minZ,
            int maxX,
            int maxY,
            int maxZ,
            BlockState state
    ) {
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    set(level, origin.offset(x, y, z), state);
                }
            }
        }
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, UPDATE_FLAGS);
    }

    private record GridOffset(int x, int z) {
        private int distanceSquared() {
            return x * x + z * z;
        }
    }
}
