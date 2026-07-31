package kr.countrysidedays.world;

import kr.countrysidedays.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;

/** Builds the restaurant, player property, resident homes and connected village centre. */
public final class StarterHomesteadGenerator {
    public static final int HALF_WIDTH = 14;
    public static final int HALF_DEPTH = 12;
    private static final int FLAGS = Block.UPDATE_ALL;

    private StarterHomesteadGenerator() {
    }

    /** Compact deterministic layout retained for GameTest. */
    public static void buildHomestead(ServerLevel level, BlockPos origin) {
        clearPlot(level, origin, -HALF_WIDTH, -HALF_DEPTH, HALF_WIDTH, HALF_DEPTH, 10);
        restaurant(level, origin);
        playerFarm(level, origin);
        well(level, origin);
        coreYard(level, origin);
    }

    /** Full village generated once at the centre of a fresh flat countryside. */
    public static void buildCompleteVillage(ServerLevel level, BlockPos origin) {
        sanitizeVillageSite(level, origin);
        buildHomestead(level, origin);
        roadNetwork(level, origin);

        house(level, origin.offset(-42, 0, -28), 16, 12, Palette.SPRUCE, true);
        house(level, origin.offset(22, 0, -28), 14, 11, Palette.BIRCH, true);
        house(level, origin.offset(-43, 0, 31), 14, 11, Palette.OAK, false);
        house(level, origin.offset(21, 0, 32), 14, 11, Palette.SPRUCE, false);
        communityHall(level, origin.offset(-10, 0, 31));

        market(level, origin);
        playerRanch(level, origin);
        orchard(level, origin);
        pond(level, origin.offset(40, 0, 57));
        lamps(level, origin);

        connectAllEntrances(level, origin);
        refreshOwnershipSigns(level, origin, "새 주민", "나의 시골식당");
        residentSigns(level, origin);
    }

    public static BlockPos kitchenCounterPos(BlockPos origin) {
        return origin.offset(-10, 1, -6);
    }

    public static void refreshOwnershipSigns(
            ServerLevel level,
            BlockPos origin,
            String ownerName,
            String restaurantName
    ) {
        String owner = ownerName == null || ownerName.isBlank() ? "새 주민" : ownerName;
        String restaurant = restaurantName == null || restaurantName.isBlank() ? "나의 시골식당" : restaurantName;
        sign(level, origin.offset(-34, 1, -14), "내 집", owner, "사유지", "");
        sign(level, origin.offset(3, 1, -4), "내 농장", owner, "수확 구역", "");
        sign(level, origin.offset(-7, 1, 3), restaurant, owner, "영업 준비 중", "");
        sign(level, origin.offset(8, 1, 41), "내 목장", owner, "동물 돌보기", "");
    }

    private static void residentSigns(ServerLevel level, BlockPos o) {
        sign(level, o.offset(29, 1, -16), "복순 할머니네", "마을 어른", "어서 오렴", "");
        sign(level, o.offset(-36, 1, 31), "한결이네", "농부", "밭 담당", "");
        sign(level, o.offset(28, 1, 32), "소미네", "목장지기", "목장 담당", "");
        sign(level, o.offset(0, 1, 29), "마을회관", "안내·행사", "공용 시설", "");
        sign(level, o.offset(-1, 1, 9), "느티나무 장터", "아침 장터", "공용 시설", "");
        sign(level, o.offset(-27, 1, 42), "공동 과수원", "함께 돌보는 곳", "", "");
    }

    private static void sanitizeVillageSite(ServerLevel level, BlockPos o) {
        for (int x = -58; x <= 66; x++) {
            for (int z = -42; z <= 78; z++) {
                for (int y = -8; y <= 16; y++) {
                    BlockPos pos = o.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (!state.getFluidState().isEmpty() && !state.getFluidState().isSource()) continue;
                    if (state.is(Blocks.LAVA)) {
                        set(level, pos, y < 0 ? Blocks.DIRT.defaultBlockState() : Blocks.AIR.defaultBlockState());
                    } else if (y >= 0 && (state.is(Blocks.OAK_LOG)
                            || state.is(Blocks.OAK_LEAVES)
                            || state.is(Blocks.FIRE))) {
                        set(level, pos, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
    }

    private static void clearPlot(ServerLevel level, BlockPos o, int minX, int minZ, int maxX, int maxZ, int height) {
        fill(level, o, minX, 0, minZ, maxX, height, maxZ, Blocks.AIR.defaultBlockState());
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                set(level, o.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState());
                fill(level, o, x, -5, z, x, -2, z, Blocks.DIRT.defaultBlockState());
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
        signFrame(level, o.offset(-10, 0, 3), 7);
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

    private static void playerFarm(ServerLevel level, BlockPos o) {
        for (int x = 3; x <= 16; x++) {
            for (int z = -10; z <= 3; z++) {
                boolean edge = x == 3 || x == 16 || z == -10 || z == 3;
                if (edge) {
                    set(level, o.offset(x, 0, z), Blocks.OAK_FENCE.defaultBlockState());
                } else if (x == 8 || x == 13) {
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
        fill(level, o, 3, 0, -4, 3, 1, -3, Blocks.AIR.defaultBlockState());
        signFrame(level, o.offset(2, 0, -5), 4);
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
        fill(level, o, -10, -1, 2, -5, -1, 6, Blocks.GRAVEL.defaultBlockState());
        fill(level, o, 1, -1, 3, 13, -1, 12, Blocks.COARSE_DIRT.defaultBlockState());
        set(level, o.offset(-1, 0, 5), Blocks.HAY_BLOCK.defaultBlockState());
        set(level, o.offset(0, 0, 5), Blocks.HAY_BLOCK.defaultBlockState());
        set(level, o.offset(-1, 1, 5), Blocks.PUMPKIN.defaultBlockState());
        set(level, o.offset(1, 0, 6), Blocks.COMPOSTER.defaultBlockState());
        fill(level, o, 12, 0, 11, 12, 3, 11, Blocks.OAK_LOG.defaultBlockState());
        fill(level, o, 11, 4, 10, 13, 4, 12, Blocks.OAK_LEAVES.defaultBlockState());
    }

    private static void roadNetwork(ServerLevel level, BlockPos o) {
        pathRect(level, o, -58, 16, 66, 21);
        pathRect(level, o, -2, -40, 3, 78);
        pathRect(level, o, -48, -16, 40, -12);
        pathRect(level, o, -48, 27, 43, 31);
        pathRect(level, o, -48, 72, 46, 76);
    }

    private static void connectAllEntrances(ServerLevel level, BlockPos o) {
        pathBetween(level, o, -7, 3, 0, 18);
        pathBetween(level, o, 3, -4, 0, -14);
        pathBetween(level, o, -34, -16, 0, -14);
        pathBetween(level, o, 29, -16, 0, -14);
        pathBetween(level, o, -36, 31, 0, 29);
        pathBetween(level, o, 28, 32, 0, 29);
        pathBetween(level, o, 0, 31, 0, 18);
        pathBetween(level, o, 8, 41, 0, 29);
        pathBetween(level, o, -27, 42, 0, 29);
        pathBetween(level, o, 40, 57, 0, 29);
    }

    private static void pathBetween(ServerLevel level, BlockPos o, int x0, int z0, int x1, int z1) {
        int x = x0;
        while (x != x1) {
            pathDot(level, o, x, z0);
            x += Integer.compare(x1, x);
        }
        int z = z0;
        while (z != z1) {
            pathDot(level, o, x1, z);
            z += Integer.compare(z1, z);
        }
        pathDot(level, o, x1, z1);
    }

    private static void pathRect(ServerLevel level, BlockPos o, int minX, int minZ, int maxX, int maxZ) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                set(level, o.offset(x, -1, z), road(x, z));
                if (level.getBlockState(o.offset(x, 0, z)).is(Blocks.SHORT_GRASS)) {
                    set(level, o.offset(x, 0, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private static void pathDot(ServerLevel level, BlockPos o, int x, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                set(level, o.offset(x + dx, -1, z + dz), road(x + dx, z + dz));
                set(level, o.offset(x + dx, 0, z + dz), Blocks.AIR.defaultBlockState());
            }
        }
    }

    private static BlockState road(int x, int z) {
        return Math.floorMod(x * 3 + z, 8) == 0
                ? Blocks.GRAVEL.defaultBlockState() : Blocks.PACKED_MUD.defaultBlockState();
    }

    private static void house(ServerLevel level, BlockPos b, int width, int depth, Palette palette, boolean frontAtMaxZ) {
        clearPlot(level, b, -1, -1, width, depth, 10);
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

    private static void communityHall(ServerLevel level, BlockPos b) {
        int width = 20, depth = 14;
        clearPlot(level, b, -1, -1, width, depth, 12);
        fill(level, b, 0, -1, 0, width - 1, -1, depth - 1, Blocks.STONE_BRICKS.defaultBlockState());
        fill(level, b, 0, 0, 0, width - 1, 0, depth - 1, Blocks.SPRUCE_PLANKS.defaultBlockState());
        for (int y = 1; y <= 5; y++) {
            for (int x = 0; x < width; x++) {
                BlockState wall = x == 0 || x == width - 1
                        ? Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState()
                        : y == 3 && x % 4 == 2 ? Blocks.GLASS_PANE.defaultBlockState()
                        : Blocks.BIRCH_PLANKS.defaultBlockState();
                set(level, b.offset(x, y, 0), wall);
                set(level, b.offset(x, y, depth - 1), wall);
            }
            for (int z = 1; z < depth - 1; z++) {
                set(level, b.offset(0, y, z), Blocks.BIRCH_PLANKS.defaultBlockState());
                set(level, b.offset(width - 1, y, z), Blocks.BIRCH_PLANKS.defaultBlockState());
            }
        }
        fill(level, b, 8, 1, 0, 11, 4, 0, Blocks.AIR.defaultBlockState());
        fill(level, b, -1, 6, -1, width, 6, depth, Blocks.DEEPSLATE_TILES.defaultBlockState());
        fill(level, b, 1, 7, 1, width - 2, 7, depth - 2, Blocks.DEEPSLATE_TILES.defaultBlockState());
        table(level, b.offset(5, 1, 7));
        table(level, b.offset(10, 1, 7));
        table(level, b.offset(15, 1, 7));
        set(level, b.offset(2, 1, 2), Blocks.BELL.defaultBlockState());
    }

    private static void market(ServerLevel level, BlockPos o) {
        stall(level, o.offset(-18, 0, 10), Blocks.BRICKS.defaultBlockState());
        stall(level, o.offset(-5, 0, 10), Blocks.BIRCH_PLANKS.defaultBlockState());
        stall(level, o.offset(8, 0, 10), Blocks.SPRUCE_PLANKS.defaultBlockState());
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

    private static void playerRanch(ServerLevel level, BlockPos o) {
        barn(level, o.offset(-6, 0, 45));
        paddock(level, o.offset(11, 0, 45));
        signFrame(level, o.offset(7, 0, 41), 5);
    }

    private static void barn(ServerLevel level, BlockPos b) {
        int w = 15, d = 12;
        clearPlot(level, b, -1, -1, w, d, 11);
        fill(level, b, 0, -1, 0, w - 1, -1, d - 1, Blocks.COBBLESTONE.defaultBlockState());
        fill(level, b, 0, 0, 0, w - 1, 0, d - 1, Blocks.COARSE_DIRT.defaultBlockState());
        for (int y = 1; y <= 6; y++) {
            for (int x = 0; x < w; x++) {
                BlockState s = x == 0 || x == w - 1 || x % 5 == 0
                        ? Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState() : Blocks.SPRUCE_PLANKS.defaultBlockState();
                set(level, b.offset(x, y, 0), s);
                set(level, b.offset(x, y, d - 1), s);
            }
            for (int z = 1; z < d - 1; z++) {
                set(level, b.offset(0, y, z), Blocks.SPRUCE_PLANKS.defaultBlockState());
                set(level, b.offset(w - 1, y, z), Blocks.SPRUCE_PLANKS.defaultBlockState());
            }
        }
        fill(level, b, 5, 1, 0, 9, 4, 0, Blocks.AIR.defaultBlockState());
        fill(level, b, -1, 7, -1, w, 7, d, Blocks.DARK_OAK_PLANKS.defaultBlockState());
        fill(level, b, 1, 8, 1, w - 2, 8, d - 2, Blocks.DARK_OAK_PLANKS.defaultBlockState());
        fill(level, b, 2, 1, 7, 4, 2, 9, Blocks.HAY_BLOCK.defaultBlockState());
        set(level, b.offset(11, 1, 8), Blocks.BARREL.defaultBlockState());
    }

    private static void paddock(ServerLevel level, BlockPos b) {
        int w = 14, d = 19;
        for (int x = 0; x <= w; x++) {
            set(level, b.offset(x, 0, 0), Blocks.OAK_FENCE.defaultBlockState());
            set(level, b.offset(x, 0, d), Blocks.OAK_FENCE.defaultBlockState());
        }
        for (int z = 1; z < d; z++) {
            set(level, b.offset(0, 0, z), Blocks.OAK_FENCE.defaultBlockState());
            set(level, b.offset(w, 0, z), Blocks.OAK_FENCE.defaultBlockState());
        }
        fill(level, b, 6, 0, 0, 8, 1, 0, Blocks.AIR.defaultBlockState());
        fill(level, b, 2, 0, 3, 3, 1, 4, Blocks.HAY_BLOCK.defaultBlockState());
        fill(level, b, 10, 0, 14, 12, 0, 16, Blocks.WATER.defaultBlockState());
    }

    private static void orchard(ServerLevel level, BlockPos o) {
        for (int x = -42; x <= -18; x += 8) {
            for (int z = 46; z <= 62; z += 8) tree(level, o.offset(x, 0, z));
        }
        for (int x = -46; x <= -12; x++) {
            set(level, o.offset(x, 0, 42), Blocks.OAK_FENCE.defaultBlockState());
            set(level, o.offset(x, 0, 68), Blocks.OAK_FENCE.defaultBlockState());
        }
        for (int z = 43; z < 68; z++) {
            set(level, o.offset(-46, 0, z), Blocks.OAK_FENCE.defaultBlockState());
            set(level, o.offset(-12, 0, z), Blocks.OAK_FENCE.defaultBlockState());
        }
        fill(level, o, -30, 0, 42, -28, 1, 42, Blocks.AIR.defaultBlockState());
        signFrame(level, o.offset(-30, 0, 42), 5);
    }

    private static void tree(ServerLevel level, BlockPos b) {
        fill(level, b, 0, 0, 0, 0, 4, 0, Blocks.OAK_LOG.defaultBlockState());
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (Math.abs(x) + Math.abs(z) <= 3) {
                    set(level, b.offset(x, 4, z), Blocks.OAK_LEAVES.defaultBlockState());
                }
            }
        }
        fill(level, b, -1, 5, -1, 1, 5, 1, Blocks.OAK_LEAVES.defaultBlockState());
    }

    private static void pond(ServerLevel level, BlockPos c) {
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
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
        }
        set(level, c.offset(-2, 0, 0), Blocks.LILY_PAD.defaultBlockState());
        set(level, c.offset(2, 0, 1), Blocks.LILY_PAD.defaultBlockState());
    }

    private static void signFrame(ServerLevel level, BlockPos b, int width) {
        fill(level, b, 0, 0, 0, 0, 3, 0, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());
        fill(level, b, width, 0, 0, width, 3, 0, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());
        fill(level, b, 1, 2, 0, width - 1, 3, 0, Blocks.DARK_OAK_PLANKS.defaultBlockState());
        set(level, b.offset(0, 4, 0), Blocks.LANTERN.defaultBlockState());
        set(level, b.offset(width, 4, 0), Blocks.LANTERN.defaultBlockState());
    }

    private static void sign(ServerLevel level, BlockPos pos, String line1, String line2, String line3, String line4) {
        set(level, pos, Blocks.OAK_SIGN.defaultBlockState());
        if (!(level.getBlockEntity(pos) instanceof SignBlockEntity sign)) return;
        SignText text = sign.getFrontText()
                .setMessage(0, Component.literal(line1))
                .setMessage(1, Component.literal(line2))
                .setMessage(2, Component.literal(line3))
                .setMessage(3, Component.literal(line4));
        sign.setText(text, true);
        sign.setChanged();
    }

    private static void lamps(ServerLevel level, BlockPos o) {
        int[][] points = {
                {-48,18},{-28,18},{-10,18},{10,18},{30,18},{50,18},
                {0,-32},{0,-14},{0,36},{0,56},{0,74},
                {-34,-13},{29,-13},{-36,30},{28,30},{-28,44},{8,42},{40,52}
        };
        for (int[] p : points) {
            fill(level, o, p[0], 0, p[1], p[0], 2, p[1], Blocks.SPRUCE_FENCE.defaultBlockState());
            set(level, o.offset(p[0], 3, p[1]), Blocks.LANTERN.defaultBlockState());
        }
    }

    private static void fill(ServerLevel level, BlockPos o, int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) set(level, o.offset(x, y, z), state);
            }
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
