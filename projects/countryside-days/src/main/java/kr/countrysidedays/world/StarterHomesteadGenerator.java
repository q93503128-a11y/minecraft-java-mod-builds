package kr.countrysidedays.world;

import kr.countrysidedays.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Builds the restaurant core and the permanent central countryside village. */
public final class StarterHomesteadGenerator {
    public static final int HALF_WIDTH = 14;
    public static final int HALF_DEPTH = 12;
    private static final int UPDATE_FLAGS = Block.UPDATE_ALL;

    private StarterHomesteadGenerator() {
    }

    /** Core layout retained as a compact deterministic GameTest scene. */
    public static void buildHomestead(ServerLevel level, BlockPos origin) {
        preparePlot(level, origin);
        buildRestaurant(level, origin);
        buildFarm(level, origin);
        buildWell(level, origin);
        buildYard(level, origin);
    }

    /** Full playable settlement used in real superflat worlds. */
    public static void buildCompleteVillage(ServerLevel level, BlockPos origin) {
        buildHomestead(level, origin);
        buildVillageRoads(level, origin);
        buildCottage(level, origin.offset(-34, 0, -18), 12, 10, 0, true);
        buildCottage(level, origin.offset(20, 0, -18), 11, 10, 1, true);
        buildCottage(level, origin.offset(-34, 0, 27), 13, 10, 2, false);
        buildCottage(level, origin.offset(17, 0, 28), 12, 10, 0, false);
        buildBarn(level, origin.offset(35, 0, 7));
        buildMarket(level, origin);
        buildOrchard(level, origin);
        buildPond(level, origin.offset(26, 0, 45));
        buildVillageLighting(level, origin);
    }

    public static BlockPos kitchenCounterPos(BlockPos origin) {
        return origin.offset(-10, 1, -6);
    }

    private static void preparePlot(ServerLevel level, BlockPos origin) {
        clearArea(level, origin, -HALF_WIDTH, -HALF_DEPTH, HALF_WIDTH, HALF_DEPTH, 10);
        for (int x = -HALF_WIDTH; x <= HALF_WIDTH; x++) {
            for (int z = -HALF_DEPTH; z <= HALF_DEPTH; z++) {
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

    private static void buildYard(ServerLevel level, BlockPos origin) {
        fill(level, origin, -9, -1, 2, -6, -1, 5, Blocks.GRAVEL.defaultBlockState());
        for (int z = 2; z <= 18; z++) {
            int x = z < 8 ? -7 : -7 + (z - 7) / 5;
            set(level, origin.offset(x, -1, z), Blocks.PACKED_MUD.defaultBlockState());
            set(level, origin.offset(x + 1, -1, z), Math.floorMod(z, 4) == 0
                    ? Blocks.GRAVEL.defaultBlockState()
                    : Blocks.PACKED_MUD.defaultBlockState());
        }
        fill(level, origin, 2, -1, 3, 12, -1, 12, Blocks.COARSE_DIRT.defaultBlockState());
        set(level, origin.offset(-1, 0, 5), Blocks.HAY_BLOCK.defaultBlockState());
        set(level, origin.offset(0, 0, 5), Blocks.HAY_BLOCK.defaultBlockState());
        set(level, origin.offset(-1, 1, 5), Blocks.PUMPKIN.defaultBlockState());
        set(level, origin.offset(1, 0, 6), Blocks.COMPOSTER.defaultBlockState());
    }

    private static void buildVillageRoads(ServerLevel level, BlockPos origin) {
        for (int x = -46; x <= 52; x++) {
            for (int z = 17; z <= 21; z++) {
                set(level, origin.offset(x, -1, z), Math.floorMod(x + z, 7) == 0
                        ? Blocks.GRAVEL.defaultBlockState()
                        : Blocks.PACKED_MUD.defaultBlockState());
            }
        }
        for (int z = -27; z <= 56; z++) {
            for (int x = -1; x <= 2; x++) {
                if (z >= 4 && z <= 12 && x >= 1) {
                    continue;
                }
                set(level, origin.offset(x, -1, z), Math.floorMod(x * 3 + z, 8) == 0
                        ? Blocks.GRAVEL.defaultBlockState()
                        : Blocks.PACKED_MUD.defaultBlockState());
            }
        }
    }

    private static void buildCottage(
            ServerLevel level,
            BlockPos base,
            int width,
            int depth,
            int style,
            boolean frontAtMaxZ
    ) {
        clearArea(level, base, -1, -1, width, depth, 10);
        BlockState wall = switch (style) {
            case 1 -> Blocks.BIRCH_PLANKS.defaultBlockState();
            case 2 -> Blocks.OAK_PLANKS.defaultBlockState();
            default -> Blocks.SPRUCE_PLANKS.defaultBlockState();
        };
        BlockState beam = switch (style) {
            case 1 -> Blocks.STRIPPED_BIRCH_LOG.defaultBlockState();
            case 2 -> Blocks.STRIPPED_OAK_LOG.defaultBlockState();
            default -> Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState();
        };
        BlockState roof = style == 1 ? Blocks.BRICKS.defaultBlockState() : Blocks.DARK_OAK_PLANKS.defaultBlockState();

        fill(level, base, 0, -1, 0, width - 1, -1, depth - 1, Blocks.COBBLESTONE.defaultBlockState());
        fill(level, base, 0, 0, 0, width - 1, 0, depth - 1, Blocks.OAK_PLANKS.defaultBlockState());
        for (int y = 1; y <= 4; y++) {
            for (int x = 0; x < width; x++) {
                cottageWall(level, base, x, y, 0, width, depth, wall, beam);
                cottageWall(level, base, x, y, depth - 1, width, depth, wall, beam);
            }
            for (int z = 1; z < depth - 1; z++) {
                cottageWall(level, base, 0, y, z, width, depth, wall, beam);
                cottageWall(level, base, width - 1, y, z, width, depth, wall, beam);
            }
        }

        int doorX = width / 2;
        int doorZ = frontAtMaxZ ? depth - 1 : 0;
        set(level, base.offset(doorX, 1, doorZ), Blocks.AIR.defaultBlockState());
        set(level, base.offset(doorX, 2, doorZ), Blocks.AIR.defaultBlockState());
        fill(level, base, -1, 5, -1, width, 5, depth, roof);
        fill(level, base, 0, 6, 0, width - 1, 6, depth - 1, roof);
        if (width > 8 && depth > 7) {
            fill(level, base, 2, 7, 2, width - 3, 7, depth - 3, Blocks.DARK_OAK_SLAB.defaultBlockState());
        }

        set(level, base.offset(2, 1, 2), Blocks.BARREL.defaultBlockState());
        set(level, base.offset(width - 3, 1, 2), Blocks.CRAFTING_TABLE.defaultBlockState());
        set(level, base.offset(2, 1, depth - 3), Blocks.LANTERN.defaultBlockState());
        set(level, base.offset(width - 3, 1, depth - 3), Blocks.WHITE_BED.defaultBlockState());
    }

    private static void cottageWall(
            ServerLevel level,
            BlockPos base,
            int x,
            int y,
            int z,
            int width,
            int depth,
            BlockState wall,
            BlockState beam
    ) {
        boolean corner = (x == 0 || x == width - 1) && (z == 0 || z == depth - 1);
        boolean window = y == 2 && (((z == 0 || z == depth - 1) && (x == 2 || x == width - 3))
                || ((x == 0 || x == width - 1) && (z == 2 || z == depth - 3)));
        set(level, base.offset(x, y, z), corner ? beam : window ? Blocks.GLASS_PANE.defaultBlockState() : wall);
    }

    private static void buildBarn(ServerLevel level, BlockPos base) {
        int width = 16, depth = 13;
        clearArea(level, base, -1, -1, width, depth, 12);
        fill(level, base, 0, -1, 0, width - 1, -1, depth - 1, Blocks.COBBLESTONE.defaultBlockState());
        fill(level, base, 0, 0, 0, width - 1, 0, depth - 1, Blocks.COARSE_DIRT.defaultBlockState());
        for (int y = 1; y <= 6; y++) {
            for (int x = 0; x < width; x++) {
                boolean beam = x == 0 || x == width - 1 || x % 5 == 0;
                set(level, base.offset(x, y, 0), beam ? Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState() : Blocks.SPRUCE_PLANKS.defaultBlockState());
                set(level, base.offset(x, y, depth - 1), beam ? Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState() : Blocks.SPRUCE_PLANKS.defaultBlockState());
            }
            for (int z = 1; z < depth - 1; z++) {
                set(level, base.offset(0, y, z), z % 4 == 0 ? Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState() : Blocks.SPRUCE_PLANKS.defaultBlockState());
                set(level, base.offset(width - 1, y, z), z % 4 == 0 ? Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState() : Blocks.SPRUCE_PLANKS.defaultBlockState());
            }
        }
        for (int x = 5; x <= 10; x++) {
            for (int y = 1; y <= 4; y++) {
                set(level, base.offset(x, y, 0), Blocks.AIR.defaultBlockState());
            }
        }
        fill(level, base, -1, 7, -1, width, 7, depth, Blocks.DARK_OAK_PLANKS.defaultBlockState());
        fill(level, base, 1, 8, 1, width - 2, 8, depth - 2, Blocks.DARK_OAK_PLANKS.defaultBlockState());
        fill(level, base, 3, 9, 3, width - 4, 9, depth - 4, Blocks.DARK_OAK_SLAB.defaultBlockState());
        fill(level, base, 2, 1, 2, 4, 2, 4, Blocks.HAY_BLOCK.defaultBlockState());
        set(level, base.offset(12, 1, 3), Blocks.BARREL.defaultBlockState());
        set(level, base.offset(12, 1, 5), Blocks.BARREL.defaultBlockState());
        set(level, base.offset(2, 1, 9), Blocks.LANTERN.defaultBlockState());
        set(level, base.offset(13, 1, 9), Blocks.LANTERN.defaultBlockState());
    }

    private static void buildMarket(ServerLevel level, BlockPos origin) {
        buildMarketStall(level, origin.offset(-16, 0, 12), Blocks.RED_WOOL.defaultBlockState());
        buildMarketStall(level, origin.offset(-4, 0, 12), Blocks.WHITE_WOOL.defaultBlockState());
        buildMarketStall(level, origin.offset(8, 0, 12), Blocks.YELLOW_WOOL.defaultBlockState());
    }

    private static void buildMarketStall(ServerLevel level, BlockPos base, BlockState canopy) {
        clearArea(level, base, 0, 0, 7, 5, 6);
        fill(level, base, 0, -1, 0, 7, -1, 5, Blocks.COARSE_DIRT.defaultBlockState());
        int[][] posts = {{0, 0}, {7, 0}, {0, 5}, {7, 5}};
        for (int[] post : posts) {
            fill(level, base, post[0], 0, post[1], post[0], 3, post[1], Blocks.OAK_FENCE.defaultBlockState());
        }
        fill(level, base, 0, 4, 0, 7, 4, 5, canopy);
        fill(level, base, 1, 1, 1, 6, 1, 1, Blocks.OAK_SLAB.defaultBlockState());
        set(level, base.offset(1, 0, 4), Blocks.BARREL.defaultBlockState());
        set(level, base.offset(6, 0, 4), Blocks.BARREL.defaultBlockState());
    }

    private static void buildOrchard(ServerLevel level, BlockPos origin) {
        for (int x = -34; x <= -14; x += 7) {
            for (int z = 42; z <= 56; z += 7) {
                buildSmallTree(level, origin.offset(x, 0, z));
            }
        }
        for (int x = -38; x <= -10; x++) {
            set(level, origin.offset(x, 0, 38), Blocks.OAK_FENCE.defaultBlockState());
            set(level, origin.offset(x, 0, 60), Blocks.OAK_FENCE.defaultBlockState());
        }
        for (int z = 39; z < 60; z++) {
            set(level, origin.offset(-38, 0, z), Blocks.OAK_FENCE.defaultBlockState());
            set(level, origin.offset(-10, 0, z), Blocks.OAK_FENCE.defaultBlockState());
        }
        set(level, origin.offset(-24, 0, 38), Blocks.AIR.defaultBlockState());
        set(level, origin.offset(-23, 0, 38), Blocks.AIR.defaultBlockState());
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

    private static void buildPond(ServerLevel level, BlockPos center) {
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                double distance = (dx * dx) / 36.0 + (dz * dz) / 25.0;
                if (distance <= 1.0) {
                    set(level, center.offset(dx, -1, dz), Blocks.WATER.defaultBlockState());
                    set(level, center.offset(dx, -2, dz), Math.floorMod(dx + dz, 4) == 0
                            ? Blocks.CLAY.defaultBlockState()
                            : Blocks.GRAVEL.defaultBlockState());
                    set(level, center.offset(dx, 0, dz), Blocks.AIR.defaultBlockState());
                } else if (distance <= 1.35) {
                    set(level, center.offset(dx, -1, dz), Math.floorMod(dx * 7 + dz, 3) == 0
                            ? Blocks.GRAVEL.defaultBlockState()
                            : Blocks.COARSE_DIRT.defaultBlockState());
                }
            }
        }
        set(level, center.offset(-2, 0, 0), Blocks.LILY_PAD.defaultBlockState());
        set(level, center.offset(2, 0, 1), Blocks.LILY_PAD.defaultBlockState());
    }

    private static void buildVillageLighting(ServerLevel level, BlockPos origin) {
        int[][] lamps = {
                {-22, 16}, {-10, 22}, {6, 22}, {22, 16}, {39, 19},
                {-3, -15}, {5, -15}, {-18, 34}, {12, 36}, {31, 36}
        };
        for (int[] lamp : lamps) {
            buildLamp(level, origin.offset(lamp[0], 0, lamp[1]));
        }
    }

    private static void buildLamp(ServerLevel level, BlockPos base) {
        fill(level, base, 0, 0, 0, 0, 2, 0, Blocks.SPRUCE_FENCE.defaultBlockState());
        set(level, base.above(3), Blocks.LANTERN.defaultBlockState());
    }

    private static void clearArea(ServerLevel level, BlockPos origin, int x0, int z0, int x1, int z1, int height) {
        fill(level, origin, x0, 0, z0, x1, height, z1, Blocks.AIR.defaultBlockState());
    }

    private static void fill(ServerLevel level, BlockPos origin, int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    set(level, origin.offset(x, y, z), state);
                }
            }
        }
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, UPDATE_FLAGS);
    }
}
