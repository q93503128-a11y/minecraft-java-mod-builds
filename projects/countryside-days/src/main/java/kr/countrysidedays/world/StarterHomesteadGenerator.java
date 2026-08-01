package kr.countrysidedays.world;

import kr.countrysidedays.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;

/** Deterministic public village and isolated player estate builder. */
public final class StarterHomesteadGenerator {
    public static final int PUBLIC_HALF_WIDTH = 52;
    public static final int PUBLIC_HALF_DEPTH = 46;
    private static final int FLAGS = Block.UPDATE_ALL;

    private StarterHomesteadGenerator() {
    }

    public static void buildHomestead(ServerLevel level, BlockPos origin) {
        buildPlayerEstate(level, origin, "테스트 주민", "테스트 시골식당");
    }

    public static void buildCompleteVillage(ServerLevel level, BlockPos origin) {
        buildPublicVillage(level, origin);
    }

    public static void buildPublicVillage(ServerLevel level, BlockPos origin) {
        prepareArea(level, origin, -PUBLIC_HALF_WIDTH, -PUBLIC_HALF_DEPTH,
                PUBLIC_HALF_WIDTH, PUBLIC_HALF_DEPTH, 12);
        publicRoads(level, origin);
        communityHall(level, origin.offset(-10, 0, -40));
        home(level, origin.offset(-44, 0, -22), 14, 11, Palette.BIRCH, true, "복순 할머니");
        home(level, origin.offset(30, 0, -22), 14, 11, Palette.OAK, true, "농부 한결");
        home(level, origin.offset(-44, 0, 16), 14, 11, Palette.OAK, false, "목장지기 소미");
        home(level, origin.offset(30, 0, 16), 14, 11, Palette.BIRCH, false, "회관지기 도윤");
        market(level, origin);
        publicLamps(level, origin);
        publicSigns(level, origin);
    }

    public static void buildPlayerEstate(ServerLevel level, BlockPos origin, String ownerName, String restaurantName) {
        prepareArea(level, origin, PlayerEstateLayout.MIN_X, PlayerEstateLayout.MIN_Z,
                PlayerEstateLayout.MAX_X, PlayerEstateLayout.MAX_Z, 12);
        estateBoundary(level, origin);
        estatePaths(level, origin);
        home(level, origin.offset(-27, 0, -19), 13, 12, Palette.BIRCH, true, ownerName);
        restaurant(level, origin);
        playerFarm(level, origin);
        playerRanch(level, origin);
        estateLamps(level, origin);
        refreshEstateSigns(level, origin, ownerName, restaurantName);
    }

    public static BlockPos kitchenCounterPos(BlockPos estateOrigin) {
        return PlayerEstateLayout.kitchenCounter(estateOrigin);
    }

    public static void connectEstateToVillage(ServerLevel level, BlockPos village, BlockPos estate) {
        int dx = estate.getX() - village.getX();
        int dz = estate.getZ() - village.getZ();
        BlockPos start;
        BlockPos end;
        if (Math.abs(dx) >= Math.abs(dz)) {
            start = village.offset(dx >= 0 ? PUBLIC_HALF_WIDTH : -PUBLIC_HALF_WIDTH, 0, 0);
            end = estate.offset(dx >= 0 ? PlayerEstateLayout.MIN_X : PlayerEstateLayout.MAX_X, 0, 0);
        } else {
            start = village.offset(0, 0, dz >= 0 ? PUBLIC_HALF_DEPTH : -PUBLIC_HALF_DEPTH);
            end = estate.offset(0, 0, dz >= 0 ? PlayerEstateLayout.MIN_Z : PlayerEstateLayout.MAX_Z);
        }
        safeWorldPath(level, start, new BlockPos(end.getX(), start.getY(), start.getZ()));
        safeWorldPath(level, new BlockPos(end.getX(), start.getY(), start.getZ()), end);
    }

    public static void refreshEstateSigns(ServerLevel level, BlockPos origin, String ownerName, String restaurantName) {
        String owner = ownerName == null || ownerName.isBlank() ? "새 주민" : ownerName;
        String restaurantTitle = restaurantName == null || restaurantName.isBlank()
                ? owner + "의 시골식당" : restaurantName;
        sign(level, origin.offset(-24, 0, -7), "내 집", owner, "사유지", "");
        sign(level, origin.offset(11, 0, -7), restaurantTitle, owner, "영업 1000~11500", "");
        sign(level, origin.offset(-6, 0, 10), "내 농장", owner, "주인만 수확", "");
        sign(level, origin.offset(5, 0, 14), "내 목장", owner, "주인만 사용", "");
        sign(level, origin.offset(3, 0, PlayerEstateLayout.MIN_Z + 2), "생활 구획", owner, "집·식당·농장·목장", "");
    }

    public static void refreshOwnershipSigns(ServerLevel level, BlockPos origin, String ownerName, String restaurantName) {
        refreshEstateSigns(level, origin, ownerName, restaurantName);
    }

    private static void publicRoads(ServerLevel level, BlockPos o) {
        pathRect(level, o, -48, -2, 48, 2);
        pathRect(level, o, -2, -44, 2, 44);
        pathBetween(level, o, -37, -11, -37, -2);
        pathBetween(level, o, 37, -11, 37, -2);
        pathBetween(level, o, -37, 16, -37, 2);
        pathBetween(level, o, 37, 16, 37, 2);
        pathBetween(level, o, 0, -27, 0, -2);
        pathBetween(level, o, -18, 8, -18, 2);
        pathBetween(level, o, 18, 8, 18, 2);
    }

    private static void publicSigns(ServerLevel level, BlockPos o) {
        sign(level, o.offset(3, 0, -25), "마을회관", "안내·행사", "공공시설", "파손 금지");
        sign(level, o.offset(-34, 0, -10), "복순 할머니네", "마을 어른", "개인 주택", "");
        sign(level, o.offset(34, 0, -10), "한결이네", "농부", "개인 주택", "");
        sign(level, o.offset(-34, 0, 14), "소미네", "목장지기", "개인 주택", "");
        sign(level, o.offset(34, 0, 14), "도윤이네", "회관지기", "개인 주택", "");
        sign(level, o.offset(4, 0, 5), "느티나무 장터", "공공시설", "아침 장터", "");
        sign(level, o.offset(4, 0, 42), "개인 생활 구획", "길을 따라 이동", "각자 사유지", "");
    }

    private static void estateBoundary(ServerLevel level, BlockPos o) {
        for (int x = PlayerEstateLayout.MIN_X; x <= PlayerEstateLayout.MAX_X; x++) {
            if (x < -1 || x > 1) {
                set(level, o.offset(x, 0, PlayerEstateLayout.MIN_Z), Blocks.OAK_FENCE.defaultBlockState());
                set(level, o.offset(x, 0, PlayerEstateLayout.MAX_Z), Blocks.OAK_FENCE.defaultBlockState());
            }
        }
        for (int z = PlayerEstateLayout.MIN_Z + 1; z < PlayerEstateLayout.MAX_Z; z++) {
            set(level, o.offset(PlayerEstateLayout.MIN_X, 0, z), Blocks.OAK_FENCE.defaultBlockState());
            set(level, o.offset(PlayerEstateLayout.MAX_X, 0, z), Blocks.OAK_FENCE.defaultBlockState());
        }
    }

    private static void estatePaths(ServerLevel level, BlockPos o) {
        pathRect(level, o, -2, PlayerEstateLayout.MIN_Z, 2, PlayerEstateLayout.MAX_Z);
        pathRect(level, o, -23, -10, 18, -6);
        pathRect(level, o, -8, 8, 8, 12);
        pathRect(level, o, -2, 12, 8, 16);
    }

    private static void restaurant(ServerLevel level, BlockPos o) {
        BlockPos b = o.offset(7, 0, -20);
        shell(level, b, 21, 13, 5, Palette.OAK, true);
        set(level, PlayerEstateLayout.kitchenCounter(o), ModBlocks.COUNTRY_KITCHEN_COUNTER.get().defaultBlockState());
        set(level, b.offset(2, 1, 2), Blocks.FURNACE.defaultBlockState());
        set(level, b.offset(3, 1, 2), Blocks.BARREL.defaultBlockState());
        set(level, b.offset(4, 1, 2), Blocks.CRAFTING_TABLE.defaultBlockState());
        set(level, b.offset(6, 1, 2), Blocks.CHEST.defaultBlockState());
        tableAndSeats(level, b.offset(7, 1, 7));
        tableAndSeats(level, b.offset(14, 1, 7));
        set(level, b.offset(3, 1, 9), Blocks.BOOKSHELF.defaultBlockState());
        set(level, b.offset(3, 2, 9), Blocks.FLOWER_POT.defaultBlockState());
        set(level, b.offset(2, 1, 10), Blocks.LANTERN.defaultBlockState());
        set(level, b.offset(18, 1, 10), Blocks.LANTERN.defaultBlockState());
        set(level, b.offset(10, 1, 3), Blocks.LANTERN.defaultBlockState());
    }

    private static void playerFarm(ServerLevel level, BlockPos o) {
        int minX = -27, maxX = -7, minZ = 2, maxZ = 20;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                boolean gate = x == maxX && z >= 9 && z <= 11;
                boolean edge = x == minX || x == maxX || z == minZ || z == maxZ;
                if (edge) {
                    if (!gate) set(level, o.offset(x, 0, z), Blocks.OAK_FENCE.defaultBlockState());
                    continue;
                }
                if (x == -18 && z >= 5 && z <= 17) {
                    set(level, o.offset(x, 0, z), Blocks.WATER.defaultBlockState());
                    continue;
                }
                set(level, o.offset(x, 0, z), Blocks.FARMLAND.defaultBlockState());
                BlockState crop = switch (Math.floorMod(x + z, 3)) {
                    case 0 -> Blocks.WHEAT.defaultBlockState();
                    case 1 -> Blocks.CARROTS.defaultBlockState();
                    default -> Blocks.POTATOES.defaultBlockState();
                };
                set(level, o.offset(x, 1, z), crop);
            }
        }
        set(level, o.offset(-25, 0, 18), Blocks.COMPOSTER.defaultBlockState());
        set(level, o.offset(-9, 0, 18), Blocks.BARREL.defaultBlockState());
    }

    private static void playerRanch(ServerLevel level, BlockPos o) {
        int minX = 6, maxX = 28, minZ = 2, maxZ = 27;
        for (int x = minX; x <= maxX; x++) {
            boolean gate = x >= 6 && x <= 8;
            if (!gate) set(level, o.offset(x, 0, minZ), Blocks.OAK_FENCE.defaultBlockState());
            set(level, o.offset(x, 0, maxZ), Blocks.OAK_FENCE.defaultBlockState());
        }
        for (int z = minZ + 1; z < maxZ; z++) {
            set(level, o.offset(minX, 0, z), Blocks.OAK_FENCE.defaultBlockState());
            set(level, o.offset(maxX, 0, z), Blocks.OAK_FENCE.defaultBlockState());
        }
        barn(level, o.offset(10, 0, 5));
        set(level, o.offset(9, 0, 22), Blocks.CAULDRON.defaultBlockState());
        fill(level, o, 22, 0, 21, 25, 1, 24, Blocks.HAY_BLOCK.defaultBlockState());
        set(level, o.offset(24, 0, 7), Blocks.BARREL.defaultBlockState());
    }

    private static void barn(ServerLevel level, BlockPos b) {
        int width = 16, depth = 10;
        fill(level, b, 0, -1, 0, width - 1, -1, depth - 1, Blocks.COBBLESTONE.defaultBlockState());
        fill(level, b, 0, 0, 0, width - 1, 0, depth - 1, Blocks.COARSE_DIRT.defaultBlockState());
        for (int y = 1; y <= 5; y++) {
            for (int x = 0; x < width; x++) {
                BlockState wall = x == 0 || x == width - 1 || x % 5 == 0
                        ? Blocks.STRIPPED_OAK_LOG.defaultBlockState()
                        : Blocks.OAK_PLANKS.defaultBlockState();
                set(level, b.offset(x, y, 0), wall);
                set(level, b.offset(x, y, depth - 1), wall);
            }
            for (int z = 1; z < depth - 1; z++) {
                set(level, b.offset(0, y, z), Blocks.OAK_PLANKS.defaultBlockState());
                set(level, b.offset(width - 1, y, z), Blocks.OAK_PLANKS.defaultBlockState());
            }
        }
        fill(level, b, 6, 1, 0, 9, 4, 0, Blocks.AIR.defaultBlockState());
        fill(level, b, -1, 6, -1, width, 6, depth, Blocks.BRICKS.defaultBlockState());
        fill(level, b, 1, 7, 1, width - 2, 7, depth - 2, Blocks.BRICK_SLAB.defaultBlockState());
        set(level, b.offset(2, 1, 2), Blocks.LANTERN.defaultBlockState());
        set(level, b.offset(13, 1, 7), Blocks.LANTERN.defaultBlockState());
    }

    private static void home(ServerLevel level, BlockPos base, int width, int depth,
                             Palette palette, boolean frontAtMaxZ, String residentName) {
        shell(level, base, width, depth, 4, palette, frontAtMaxZ);
        furnishHome(level, base, width, depth, residentName);
    }

    private static void shell(ServerLevel level, BlockPos b, int width, int depth,
                              int wallHeight, Palette palette, boolean frontAtMaxZ) {
        fill(level, b, 0, -1, 0, width - 1, -1, depth - 1, Blocks.COBBLESTONE.defaultBlockState());
        fill(level, b, 0, 0, 0, width - 1, 0, depth - 1, Blocks.BIRCH_PLANKS.defaultBlockState());
        for (int y = 1; y <= wallHeight; y++) {
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
        fill(level, b, doorX - 1, 1, doorZ, doorX + 1, 3, doorZ, Blocks.AIR.defaultBlockState());
        fill(level, b, -1, wallHeight + 1, -1, width, wallHeight + 1, depth, palette.roof);
        fill(level, b, 1, wallHeight + 2, 1, width - 2, wallHeight + 2, depth - 2, palette.roofSlab);
    }

    private static void houseWall(ServerLevel level, BlockPos b, int x, int y, int z,
                                  int width, int depth, Palette palette) {
        boolean corner = (x == 0 || x == width - 1) && (z == 0 || z == depth - 1);
        boolean window = (y == 2 || y == 3)
                && (((z == 0 || z == depth - 1) && (x == 2 || x == width - 3))
                || ((x == 0 || x == width - 1) && (z == 2 || z == depth - 3)));
        set(level, b.offset(x, y, z), corner ? palette.beam
                : window ? Blocks.GLASS_PANE.defaultBlockState() : palette.wall);
    }

    private static void furnishHome(ServerLevel level, BlockPos b, int width, int depth, String residentName) {
        set(level, b.offset(2, 1, 2), Blocks.CHEST.defaultBlockState());
        set(level, b.offset(3, 1, 2), Blocks.CRAFTING_TABLE.defaultBlockState());
        set(level, b.offset(4, 1, 2), Blocks.FURNACE.defaultBlockState());
        set(level, b.offset(width - 3, 1, 2), Blocks.BOOKSHELF.defaultBlockState());
        set(level, b.offset(width - 3, 2, 2), Blocks.FLOWER_POT.defaultBlockState());
        fill(level, b, 2, 1, depth - 4, 4, 1, depth - 3,
                Blocks.WOOL.pick(DyeColor.WHITE).defaultBlockState());
        fill(level, b, 2, 2, depth - 4, 4, 2, depth - 3,
                Blocks.CARPET.pick(DyeColor.YELLOW).defaultBlockState());
        tableAndSeats(level, b.offset(width - 4, 1, depth - 4));
        set(level, b.offset(2, 1, depth / 2), Blocks.LANTERN.defaultBlockState());
        set(level, b.offset(width - 3, 1, depth / 2), Blocks.LANTERN.defaultBlockState());
        if (residentName != null && !residentName.isBlank()) {
            sign(level, b.offset(width / 2 + 2, 0, depth - 1), residentName + "의 집", "생활 공간", "", "");
        }
    }

    private static void communityHall(ServerLevel level, BlockPos b) {
        shell(level, b, 20, 14, 5, Palette.HALL, true);
        tableAndSeats(level, b.offset(5, 1, 7));
        tableAndSeats(level, b.offset(14, 1, 7));
        set(level, b.offset(2, 1, 2), Blocks.BELL.defaultBlockState());
        set(level, b.offset(4, 1, 2), Blocks.BOOKSHELF.defaultBlockState());
        set(level, b.offset(5, 1, 2), Blocks.BOOKSHELF.defaultBlockState());
        set(level, b.offset(14, 1, 2), Blocks.CHEST.defaultBlockState());
        set(level, b.offset(17, 1, 2), Blocks.LANTERN.defaultBlockState());
        set(level, b.offset(2, 1, 11), Blocks.LANTERN.defaultBlockState());
        set(level, b.offset(17, 1, 11), Blocks.LANTERN.defaultBlockState());
    }

    private static void market(ServerLevel level, BlockPos o) {
        stall(level, o.offset(-25, 0, 7), Blocks.BRICKS.defaultBlockState());
        stall(level, o.offset(10, 0, 7), Blocks.BIRCH_PLANKS.defaultBlockState());
    }

    private static void stall(ServerLevel level, BlockPos b, BlockState roof) {
        int width = 10, depth = 7;
        fill(level, b, 0, -1, 0, width, -1, depth, Blocks.COARSE_DIRT.defaultBlockState());
        int[][] posts = {{0, 0}, {width, 0}, {0, depth}, {width, depth}};
        for (int[] post : posts) {
            fill(level, b, post[0], 0, post[1], post[0], 3, post[1], Blocks.STRIPPED_OAK_LOG.defaultBlockState());
        }
        fill(level, b, 0, 4, 0, width, 4, depth, roof);
        fill(level, b, 1, 1, 1, width - 1, 1, 1, Blocks.OAK_SLAB.defaultBlockState());
        set(level, b.offset(1, 0, depth - 1), Blocks.BARREL.defaultBlockState());
        set(level, b.offset(width - 1, 0, depth - 1), Blocks.BARREL.defaultBlockState());
        set(level, b.offset(width / 2, 1, depth - 1), Blocks.LANTERN.defaultBlockState());
    }

    private static void tableAndSeats(ServerLevel level, BlockPos table) {
        set(level, table, Blocks.OAK_FENCE.defaultBlockState());
        set(level, table.above(), Blocks.OAK_PRESSURE_PLATE.defaultBlockState());
        set(level, table.north(), Blocks.OAK_STAIRS.defaultBlockState());
        set(level, table.south(), Blocks.OAK_STAIRS.defaultBlockState());
    }

    private static void publicLamps(ServerLevel level, BlockPos o) {
        int[][] points = {
                {-45, -4}, {-25, -4}, {25, -4}, {45, -4},
                {-4, -38}, {-4, -22}, {-4, 18}, {-4, 38},
                {-30, 4}, {30, 4}, {-20, 16}, {20, 16}
        };
        for (int[] point : points) lamp(level, o.offset(point[0], 0, point[1]));
    }

    private static void estateLamps(ServerLevel level, BlockPos o) {
        int[][] points = {
                {-4, -22}, {4, -22}, {-4, -10}, {4, -10},
                {-4, 2}, {4, 2}, {-4, 16}, {4, 16}, {-4, 27}, {4, 27}
        };
        for (int[] point : points) lamp(level, o.offset(point[0], 0, point[1]));
    }

    private static void lamp(ServerLevel level, BlockPos pos) {
        fill(level, pos, 0, 0, 0, 0, 2, 0, Blocks.SPRUCE_FENCE.defaultBlockState());
        set(level, pos.above(3), Blocks.LANTERN.defaultBlockState());
    }

    private static void prepareArea(ServerLevel level, BlockPos origin, int minX, int minZ,
                                    int maxX, int maxZ, int clearHeight) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                fill(level, origin, x, 0, z, x, clearHeight, z, Blocks.AIR.defaultBlockState());
                set(level, origin.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState());
                fill(level, origin, x, -5, z, x, -2, z, Blocks.DIRT.defaultBlockState());
            }
        }
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
            }
        }
    }

    private static void pathDot(ServerLevel level, BlockPos o, int x, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                set(level, o.offset(x + dx, -1, z + dz), road(x + dx, z + dz));
            }
        }
    }

    private static void safeWorldPath(ServerLevel level, BlockPos start, BlockPos end) {
        int x = start.getX();
        int z = start.getZ();
        while (x != end.getX() || z != end.getZ()) {
            safeWorldPathDot(level, new BlockPos(x, start.getY(), z));
            if (x != end.getX()) x += Integer.compare(end.getX(), x);
            else z += Integer.compare(end.getZ(), z);
        }
        safeWorldPathDot(level, end);
    }

    private static void safeWorldPathDot(ServerLevel level, BlockPos centre) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos ground = centre.offset(dx, -1, dz);
                BlockPos above = centre.offset(dx, 0, dz);
                if (level.getBlockState(above).isAir()
                        || level.getBlockState(above).is(Blocks.SHORT_GRASS)
                        || level.getBlockState(above).is(Blocks.DANDELION)
                        || level.getBlockState(above).is(Blocks.POPPY)
                        || level.getBlockState(above).is(Blocks.CORNFLOWER)) {
                    set(level, ground, road(ground.getX(), ground.getZ()));
                    set(level, above, Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private static BlockState road(int x, int z) {
        return Math.floorMod(x * 3 + z, 8) == 0
                ? Blocks.GRAVEL.defaultBlockState() : Blocks.PACKED_MUD.defaultBlockState();
    }

    private static void sign(ServerLevel level, BlockPos pos, String line1, String line2,
                             String line3, String line4) {
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

    private static void fill(ServerLevel level, BlockPos origin, int x0, int y0, int z0,
                             int x1, int y1, int z1, BlockState state) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    set(level, origin.offset(x, y, z), state);
                }
            }
        }
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, FLAGS);
    }

    private enum Palette {
        BIRCH(
                Blocks.BIRCH_PLANKS.defaultBlockState(),
                Blocks.STRIPPED_OAK_LOG.defaultBlockState(),
                Blocks.BRICKS.defaultBlockState(),
                Blocks.BRICK_SLAB.defaultBlockState()
        ),
        OAK(
                Blocks.OAK_PLANKS.defaultBlockState(),
                Blocks.STRIPPED_BIRCH_LOG.defaultBlockState(),
                Blocks.SPRUCE_PLANKS.defaultBlockState(),
                Blocks.SPRUCE_SLAB.defaultBlockState()
        ),
        HALL(
                Blocks.BIRCH_PLANKS.defaultBlockState(),
                Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(),
                Blocks.DEEPSLATE_TILES.defaultBlockState(),
                Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState()
        );

        private final BlockState wall;
        private final BlockState beam;
        private final BlockState roof;
        private final BlockState roofSlab;

        Palette(BlockState wall, BlockState beam, BlockState roof, BlockState roofSlab) {
            this.wall = wall;
            this.beam = beam;
            this.roof = roof;
            this.roofSlab = roofSlab;
        }
    }
}
