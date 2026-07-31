package kr.countrysidedays.world;

import kr.countrysidedays.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Optional;

/** Builds one persistent restaurant homestead on safe natural terrain. */
public final class StarterHomesteadGenerator {
    public static final int HALF_WIDTH = 14;
    public static final int HALF_DEPTH = 12;
    private static final int SEARCH_RADIUS = 112;
    private static final int SEARCH_STEP = 8;
    private static final int SAMPLE_STEP = 4;
    private static final int UPDATE_FLAGS = Block.UPDATE_ALL;

    private StarterHomesteadGenerator() {
    }

    public static Optional<BlockPos> ensureGenerated(ServerLevel level, BlockPos searchCenter) {
        CountrysideWorldData data = CountrysideWorldData.get(level.getServer());
        if (data.homesteadOrigin().isPresent()) {
            return data.homesteadOrigin();
        }

        Optional<BlockPos> candidate = findSafeOrigin(level, searchCenter);
        candidate.ifPresent(origin -> {
            buildHomestead(level, origin);
            data.claimHomesteadOrigin(origin);
            data.claimRestaurantAnchor(kitchenCounterPos(origin));
        });
        return candidate;
    }

    public static Optional<BlockPos> findSafeOrigin(ServerLevel level, BlockPos center) {
        for (int radius = 0; radius <= SEARCH_RADIUS; radius += SEARCH_STEP) {
            for (int dx = -radius; dx <= radius; dx += SEARCH_STEP) {
                for (int dz = -radius; dz <= radius; dz += SEARCH_STEP) {
                    if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }
                    BlockPos candidate = surfaceAt(level, center.getX() + dx, center.getZ() + dz);
                    if (isSafeFootprint(level, candidate)) {
                        return Optional.of(candidate);
                    }
                }
            }
        }
        return Optional.empty();
    }

    public static boolean isSafeFootprint(ServerLevel level, BlockPos origin) {
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (int dx = -HALF_WIDTH; dx <= HALF_WIDTH; dx += SAMPLE_STEP) {
            for (int dz = -HALF_DEPTH; dz <= HALF_DEPTH; dz += SAMPLE_STEP) {
                BlockPos air = surfaceAt(level, origin.getX() + dx, origin.getZ() + dz);
                BlockPos surface = air.below();
                BlockState top = level.getBlockState(surface);
                if (!isNaturalSurface(top) || !top.getFluidState().isEmpty() || !level.getFluidState(air).isEmpty()) {
                    return false;
                }
                if (level.getBlockEntity(surface) != null || level.getBlockEntity(air) != null) {
                    return false;
                }
                for (int depth = 1; depth <= 4; depth++) {
                    BlockState below = level.getBlockState(surface.below(depth));
                    if (below.isAir() || !below.getFluidState().isEmpty()) {
                        return false;
                    }
                }
                minY = Math.min(minY, air.getY());
                maxY = Math.max(maxY, air.getY());
                if (maxY - minY > 2) {
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
        buildSmallTree(level, origin.offset(-10, 0, 7));
        buildSmallTree(level, origin.offset(-3, 0, 8));
        buildYard(level, origin);
    }

    public static BlockPos kitchenCounterPos(BlockPos origin) {
        return origin.offset(-10, 1, -6);
    }

    private static BlockPos surfaceAt(ServerLevel level, int x, int z) {
        return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
    }

    private static boolean isNaturalSurface(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL) || state.is(Blocks.MOSS_BLOCK);
    }

    private static void preparePlot(ServerLevel level, BlockPos origin) {
        for (int x = -HALF_WIDTH; x <= HALF_WIDTH; x++) {
            for (int z = -HALF_DEPTH; z <= HALF_DEPTH; z++) {
                fill(level, origin, x, 0, z, x, 10, z, Blocks.AIR.defaultBlockState());
                set(level, origin.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState());
                fill(level, origin, x, -4, z, x, -2, z, Blocks.DIRT.defaultBlockState());
            }
        }
    }

    private static void buildRestaurant(ServerLevel level, BlockPos origin) {
        int x0 = -12, x1 = -2, z0 = -8, z1 = 1;
        fill(level, origin, x0, -1, z0, x1, -1, z1, Blocks.COBBLESTONE.defaultBlockState());
        fill(level, origin, x0, 0, z0, x1, 0, z1, Blocks.SPRUCE_PLANKS.defaultBlockState());

        for (int y = 1; y <= 4; y++) {
            for (int x = x0; x <= x1; x++) {
                wall(level, origin, x, y, z0, x0, x1, z0, z1);
                wall(level, origin, x, y, z1, x0, x1, z0, z1);
            }
            for (int z = z0 + 1; z < z1; z++) {
                wall(level, origin, x0, y, z, x0, x1, z0, z1);
                wall(level, origin, x1, y, z, x0, x1, z0, z1);
            }
        }
        for (int y = 1; y <= 2; y++) {
            set(level, origin.offset(-8, y, z1), Blocks.AIR.defaultBlockState());
            set(level, origin.offset(-7, y, z1), Blocks.AIR.defaultBlockState());
        }

        fill(level, origin, x0 - 1, 5, z0 - 1, x1 + 1, 5, z1 + 1, Blocks.DEEPSLATE_TILES.defaultBlockState());
        fill(level, origin, x0, 6, z0, x1, 6, z1, Blocks.DEEPSLATE_TILES.defaultBlockState());
        fill(level, origin, x0 + 2, 7, z0 + 2, x1 - 2, 7, z1 - 2, Blocks.POLISHED_DEEPSLATE.defaultBlockState());

        set(level, kitchenCounterPos(origin), ModBlocks.COUNTRY_KITCHEN_COUNTER.get().defaultBlockState());
        set(level, origin.offset(-11, 1, -6), Blocks.FURNACE.defaultBlockState());
        set(level, origin.offset(-11, 1, -5), Blocks.BARREL.defaultBlockState());
        set(level, origin.offset(-11, 1, -4), Blocks.CRAFTING_TABLE.defaultBlockState());
        table(level, origin.offset(-4, 1, -6));
        table(level, origin.offset(-6, 1, -3));
        set(level, origin.offset(-9, 1, -1), Blocks.LANTERN.defaultBlockState());
        set(level, origin.offset(-3, 1, -1), Blocks.LANTERN.defaultBlockState());
        set(level, origin.offset(-3, 1, -7), Blocks.CAMPFIRE.defaultBlockState());
        fill(level, origin, -3, 2, -7, -3, 8, -7, Blocks.BRICKS.defaultBlockState());
    }

    private static void wall(ServerLevel level, BlockPos origin, int x, int y, int z, int x0, int x1, int z0, int z1) {
        boolean corner = (x == x0 || x == x1) && (z == z0 || z == z1);
        boolean window = y >= 2 && y <= 3 && ((z == z0 && (x == -9 || x == -5))
                || (x == x0 && (z == -5 || z == -2)) || (x == x1 && (z == -5 || z == -2)));
        set(level, origin.offset(x, y, z), corner ? Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState()
                : window ? Blocks.GLASS_PANE.defaultBlockState() : Blocks.SPRUCE_PLANKS.defaultBlockState());
    }

    private static void table(ServerLevel level, BlockPos pos) {
        set(level, pos, Blocks.OAK_FENCE.defaultBlockState());
        set(level, pos.above(), Blocks.OAK_PRESSURE_PLATE.defaultBlockState());
    }

    private static void buildFarm(ServerLevel level, BlockPos origin) {
        for (int x = 3; x <= 12; x++) {
            for (int z = -8; z <= 0; z++) {
                boolean edge = x == 3 || x == 12 || z == -8 || z == 0;
                if (edge) {
                    set(level, origin.offset(x, 0, z), Blocks.OAK_FENCE.defaultBlockState());
                } else if (x == 7) {
                    set(level, origin.offset(x, 0, z), Blocks.WATER.defaultBlockState());
                } else {
                    set(level, origin.offset(x, 0, z), Blocks.FARMLAND.defaultBlockState());
                    BlockState crop = switch (Math.floorMod(x + z, 3)) {
                        case 0 -> Blocks.WHEAT.defaultBlockState();
                        case 1 -> Blocks.CARROTS.defaultBlockState();
                        default -> Blocks.POTATOES.defaultBlockState();
                    };
                    set(level, origin.offset(x, 1, z), crop);
                }
            }
        }
        set(level, origin.offset(3, 0, -4), Blocks.AIR.defaultBlockState());
    }

    private static void buildWell(ServerLevel level, BlockPos origin) {
        int cx = 7, cz = 7;
        for (int x = cx - 2; x <= cx + 2; x++) {
            for (int z = cz - 2; z <= cz + 2; z++) {
                boolean rim = x == cx - 2 || x == cx + 2 || z == cz - 2 || z == cz + 2;
                set(level, origin.offset(x, 0, z), rim ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.WATER.defaultBlockState());
                if (!rim) {
                    set(level, origin.offset(x, -1, z), Blocks.WATER.defaultBlockState());
                }
            }
        }
        int[][] posts = {{5, 5}, {9, 5}, {5, 9}, {9, 9}};
        for (int[] post : posts) {
            fill(level, origin, post[0], 1, post[1], post[0], 3, post[1], Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());
        }
        fill(level, origin, 4, 4, 4, 10, 4, 10, Blocks.SPRUCE_SLAB.defaultBlockState());
        set(level, origin.offset(cx, 1, cz), Blocks.IRON_CHAIN.defaultBlockState());
        set(level, origin.offset(cx, 2, cz), Blocks.IRON_CHAIN.defaultBlockState());
    }

    private static void buildSmallTree(ServerLevel level, BlockPos base) {
        fill(level, base, 0, 0, 0, 0, 3, 0, Blocks.OAK_LOG.defaultBlockState());
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (Math.abs(x) + Math.abs(z) <= 3) {
                    set(level, base.offset(x, 3, z), Blocks.OAK_LEAVES.defaultBlockState());
                }
            }
        }
        fill(level, base, -1, 4, -1, 1, 4, 1, Blocks.OAK_LEAVES.defaultBlockState());
    }

    private static void buildYard(ServerLevel level, BlockPos origin) {
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
        for (int x = -HALF_WIDTH; x <= HALF_WIDTH; x += 4) {
            set(level, origin.offset(x, 0, -HALF_DEPTH), Blocks.OAK_FENCE.defaultBlockState());
            set(level, origin.offset(x, 0, HALF_DEPTH), Blocks.OAK_FENCE.defaultBlockState());
        }
        for (int z = -HALF_DEPTH; z <= HALF_DEPTH; z += 4) {
            set(level, origin.offset(-HALF_WIDTH, 0, z), Blocks.OAK_FENCE.defaultBlockState());
            set(level, origin.offset(HALF_WIDTH, 0, z), Blocks.OAK_FENCE.defaultBlockState());
        }
        set(level, origin.offset(-1, 0, 5), Blocks.HAY_BLOCK.defaultBlockState());
        set(level, origin.offset(0, 0, 5), Blocks.HAY_BLOCK.defaultBlockState());
        set(level, origin.offset(-1, 1, 5), Blocks.PUMPKIN.defaultBlockState());
        set(level, origin.offset(1, 0, 6), Blocks.COMPOSTER.defaultBlockState());
    }

    private static void fill(ServerLevel level, BlockPos origin, int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
        for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++)
            set(level, origin.offset(x, y, z), state);
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, UPDATE_FLAGS);
    }
}
