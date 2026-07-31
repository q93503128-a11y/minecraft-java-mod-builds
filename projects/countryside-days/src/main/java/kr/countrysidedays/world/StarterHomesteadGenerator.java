package kr.countrysidedays.world;

import kr.countrysidedays.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Builds a coherent central village without scattered runtime terrain edits. */
public final class StarterHomesteadGenerator {
    public static final int HALF_WIDTH = 14;
    public static final int HALF_DEPTH = 12;
    private static final int FLAGS = Block.UPDATE_ALL;

    private StarterHomesteadGenerator() {
    }

    /** Compact deterministic layout used by GameTest. */
    public static void buildHomestead(ServerLevel level, BlockPos origin) {
        flattenCore(level, origin);
        restaurant(level, origin);
        kitchenGarden(level, origin);
        well(level, origin);
        coreYard(level, origin);
    }

    /** Full playable centre used in real superflat worlds. */
    public static void buildCompleteVillage(ServerLevel level, BlockPos origin) {
        buildHomestead(level, origin);
        roads(level, origin);
        house(level, origin.offset(-39, 0, -25), 13, 10, Palette.SPRUCE, true);
        house(level, origin.offset(20, 0, -25), 12, 10, Palette.BIRCH, true);
        house(level, origin.offset(-40, 0, 30), 14, 11, Palette.OAK, false);
        house(level, origin.offset(18, 0, 31), 13, 10, Palette.SPRUCE, false);
        barn(level, origin.offset(39, 0, 4));
        market(level, origin);
        orchard(level, origin);
        paddock(level, origin.offset(-3, 0, 45));
        pond(level, origin.offset(31, 0, 52));
        lamps(level, origin);
    }

    public static BlockPos kitchenCounterPos(BlockPos origin) {
        return origin.offset(-10, 1, -6);
    }

    private static void flattenCore(ServerLevel level, BlockPos origin) {
        fill(level, origin, -HALF_WIDTH, 0, -HALF_DEPTH, HALF_WIDTH, 10, HALF_DEPTH, Blocks.AIR.defaultBlockState());
        for (int x = -HALF_WIDTH; x <= HALF_WIDTH; x++) {
            for (int z = -HALF_DEPTH; z <= HALF_DEPTH; z++) {
                set(level, origin.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState());
                fill(level, origin, x, -4, z, x, -2, z, Blocks.DIRT.defaultBlockState());
            }
        }
    }

    private static void restaurant(ServerLevel level, BlockPos o) {
        int x0 = -12, x1 = -2, z0 = -8, z1 = 1;
        fill(level, o, x0, -1, z0, x1, -1, z1, Blocks.COBBLESTONE.defaultBlockState());
        fill(level, o, x0, 0, z0, x1, 0, z1, Blocks.SPRUCE_PLANKS.defaultBlockState());
        for (int y = 1; y <= 4; y++) {
            for (int x = x0; x <= x1; x++) {
                restaurantWall(level, o, x, y, z0, x0, x1, z0, z1);
                restaurantWall(level, o, x, y, z1, x0, x1, z0, z1);
            }
            for (int z = z0 + 1; z < z1; z++) {
                restaurantWall(level, o, x0, y, z, x0, x1, z0, z1);
                restaurantWall(level, o, x1, y, z, x0, x1, z0, z1);
            }
        }
        fill(level, o, -8, 1, z1, -7, 3, z1, Blocks.AIR.defaultBlockState());
        fill(level, o, x0 - 1, 5, z0 - 1, x1 + 1, 5, z1 + 1, Blocks.DEEPSLATE_TILES.defaultBlockState());
        fill(level, o, x0, 6, z0, x1, 6, z1, Blocks.DEEPSLATE_TILES.defaultBlockState());
        fill(level, o, x0 + 2, 7, z0 + 2, x1 - 2, 7, z1 - 2, Blocks.POLISHED_DEEPSLATE.defaultBlockState());

        set(level, kitchenCounterPos(o), ModBlocks.COUNTRY_KITCHEN_COUNTER.get().defaultBlockState());
        set(level, o.offset(-11, 1, -6), Blocks.FURNACE.defaultBlockState());
        set(level, o.offset(-11, 1, -5), Blocks.BARREL.defaultBlockState());
        set(level, o.offset(-11, 1, -4), Blocks.CRAFTING_TABLE.defaultBlockState());
        table(level, o.offset(-4, 1, -6));
        table(level, o.offset(-6, 1, -3));
        set(level, o.offset(-9, 1, -1), Blocks.LANTERN.defaultBlockState());
        set(level, o.offset(-3, 1, -1), Blocks.LANTERN.defaultBlockState());
        set(level, o.offset(-3, 1, -7), Blocks.CAMPFIRE.defaultBlockState());
        fill(level, o, -3, 2, -7, -3, 8, -7, Blocks.BRICKS.defaultBlockState());
    }

    private static void restaurantWall(ServerLevel level, BlockPos o, int x, int y, int z, int x0, int x1, int z0, int z1) {
        boolean corner = (x == x0 || x == x1) && (z == z0 || z == z1);
        boolean window = y >= 2 && y <= 3 && ((z == z0 && (x == -9 || x == -5))
                || (x == x0 && (z == -5 || z == -2))
                || (x == x1 && (z == -5 || z == -2)));
        set(level, o.offset(x, y, z), corner
                ? Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState()
                : window ? Blocks.GLASS_PANE.defaultBlockState() : Blocks.SPRUCE_PLANKS.defaultBlockState());
    }

    private static void table(ServerLevel level, BlockPos pos) {
        set(level, pos, Blocks.OAK_FENCE.defaultBlockState());
        set(level, pos.above(), Blocks.OAK_PRESSURE_PLATE.defaultBlockState());
    }

    private static void kitchenGarden(ServerLevel level, BlockPos o) {
        for (int x = 3; x <= 12; x++) {
            for (int z = -8; z <= 0; z++) {
                boolean edge = x == 3 || x == 12 || z == -8 || z == 0;
                if (edge) {
                    set(level, o.offset(x, 0, z), Blocks.OAK_FENCE.defaultBlockState());
                } else if (x == 7) {
                    set(level, o.offset(x, 0, z), Blocks.WATER.defaultBlockState());
                } else {
                    set(level, o.offset(x, 0, z), Blocks.FARMLAND.defaultBlockState());
                    BlockState crop = switch (Math.floorMod(x + z, 3)) {
                        case 0 -> Blocks.WHEAT.defaultBlockState();
                        case 1 -> Blocks.CARROTS.defaultBlockState();
                        default -> Blocks.POTATOES.defaultBlockState();
                    };
                    set(level, o.offset(x, 1, z), crop);
                }
            }
        }
        fill(level, o, 3, 0, -4, 3, 1, -4, Blocks.AIR.defaultBlockState());
    }

    private static void well(ServerLevel level, BlockPos o) {
        for (int x = 5; x <= 9; x++) {
            for (int z = 5; z <= 9; z++) {
                boolean rim = x == 5 || x == 9 || z == 5 || z == 9;
                set(level, o.offset(x, 0, z), rim ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.WATER.defaultBlockState());
                if (!rim) set(level, o.offset(x, -1, z), Blocks.WATER.defaultBlockState());
            }
        }
        int[][] posts = {{5, 5}, {9, 5}, {5, 9}, {9, 9}};
        for (int[] p : posts) fill(level, o, p[0], 1, p[1], p[0], 3, p[1], Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());
        fill(level, o, 4, 4, 4, 10, 4, 10, Blocks.SPRUCE_SLAB.defaultBlockState());
        fill(level, o, 7, 1, 7, 7, 2, 7, Blocks.IRON_CHAIN.defaultBlockState());
    }

    private static void coreYard(ServerLevel level, BlockPos o) {
        fill(level, o, -9, -1, 2, -6, -1, 5, Blocks.GRAVEL.defaultBlockState());
        for (int z = 2; z <= 18; z++) {
            int x = z < 8 ? -7 : -7 + (z - 7) / 5;
            set(level, o.offset(x, -1, z), Blocks.PACKED_MUD.defaultBlockState());
            set(level, o.offset(x + 1, -1, z), Math.floorMod(z, 4) == 0
                    ? Blocks.GRAVEL.defaultBlockState() : Blocks.PACKED_MUD.defaultBlockState());
        }
        fill(level, o, 2, -1, 3, 12, -1, 12, Blocks.COARSE_DIRT.defaultBlockState());
        set(level, o.offset(-1, 0, 5), Blocks.HAY_BLOCK.defaultBlockState());
        set(level, o.offset(0, 0, 5), Blocks.HAY_BLOCK.defaultBlockState());
        set(level, o.offset(-1, 1, 5), Blocks.PUMPKIN.defaultBlockState());
        set(level, o.offset(1, 0, 6), Blocks.COMPOSTER.defaultBlockState());
    }

    private static void roads(ServerLevel level, BlockPos o) {
        for (int x = -52; x <= 59; x++) {
            for (int z = 17; z <= 21; z++) set(level, o.offset(x, -1, z), road(x, z));
        }
        for (int z = -34; z <= 67; z++) {
            for (int x = -2; x <= 2; x++) {
                if (z >= 3 && z <= 12 && x >= 1) continue;
                set(level, o.offset(x, -1, z), road(x, z));
            }
        }
    }

    private static BlockState road(int x, int z) {
        return Math.floorMod(x * 3 + z, 8) == 0
                ? Blocks.GRAVEL.defaultBlockState() : Blocks.PACKED_MUD.defaultBlockState();
    }

    private static void house(ServerLevel level, BlockPos b, int width, int depth, Palette palette, boolean frontAtMaxZ) {
        fill(level, b, -1, 0, -1, width, 10, depth, Blocks.AIR.defaultBlockState());
        fill(level, b, 0, -1, 0, width - 1, -1, depth - 1, Blocks.COBBLESTONE.defaultBlockState());
        fill(level, b, 0, 0, 0, width - 1, 0, depth - 1, Blocks.OAK_PLANKS.defaultBlockState());
        for (int y = 1; y <= 4; y++) {
            for (int x = 0; x < width; x++) {
                houseWall(level, b, x, y, 0, width, depth, palette);
                houseWall(level, b, x, y, depth - 1, width, depth, palette);
            }
            for (int z = 1; z < depth - 1; z++) {
                houseWall(level, b, 0, y, z, width, depth, palette);
                houseWall(level, b, width - 1, y, z, width, depth, palette);
            }
        }
        int doorX = width / 2;
        int doorZ = frontAtMaxZ ? depth - 1 : 0;
        fill(level, b, doorX, 1, doorZ, doorX, 3, doorZ, Blocks.AIR.defaultBlockState());
        fill(level, b, -1, 5, -1, width, 5, depth, palette.roof);
        fill(level, b, 0, 6, 0, width - 1, 6, depth - 1, palette.roof);
        fill(level, b, 2, 7, 2, width - 3, 7, depth - 3, Blocks.DARK_OAK_SLAB.defaultBlockState());
        set(level, b.offset(2, 1, 2), Blocks.BARREL.defaultBlockState());
        set(level, b.offset(width - 3, 1, 2), Blocks.CRAFTING_TABLE.defaultBlockState());
        set(level, b.offset(2, 1, depth - 3), Blocks.LANTERN.defaultBlockState());
        fill(level, b, width - 4, 1, depth - 3, width - 2, 1, depth - 2, Blocks.BIRCH_PLANKS.defaultBlockState());
    }

    private static void houseWall(ServerLevel level, BlockPos b, int x, int y, int z, int width, int depth, Palette p) {
        boolean corner = (x == 0 || x == width - 1) && (z == 0 || z == depth - 1);
        boolean window = y == 2 && (((z == 0 || z == depth - 1) && (x == 2 || x == width - 3))
                || ((x == 0 || x == width - 1) && (z == 2 || z == depth - 3)));
        set(level, b.offset(x, y, z), corner ? p.beam : window ? Blocks.GLASS_PANE.defaultBlockState() : p.wall);
    }

    private static void barn(ServerLevel level, BlockPos b) {
        int w = 16, d = 13;
        fill(level, b, -1, 0, -1, w, 12, d, Blocks.AIR.defaultBlockState());
        fill(level, b, 0, -1, 0, w - 1, -1, d - 1, Blocks.COBBLESTONE.defaultBlockState());
        fill(level, b, 0, 0, 0, w - 1, 0, d - 1, Blocks.COARSE_DIRT.defaultBlockState());
        for (int y = 1; y <= 6; y++) {
            for (int x = 0; x < w; x++) {
                BlockState s = (x == 0 || x == w - 1 || x % 5 == 0)
                        ? Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState() : Blocks.SPRUCE_PLANKS.defaultBlockState();
                set(level, b.offset(x, y, 0), s);
                set(level, b.offset(x, y, d - 1), s);
            }
            for (int z = 1; z < d - 1; z++) {
                BlockState s = z % 4 == 0
                        ? Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState() : Blocks.SPRUCE_PLANKS.defaultBlockState();
                set(level, b.offset(0, y, z), s);
                set(level, b.offset(w - 1, y, z), s);
            }
        }
        fill(level, b, 5, 1, 0, 10, 4, 0, Blocks.AIR.defaultBlockState());
        fill(level, b, -1, 7, -1, w, 7, d, Blocks.DARK_OAK_PLANKS.defaultBlockState());
        fill(level, b, 1, 8, 1, w - 2, 8, d - 2, Blocks.DARK_OAK_PLANKS.defaultBlockState());
        fill(level, b, 3, 9, 3, w - 4, 9, d - 4, Blocks.DARK_OAK_SLAB.defaultBlockState());
        fill(level, b, 2, 1, 2, 4, 2, 4, Blocks.HAY_BLOCK.defaultBlockState());
        set(level, b.offset(12, 1, 3), Blocks.BARREL.defaultBlockState());
        set(level, b.offset(12, 1, 5), Blocks.BARREL.defaultBlockState());
        set(level, b.offset(2, 1, 9), Blocks.LANTERN.defaultBlockState());
        set(level, b.offset(13, 1, 9), Blocks.LANTERN.defaultBlockState());
    }

    private static void market(ServerLevel level, BlockPos o) {
        stall(level, o.offset(-18, 0, 11), Blocks.BRICKS.defaultBlockState());
        stall(level, o.offset(-5, 0, 11), Blocks.BIRCH_PLANKS.defaultBlockState());
        stall(level, o.offset(8, 0, 11), Blocks.SPRUCE_PLANKS.defaultBlockState());
    }

    private static void stall(ServerLevel level, BlockPos b, BlockState roof) {
        fill(level, b, 0, 0, 0, 8, 6, 6, Blocks.AIR.defaultBlockState());
        fill(level, b, 0, -1, 0, 8, -1, 6, Blocks.COARSE_DIRT.defaultBlockState());
        int[][] posts = {{0, 0}, {8, 0}, {0, 6}, {8, 6}};
        for (int[] p : posts) fill(level, b, p[0], 0, p[1], p[0], 3, p[1], Blocks.OAK_FENCE.defaultBlockState());
        fill(level, b, 0, 4, 0, 8, 4, 6, roof);
        fill(level, b, 1, 1, 1, 7, 1, 1, Blocks.OAK_SLAB.defaultBlockState());
        set(level, b.offset(1, 0, 5), Blocks.BARREL.defaultBlockState());
        set(level, b.offset(7, 0, 5), Blocks.BARREL.defaultBlockState());
    }

    private static void orchard(ServerLevel level, BlockPos o) {
        for (int x = -39; x <= -13; x += 8) for (int z = 45; z <= 61; z += 8) tree(level, o.offset(x, 0, z));
        for (int x = -43; x <= -9; x++) {
            set(level, o.offset(x, 0, 40), Blocks.OAK_FENCE.defaultBlockState());
            set(level, o.offset(x, 0, 66), Blocks.OAK_FENCE.defaultBlockState());
        }
        for (int z = 41; z < 66; z++) {
            set(level, o.offset(-43, 0, z), Blocks.OAK_FENCE.defaultBlockState());
            set(level, o.offset(-9, 0, z), Blocks.OAK_FENCE.defaultBlockState());
        }
        fill(level, o, -27, 0, 40, -25, 1, 40, Blocks.AIR.defaultBlockState());
    }

    private static void tree(ServerLevel level, BlockPos b) {
        fill(level, b, 0, 0, 0, 0, 3, 0, Blocks.OAK_LOG.defaultBlockState());
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            if (Math.abs(x) + Math.abs(z) <= 3) set(level, b.offset(x, 3, z), Blocks.OAK_LEAVES.defaultBlockState());
        }
        fill(level, b, -1, 4, -1, 1, 4, 1, Blocks.OAK_LEAVES.defaultBlockState());
    }

    private static void paddock(ServerLevel level, BlockPos b) {
        int w = 20, d = 17;
        for (int x = 0; x <= w; x++) {
            set(level, b.offset(x, 0, 0), Blocks.OAK_FENCE.defaultBlockState());
            set(level, b.offset(x, 0, d), Blocks.OAK_FENCE.defaultBlockState());
        }
        for (int z = 1; z < d; z++) {
            set(level, b.offset(0, 0, z), Blocks.OAK_FENCE.defaultBlockState());
            set(level, b.offset(w, 0, z), Blocks.OAK_FENCE.defaultBlockState());
        }
        fill(level, b, 9, 0, 0, 11, 1, 0, Blocks.AIR.defaultBlockState());
        fill(level, b, 3, 0, 3, 4, 0, 3, Blocks.HAY_BLOCK.defaultBlockState());
        set(level, b.offset(3, 1, 3), Blocks.HAY_BLOCK.defaultBlockState());
        set(level, b.offset(16, 0, 13), Blocks.WATER.defaultBlockState());
    }

    private static void pond(ServerLevel level, BlockPos c) {
        for (int dx = -7; dx <= 7; dx++) for (int dz = -6; dz <= 6; dz++) {
            double n = dx * dx / 49.0 + dz * dz / 36.0;
            if (n <= 1.0) {
                set(level, c.offset(dx, -1, dz), Blocks.WATER.defaultBlockState());
                set(level, c.offset(dx, -2, dz), Math.floorMod(dx + dz, 4) == 0
                        ? Blocks.CLAY.defaultBlockState() : Blocks.GRAVEL.defaultBlockState());
                set(level, c.offset(dx, 0, dz), Blocks.AIR.defaultBlockState());
            } else if (n <= 1.35) {
                set(level, c.offset(dx, -1, dz), Math.floorMod(dx * 7 + dz, 3) == 0
                        ? Blocks.GRAVEL.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState());
            }
        }
        set(level, c.offset(-2, 0, 0), Blocks.LILY_PAD.defaultBlockState());
        set(level, c.offset(2, 0, 1), Blocks.LILY_PAD.defaultBlockState());
    }

    private static void lamps(ServerLevel level, BlockPos o) {
        int[][] points = {{-25,18},{-12,22},{5,22},{22,18},{42,18},{-3,-17},{4,-17},{-20,36},{13,37},{34,37},{-26,68},{12,66}};
        for (int[] p : points) {
            fill(level, o, p[0], 0, p[1], p[0], 2, p[1], Blocks.SPRUCE_FENCE.defaultBlockState());
            set(level, o.offset(p[0], 3, p[1]), Blocks.LANTERN.defaultBlockState());
        }
    }

    private static void fill(ServerLevel level, BlockPos o, int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
        for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) {
            set(level, o.offset(x, y, z), state);
        }
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, FLAGS);
    }

    private enum Palette {
        SPRUCE(Blocks.SPRUCE_PLANKS.defaultBlockState(), Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), Blocks.DARK_OAK_PLANKS.defaultBlockState()),
        BIRCH(Blocks.BIRCH_PLANKS.defaultBlockState(), Blocks.STRIPPED_BIRCH_LOG.defaultBlockState(), Blocks.BRICKS.defaultBlockState()),
        OAK(Blocks.OAK_PLANKS.defaultBlockState(), Blocks.STRIPPED_OAK_LOG.defaultBlockState(), Blocks.DEEPSLATE_TILES.defaultBlockState());

        private final BlockState wall;
        private final BlockState beam;
        private final BlockState roof;

        Palette(BlockState wall, BlockState beam, BlockState roof) {
            this.wall = wall;
            this.beam = beam;
            this.roof = roof;
        }
    }
}
