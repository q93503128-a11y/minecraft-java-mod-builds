package kr.countrysidedays.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** Adds enough housing and primary-industry workplaces for the expanded resident roster. */
public final class PublicVillageExpansionBuilder {
    private static final int FLAGS = Block.UPDATE_ALL;

    private PublicVillageExpansionBuilder() {
    }

    public static void ensureExpanded(ServerLevel level, BlockPos origin) {
        if (level.getBlockState(origin.offset(40, 0, 30)).is(Blocks.OAK_FENCE_GATE)
                && level.getBlockState(origin.offset(-22, 1, -20)).is(Blocks.BARREL)) {
            return;
        }

        cottage(level, origin.offset(-50, 0, -42), true);
        cottage(level, origin.offset(38, 0, -42), true);
        cottage(level, origin.offset(-50, 0, 4), true);
        cottage(level, origin.offset(38, 0, 4), true);
        cottage(level, origin.offset(-28, 0, 31), false);
        cottage(level, origin.offset(15, 0, 31), false);

        workshop(level, origin.offset(-27, 0, -24), Blocks.BRICKS.defaultBlockState(), Workshop.BAKERY);
        workshop(level, origin.offset(16, 0, -24), Blocks.SPRUCE_PLANKS.defaultBlockState(), Workshop.CARPENTER);
        workshop(level, origin.offset(-27, 0, -43), Blocks.DEEPSLATE_TILES.defaultBlockState(), Workshop.SCHOOL);
        workshop(level, origin.offset(16, 0, -43), Blocks.BIRCH_PLANKS.defaultBlockState(), Workshop.CLINIC);
        openWorkStall(level, origin.offset(-36, 0, 4), Workshop.TAILOR);
        openWorkStall(level, origin.offset(24, 0, 4), Workshop.FORGE);

        publicFarm(level, origin);
        orchard(level, origin);
        publicRanch(level, origin);
        fishingPond(level, origin);
        connectPaths(level, origin);
    }

    private static void cottage(ServerLevel level, BlockPos base, boolean frontSouth) {
        clear(level, base, -1, -1, 12, 10, 8);
        int width = 11;
        int depth = 9;
        Direction front = frontSouth ? Direction.SOUTH : Direction.NORTH;
        int doorZ = frontSouth ? depth - 1 : 0;
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                set(level, base.offset(x, -1, z), Blocks.COBBLESTONE.defaultBlockState());
                set(level, base.offset(x, 0, z), Blocks.BIRCH_PLANKS.defaultBlockState());
            }
        }
        for (int y = 1; y <= 4; y++) {
            for (int x = 0; x < width; x++) {
                houseWall(level, base, x, y, 0, width, depth);
                houseWall(level, base, x, y, depth - 1, width, depth);
            }
            for (int z = 1; z < depth - 1; z++) {
                houseWall(level, base, 0, y, z, width, depth);
                houseWall(level, base, width - 1, y, z, width, depth);
            }
        }
        for (int x = -1; x <= width; x++) {
            for (int z = -1; z <= depth; z++) {
                set(level, base.offset(x, 5, z), Blocks.SPRUCE_PLANKS.defaultBlockState());
            }
        }
        set(level, base.offset(width / 2, 1, doorZ), Blocks.AIR.defaultBlockState());
        set(level, base.offset(width / 2, 2, doorZ), Blocks.AIR.defaultBlockState());
        placeDoor(level, base.offset(width / 2, 1, doorZ), front);
        set(level, base.offset(2, 1, 2), Blocks.CHEST.defaultBlockState());
        set(level, base.offset(3, 1, 2), Blocks.CRAFTING_TABLE.defaultBlockState());
        set(level, base.offset(8, 1, 6), Blocks.YELLOW_BED.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
        set(level, base.offset(5, 1, 5), Blocks.OAK_FENCE.defaultBlockState());
        set(level, base.offset(5, 2, 5), Blocks.OAK_PRESSURE_PLATE.defaultBlockState());
        set(level, base.offset(4, 1, 5), stair(Direction.EAST));
        set(level, base.offset(6, 1, 5), stair(Direction.WEST));
    }

    private static void houseWall(
            ServerLevel level,
            BlockPos base,
            int x,
            int y,
            int z,
            int width,
            int depth
    ) {
        boolean corner = (x == 0 || x == width - 1) && (z == 0 || z == depth - 1);
        boolean window = (y == 2 || y == 3)
                && ((z == 0 || z == depth - 1) && (x == 2 || x == width - 3)
                || (x == 0 || x == width - 1) && (z == 2 || z == depth - 3));
        set(level, base.offset(x, y, z), corner
                ? Blocks.STRIPPED_OAK_LOG.defaultBlockState()
                : window
                ? Blocks.GLASS.defaultBlockState()
                : Blocks.OAK_PLANKS.defaultBlockState());
    }

    private static void workshop(
            ServerLevel level,
            BlockPos base,
            BlockState roof,
            Workshop type
    ) {
        clear(level, base, -1, -1, 12, 10, 8);
        int width = 11;
        int depth = 9;
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                set(level, base.offset(x, -1, z), Blocks.COBBLESTONE.defaultBlockState());
                set(level, base.offset(x, 0, z), Blocks.OAK_PLANKS.defaultBlockState());
            }
        }
        for (int y = 1; y <= 4; y++) {
            for (int x = 0; x < width; x++) {
                set(level, base.offset(x, y, 0), x == 0 || x == width - 1
                        ? Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState()
                        : Blocks.BIRCH_PLANKS.defaultBlockState());
                set(level, base.offset(x, y, depth - 1), x == 0 || x == width - 1
                        ? Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState()
                        : Blocks.BIRCH_PLANKS.defaultBlockState());
            }
            for (int z = 1; z < depth - 1; z++) {
                set(level, base.offset(0, y, z), Blocks.BIRCH_PLANKS.defaultBlockState());
                set(level, base.offset(width - 1, y, z), Blocks.BIRCH_PLANKS.defaultBlockState());
            }
        }
        set(level, base.offset(5, 1, depth - 1), Blocks.AIR.defaultBlockState());
        set(level, base.offset(5, 2, depth - 1), Blocks.AIR.defaultBlockState());
        placeDoor(level, base.offset(5, 1, depth - 1), Direction.SOUTH);
        for (int x = -1; x <= width; x++) {
            for (int z = -1; z <= depth; z++) {
                set(level, base.offset(x, 5, z), roof);
            }
        }
        furnishWorkshop(level, base, type);
    }

    private static void openWorkStall(ServerLevel level, BlockPos base, Workshop type) {
        clear(level, base, -1, -1, 10, 9, 6);
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 7; z++) {
                set(level, base.offset(x, -1, z), Blocks.PACKED_MUD.defaultBlockState());
            }
        }
        int[][] posts = {{0, 0}, {8, 0}, {0, 7}, {8, 7}};
        for (int[] post : posts) {
            for (int y = 0; y <= 3; y++) {
                set(level, base.offset(post[0], y, post[1]), Blocks.STRIPPED_OAK_LOG.defaultBlockState());
            }
        }
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 7; z++) {
                set(level, base.offset(x, 4, z), type == Workshop.FORGE
                        ? Blocks.BRICKS.defaultBlockState()
                        : Blocks.BIRCH_PLANKS.defaultBlockState());
            }
        }
        furnishWorkshop(level, base, type);
    }

    private static void furnishWorkshop(ServerLevel level, BlockPos base, Workshop type) {
        switch (type) {
            case BAKERY -> {
                set(level, base.offset(2, 1, 2), Blocks.FURNACE.defaultBlockState());
                set(level, base.offset(3, 1, 2), Blocks.SMOKER.defaultBlockState());
                set(level, base.offset(5, 1, 2), Blocks.BARREL.defaultBlockState());
            }
            case CARPENTER -> {
                set(level, base.offset(2, 1, 2), Blocks.CRAFTING_TABLE.defaultBlockState());
                set(level, base.offset(3, 1, 2), Blocks.CARTOGRAPHY_TABLE.defaultBlockState());
                set(level, base.offset(5, 1, 2), Blocks.BARREL.defaultBlockState());
            }
            case SCHOOL -> {
                set(level, base.offset(2, 1, 2), Blocks.LECTERN.defaultBlockState());
                set(level, base.offset(7, 1, 2), Blocks.BOOKSHELF.defaultBlockState());
                set(level, base.offset(8, 1, 2), Blocks.BOOKSHELF.defaultBlockState());
            }
            case CLINIC -> {
                set(level, base.offset(2, 1, 2), Blocks.BREWING_STAND.defaultBlockState());
                set(level, base.offset(4, 1, 2), Blocks.BARREL.defaultBlockState());
                set(level, base.offset(7, 1, 5), Blocks.WHITE_BED.defaultBlockState()
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
            }
            case TAILOR -> {
                set(level, base.offset(2, 0, 4), Blocks.LOOM.defaultBlockState());
                set(level, base.offset(4, 0, 4), Blocks.BARREL.defaultBlockState());
                set(level, base.offset(6, 0, 4), Blocks.WHITE_WOOL.defaultBlockState());
            }
            case FORGE -> {
                set(level, base.offset(2, 0, 4), Blocks.BLAST_FURNACE.defaultBlockState());
                set(level, base.offset(4, 0, 4), Blocks.ANVIL.defaultBlockState());
                set(level, base.offset(6, 0, 4), Blocks.SMITHING_TABLE.defaultBlockState());
            }
        }
    }

    private static void publicFarm(ServerLevel level, BlockPos origin) {
        int minX = -28, maxX = -12, minZ = 18, maxZ = 29;
        clear(level, origin, minX - 1, minZ - 1, maxX + 1, maxZ + 1, 4);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                boolean gate = z == minZ && (x == -21 || x == -20);
                boolean edge = x == minX || x == maxX || z == minZ || z == maxZ;
                if (edge) {
                    set(level, origin.offset(x, 0, z), gate
                            ? Blocks.OAK_FENCE_GATE.defaultBlockState()
                            .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                            : Blocks.OAK_FENCE.defaultBlockState());
                    continue;
                }
                if (x == -20) {
                    set(level, origin.offset(x, 0, z), Blocks.WATER.defaultBlockState());
                    continue;
                }
                set(level, origin.offset(x, 0, z), Blocks.FARMLAND.defaultBlockState());
                set(level, origin.offset(x, 1, z), Math.floorMod(x + z, 3) == 0
                        ? Blocks.WHEAT.defaultBlockState()
                        : Math.floorMod(x + z, 3) == 1
                        ? Blocks.CARROTS.defaultBlockState()
                        : Blocks.POTATOES.defaultBlockState());
            }
        }
    }

    private static void orchard(ServerLevel level, BlockPos origin) {
        clear(level, origin, 11, 17, 29, 30, 6);
        for (int x = 12; x <= 28; x++) {
            for (int z = 18; z <= 29; z++) {
                set(level, origin.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState());
            }
        }
        for (int x = 14; x <= 26; x += 6) {
            for (int z = 20; z <= 27; z += 7) {
                set(level, origin.offset(x, 0, z), Blocks.OAK_LOG.defaultBlockState());
                set(level, origin.offset(x, 1, z), Blocks.OAK_LOG.defaultBlockState());
                set(level, origin.offset(x, 2, z), Blocks.OAK_LOG.defaultBlockState());
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        if (Math.abs(dx) + Math.abs(dz) > 3) continue;
                        set(level, origin.offset(x + dx, 3, z + dz), Blocks.OAK_LEAVES.defaultBlockState());
                    }
                }
            }
        }
        for (int x = 12; x <= 28; x += 2) {
            set(level, origin.offset(x, 0, 18), Blocks.OAK_FENCE.defaultBlockState());
            set(level, origin.offset(x, 0, 29), Blocks.OAK_FENCE.defaultBlockState());
        }
    }

    private static void publicRanch(ServerLevel level, BlockPos origin) {
        int minX = 30, maxX = 50, minZ = 30, maxZ = 44;
        clear(level, origin, minX - 1, minZ - 1, maxX + 1, maxZ + 1, 7);
        for (int x = minX; x <= maxX; x++) {
            boolean gate = zEquals(minZ) && (x == 40 || x == 41);
            set(level, origin.offset(x, 0, minZ), gate
                    ? Blocks.OAK_FENCE_GATE.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                    : Blocks.OAK_FENCE.defaultBlockState());
            set(level, origin.offset(x, 0, maxZ), Blocks.OAK_FENCE.defaultBlockState());
        }
        for (int z = minZ + 1; z < maxZ; z++) {
            set(level, origin.offset(minX, 0, z), Blocks.OAK_FENCE.defaultBlockState());
            set(level, origin.offset(maxX, 0, z), Blocks.OAK_FENCE.defaultBlockState());
        }
        for (int x = 32; x <= 38; x++) {
            for (int z = 32; z <= 37; z++) {
                set(level, origin.offset(x, -1, z), Blocks.COARSE_DIRT.defaultBlockState());
            }
        }
        for (int x = 32; x <= 38; x++) {
            set(level, origin.offset(x, 4, 32), Blocks.SPRUCE_PLANKS.defaultBlockState());
            set(level, origin.offset(x, 4, 37), Blocks.SPRUCE_PLANKS.defaultBlockState());
        }
        set(level, origin.offset(34, 0, 39), Blocks.WATER.defaultBlockState());
        set(level, origin.offset(46, 0, 39), Blocks.HAY_BLOCK.defaultBlockState());
    }

    private static boolean zEquals(int ignored) {
        return true;
    }

    private static void fishingPond(ServerLevel level, BlockPos origin) {
        int minX = -50, maxX = -34, minZ = 30, maxZ = 44;
        clear(level, origin, minX - 1, minZ - 1, maxX + 1, maxZ + 1, 4);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                boolean edge = x == minX || x == maxX || z == minZ || z == maxZ;
                set(level, origin.offset(x, -1, z), edge
                        ? Blocks.STONE.defaultBlockState()
                        : Blocks.WATER.defaultBlockState());
                set(level, origin.offset(x, 0, z), Blocks.AIR.defaultBlockState());
            }
        }
        for (int x = -43; x <= -40; x++) {
            for (int z = 27; z <= 36; z++) {
                set(level, origin.offset(x, -1, z), Blocks.OAK_PLANKS.defaultBlockState());
            }
        }
    }

    private static void connectPaths(ServerLevel level, BlockPos origin) {
        path(level, origin, -44, -31, -2, -31);
        path(level, origin, 2, -31, 44, -31);
        path(level, origin, -44, 2, -44, 8);
        path(level, origin, 44, 2, 44, 8);
        path(level, origin, -22, 2, -22, 18);
        path(level, origin, 21, 2, 21, 18);
        path(level, origin, -22, 29, -22, 31);
        path(level, origin, 21, 29, 21, 31);
        path(level, origin, -42, 26, -42, 30);
        path(level, origin, 40, 26, 40, 30);
    }

    private static void path(ServerLevel level, BlockPos origin, int x0, int z0, int x1, int z1) {
        int x = x0;
        int z = z0;
        while (x != x1 || z != z1) {
            pathDot(level, origin, x, z);
            if (x != x1) x += Integer.compare(x1, x);
            else z += Integer.compare(z1, z);
        }
        pathDot(level, origin, x1, z1);
    }

    private static void pathDot(ServerLevel level, BlockPos origin, int x, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                set(level, origin.offset(x + dx, -1, z + dz), Blocks.PACKED_MUD.defaultBlockState());
                set(level, origin.offset(x + dx, 0, z + dz), Blocks.AIR.defaultBlockState());
            }
        }
    }

    private static void placeDoor(ServerLevel level, BlockPos lower, Direction facing) {
        BlockState base = Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, facing);
        set(level, lower, base.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        set(level, lower.above(), base.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    private static BlockState stair(Direction facing) {
        return Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, facing);
    }

    private static void clear(
            ServerLevel level,
            BlockPos origin,
            int minX,
            int minZ,
            int maxX,
            int maxZ,
            int height
    ) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = 0; y <= height; y++) {
                    set(level, origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
                set(level, origin.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState());
            }
        }
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, FLAGS);
    }

    private enum Workshop {
        BAKERY,
        CARPENTER,
        SCHOOL,
        CLINIC,
        TAILOR,
        FORGE
    }
}
