package kr.countrysidedays.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** Adds shared housing and primary-industry workplaces for the expanded resident roster. */
public final class PublicVillageExpansionBuilder {
    private static final int FLAGS = Block.UPDATE_ALL;

    private PublicVillageExpansionBuilder() {
    }

    public static void ensureExpanded(ServerLevel level, BlockPos origin) {
        if (level.getBlockState(origin.offset(40, 0, 30)).is(Blocks.OAK_FENCE_GATE)
                && level.getBlockState(origin.offset(-22, 1, -20)).is(Blocks.BARREL)) {
            return;
        }

        buildPaths(level, origin);

        cottage(level, origin.offset(-50, 0, -42), true, DyeColor.YELLOW, DyeColor.WHITE);
        cottage(level, origin.offset(38, 0, -42), true, DyeColor.LIGHT_BLUE, DyeColor.WHITE);
        cottage(level, origin.offset(-50, 0, 4), true, DyeColor.PINK, DyeColor.YELLOW);
        cottage(level, origin.offset(38, 0, 4), true, DyeColor.GREEN, DyeColor.WHITE);
        cottage(level, origin.offset(-28, 0, 31), false, DyeColor.ORANGE, DyeColor.WHITE);
        cottage(level, origin.offset(15, 0, 31), false, DyeColor.BLUE, DyeColor.YELLOW);

        workshop(level, origin.offset(-27, 0, -24), Workshop.BAKERY);
        workshop(level, origin.offset(16, 0, -24), Workshop.CARPENTER);
        workshop(level, origin.offset(-27, 0, -43), Workshop.SCHOOL);
        workshop(level, origin.offset(16, 0, -43), Workshop.CLINIC);
        stall(level, origin.offset(-36, 0, 4), Workshop.TAILOR);
        stall(level, origin.offset(24, 0, 4), Workshop.FORGE);

        publicFarm(level, origin);
        orchard(level, origin);
        publicRanch(level, origin);
        fishingPond(level, origin);
    }

    private static void cottage(
            ServerLevel level,
            BlockPos base,
            boolean frontSouth,
            DyeColor firstBed,
            DyeColor secondBed
    ) {
        clearLocal(level, base, -1, -1, 11, 9, 7);
        houseShell(level, base, 11, 9, frontSouth, Blocks.SPRUCE_PLANKS.defaultBlockState());
        placeBed(level, base.offset(2, 1, 5), Direction.SOUTH, firstBed);
        placeBed(level, base.offset(7, 1, 5), Direction.SOUTH, secondBed);
        set(level, base.offset(2, 1, 2), Blocks.CHEST.defaultBlockState());
        set(level, base.offset(3, 1, 2), Blocks.CRAFTING_TABLE.defaultBlockState());
        tableAndSeats(level, base.offset(6, 1, 3));
    }

    private static void workshop(ServerLevel level, BlockPos base, Workshop type) {
        clearLocal(level, base, -1, -1, 11, 9, 7);
        BlockState roof = switch (type) {
            case BAKERY -> Blocks.BRICKS.defaultBlockState();
            case SCHOOL -> Blocks.DEEPSLATE_TILES.defaultBlockState();
            case CLINIC -> Blocks.BIRCH_PLANKS.defaultBlockState();
            default -> Blocks.SPRUCE_PLANKS.defaultBlockState();
        };
        houseShell(level, base, 11, 9, true, roof);
        furnish(level, base, type, 1);
    }

    private static void stall(ServerLevel level, BlockPos base, Workshop type) {
        clearLocal(level, base, -1, -1, 9, 8, 6);
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
        BlockState roof = type == Workshop.FORGE
                ? Blocks.BRICKS.defaultBlockState()
                : Blocks.BIRCH_PLANKS.defaultBlockState();
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 7; z++) set(level, base.offset(x, 4, z), roof);
        }
        furnish(level, base, type, 0);
    }

    private static void houseShell(
            ServerLevel level,
            BlockPos base,
            int width,
            int depth,
            boolean frontSouth,
            BlockState roof
    ) {
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                set(level, base.offset(x, -1, z), Blocks.COBBLESTONE.defaultBlockState());
                set(level, base.offset(x, 0, z), Blocks.BIRCH_PLANKS.defaultBlockState());
            }
        }
        for (int y = 1; y <= 4; y++) {
            for (int x = 0; x < width; x++) {
                wall(level, base, x, y, 0, width, depth);
                wall(level, base, x, y, depth - 1, width, depth);
            }
            for (int z = 1; z < depth - 1; z++) {
                wall(level, base, 0, y, z, width, depth);
                wall(level, base, width - 1, y, z, width, depth);
            }
        }
        for (int x = -1; x <= width; x++) {
            for (int z = -1; z <= depth; z++) set(level, base.offset(x, 5, z), roof);
        }
        int doorZ = frontSouth ? depth - 1 : 0;
        Direction front = frontSouth ? Direction.SOUTH : Direction.NORTH;
        set(level, base.offset(width / 2, 1, doorZ), Blocks.AIR.defaultBlockState());
        set(level, base.offset(width / 2, 2, doorZ), Blocks.AIR.defaultBlockState());
        placeDoor(level, base.offset(width / 2, 1, doorZ), front);
    }

    private static void wall(
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
                && (((z == 0 || z == depth - 1) && (x == 2 || x == width - 3))
                || ((x == 0 || x == width - 1) && (z == 2 || z == depth - 3)));
        set(level, base.offset(x, y, z), corner
                ? Blocks.STRIPPED_OAK_LOG.defaultBlockState()
                : window
                ? Blocks.GLASS.defaultBlockState()
                : Blocks.OAK_PLANKS.defaultBlockState());
    }

    private static void furnish(ServerLevel level, BlockPos base, Workshop type, int y) {
        switch (type) {
            case BAKERY -> {
                set(level, base.offset(2, y, 2), Blocks.FURNACE.defaultBlockState());
                set(level, base.offset(3, y, 2), Blocks.SMOKER.defaultBlockState());
                set(level, base.offset(5, y, 2), Blocks.BARREL.defaultBlockState());
            }
            case CARPENTER -> {
                set(level, base.offset(2, y, 2), Blocks.CRAFTING_TABLE.defaultBlockState());
                set(level, base.offset(3, y, 2), Blocks.CARTOGRAPHY_TABLE.defaultBlockState());
                set(level, base.offset(5, y, 2), Blocks.BARREL.defaultBlockState());
            }
            case SCHOOL -> {
                set(level, base.offset(2, y, 2), Blocks.LECTERN.defaultBlockState());
                set(level, base.offset(7, y, 2), Blocks.BOOKSHELF.defaultBlockState());
                set(level, base.offset(8, y, 2), Blocks.BOOKSHELF.defaultBlockState());
                tableAndSeats(level, base.offset(5, y, 5));
            }
            case CLINIC -> {
                set(level, base.offset(2, y, 2), Blocks.BREWING_STAND.defaultBlockState());
                set(level, base.offset(4, y, 2), Blocks.BARREL.defaultBlockState());
                placeBed(level, base.offset(7, y, 4), Direction.SOUTH, DyeColor.WHITE);
            }
            case TAILOR -> {
                set(level, base.offset(2, y, 4), Blocks.LOOM.defaultBlockState());
                set(level, base.offset(4, y, 4), Blocks.BARREL.defaultBlockState());
                set(level, base.offset(6, y, 4), Blocks.WOOL.pick(DyeColor.WHITE).defaultBlockState());
            }
            case FORGE -> {
                set(level, base.offset(2, y, 4), Blocks.BLAST_FURNACE.defaultBlockState());
                set(level, base.offset(4, y, 4), Blocks.ANVIL.defaultBlockState());
                set(level, base.offset(6, y, 4), Blocks.SMITHING_TABLE.defaultBlockState());
            }
        }
    }

    private static void publicFarm(ServerLevel level, BlockPos origin) {
        int minX = -28, maxX = -12, minZ = 18, maxZ = 29;
        clearRelative(level, origin, minX - 1, minZ - 1, maxX + 1, maxZ + 1, 4);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                boolean gate = z == minZ && (x == -21 || x == -20);
                boolean edge = x == minX || x == maxX || z == minZ || z == maxZ;
                if (edge) {
                    set(level, origin.offset(x, 0, z), gate
                            ? Blocks.OAK_FENCE_GATE.defaultBlockState()
                            .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                            : Blocks.OAK_FENCE.defaultBlockState());
                } else if (x == -20) {
                    set(level, origin.offset(x, 0, z), Blocks.WATER.defaultBlockState());
                } else {
                    set(level, origin.offset(x, 0, z), Blocks.FARMLAND.defaultBlockState());
                    set(level, origin.offset(x, 1, z), Math.floorMod(x + z, 3) == 0
                            ? Blocks.WHEAT.defaultBlockState()
                            : Math.floorMod(x + z, 3) == 1
                            ? Blocks.CARROTS.defaultBlockState()
                            : Blocks.POTATOES.defaultBlockState());
                }
            }
        }
    }

    private static void orchard(ServerLevel level, BlockPos origin) {
        clearRelative(level, origin, 11, 17, 29, 30, 6);
        for (int x = 12; x <= 28; x++) {
            for (int z = 18; z <= 29; z++) set(level, origin.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState());
        }
        for (int x = 14; x <= 26; x += 6) {
            for (int z = 20; z <= 27; z += 7) {
                for (int y = 0; y <= 2; y++) set(level, origin.offset(x, y, z), Blocks.OAK_LOG.defaultBlockState());
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        if (Math.abs(dx) + Math.abs(dz) <= 3) {
                            set(level, origin.offset(x + dx, 3, z + dz), Blocks.OAK_LEAVES.defaultBlockState());
                        }
                    }
                }
            }
        }
        for (int x = 12; x <= 28; x++) {
            set(level, origin.offset(x, 0, 18), Blocks.OAK_FENCE.defaultBlockState());
            set(level, origin.offset(x, 0, 29), Blocks.OAK_FENCE.defaultBlockState());
        }
        for (int z = 19; z < 29; z++) {
            set(level, origin.offset(12, 0, z), Blocks.OAK_FENCE.defaultBlockState());
            set(level, origin.offset(28, 0, z), Blocks.OAK_FENCE.defaultBlockState());
        }
        set(level, origin.offset(20, 0, 18), Blocks.OAK_FENCE_GATE.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }

    private static void publicRanch(ServerLevel level, BlockPos origin) {
        int minX = 30, maxX = 50, minZ = 30, maxZ = 44;
        clearRelative(level, origin, minX - 1, minZ - 1, maxX + 1, maxZ + 1, 7);
        for (int x = minX; x <= maxX; x++) {
            boolean gate = x == 40 || x == 41;
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
            for (int z = 32; z <= 37; z++) set(level, origin.offset(x, -1, z), Blocks.COARSE_DIRT.defaultBlockState());
        }
        for (int x = 31; x <= 39; x++) {
            for (int z = 31; z <= 38; z++) set(level, origin.offset(x, 4, z), Blocks.SPRUCE_PLANKS.defaultBlockState());
        }
        BlockPos trough = origin.offset(34, 0, 39);
        set(level, trough, Blocks.WATER.defaultBlockState());
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            set(level, trough.relative(direction), Blocks.STONE_BRICK_SLAB.defaultBlockState());
        }
        set(level, origin.offset(46, 0, 39), Blocks.HAY_BLOCK.defaultBlockState());
        set(level, origin.offset(46, 1, 39), Blocks.HAY_BLOCK.defaultBlockState());
    }

    private static void fishingPond(ServerLevel level, BlockPos origin) {
        int minX = -50, maxX = -34, minZ = 30, maxZ = 44;
        clearRelative(level, origin, minX - 1, minZ - 1, maxX + 1, maxZ + 1, 4);
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
            for (int z = 27; z <= 36; z++) set(level, origin.offset(x, -1, z), Blocks.OAK_PLANKS.defaultBlockState());
        }
    }

    private static void buildPaths(ServerLevel level, BlockPos origin) {
        path(level, origin, -50, -31, 50, -31);
        path(level, origin, -30, -31, -30, 42);
        path(level, origin, 28, -31, 28, 42);
        path(level, origin, -50, 16, 50, 16);
        path(level, origin, -42, 16, -42, 30);
        path(level, origin, 40, 16, 40, 30);
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

    private static void tableAndSeats(ServerLevel level, BlockPos table) {
        set(level, table, Blocks.OAK_FENCE.defaultBlockState());
        set(level, table.above(), Blocks.OAK_PRESSURE_PLATE.defaultBlockState());
        set(level, table.west(), Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
        set(level, table.east(), Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST));
    }

    private static void placeBed(ServerLevel level, BlockPos foot, Direction facing, DyeColor color) {
        Block bed = Blocks.BED.pick(color);
        set(level, foot, bed.defaultBlockState().setValue(BedBlock.FACING, facing).setValue(BedBlock.PART, BedPart.FOOT));
        set(level, foot.relative(facing), bed.defaultBlockState().setValue(BedBlock.FACING, facing).setValue(BedBlock.PART, BedPart.HEAD));
    }

    private static void placeDoor(ServerLevel level, BlockPos lower, Direction facing) {
        BlockState base = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, facing);
        set(level, lower, base.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        set(level, lower.above(), base.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    private static void clearLocal(ServerLevel level, BlockPos base, int minX, int minZ, int maxX, int maxZ, int height) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = 0; y <= height; y++) set(level, base.offset(x, y, z), Blocks.AIR.defaultBlockState());
                set(level, base.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState());
            }
        }
    }

    private static void clearRelative(ServerLevel level, BlockPos origin, int minX, int minZ, int maxX, int maxZ, int height) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = 0; y <= height; y++) set(level, origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
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
