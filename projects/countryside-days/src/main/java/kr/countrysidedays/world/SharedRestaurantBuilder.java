package kr.countrysidedays.world;

import kr.countrysidedays.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** Rebuilds legacy private restaurants into one shared restaurant plus staff gardens. */
public final class SharedRestaurantBuilder {
    private static final int FLAGS = Block.UPDATE_ALL;

    private SharedRestaurantBuilder() {
    }

    public static void normalizeEstate(
            ServerLevel level,
            CountrysideWorldData.PlayerEstate estate,
            boolean restaurantOwner,
            boolean restaurantOpen
    ) {
        if (restaurantOwner) {
            if (!isCurrentSharedRestaurant(level, estate.originPos())) {
                buildSharedRestaurant(level, estate.originPos(), estate.ownerName(), estate.restaurantName());
            } else {
                refreshSign(level, estate.originPos(), estate.ownerName(), estate.restaurantName());
            }
            setOpen(level, estate.originPos(), restaurantOpen);
            return;
        }

        if (hasGeneratedRestaurant(level, estate.originPos())) {
            buildStaffGarden(level, estate.originPos());
        }
    }

    public static void buildSharedRestaurant(
            ServerLevel level,
            BlockPos origin,
            String ownerName,
            String restaurantName
    ) {
        clearRestaurantArea(level, origin);
        buildEntrancePath(level, origin);

        BlockPos base = origin.offset(7, 0, -20);
        buildShell(level, base);
        placeDoor(level, PlayerEstateLayout.restaurantDoor(origin), Direction.NORTH);

        set(level, PlayerEstateLayout.kitchenCounter(origin),
                ModBlocks.COUNTRY_KITCHEN_COUNTER.get().defaultBlockState());
        set(level, origin.offset(12, 1, -10),
                Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.NORTH));
        set(level, origin.offset(13, 1, -10), Blocks.BARREL.defaultBlockState());
        set(level, origin.offset(14, 1, -10), Blocks.CRAFTING_TABLE.defaultBlockState());
        set(level, origin.offset(15, 1, -10),
                Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH));
        set(level, origin.offset(24, 1, -10), Blocks.BOOKSHELF.defaultBlockState());
        set(level, origin.offset(25, 1, -10), Blocks.BOOKSHELF.defaultBlockState());

        diningTable(level, origin.offset(12, 1, -14));
        diningTable(level, origin.offset(17, 1, -14));
        diningTable(level, origin.offset(22, 1, -14));

        set(level, origin.offset(9, 1, -18), Blocks.LANTERN.defaultBlockState());
        set(level, origin.offset(25, 1, -18), Blocks.LANTERN.defaultBlockState());
        set(level, origin.offset(9, 1, -9), Blocks.FLOWER_POT.defaultBlockState());
        set(level, origin.offset(26, 1, -9), Blocks.FLOWER_POT.defaultBlockState());

        buildRestaurantFence(level, origin);
        refreshSign(level, origin, ownerName, restaurantName);
        setOpen(level, origin, false);
    }

    public static void buildStaffGarden(ServerLevel level, BlockPos origin) {
        clearRestaurantArea(level, origin);
        for (int x = 7; x <= 27; x++) {
            for (int z = -20; z <= -8; z++) {
                set(level, origin.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState());
            }
        }
        for (int x = 9; x <= 25; x += 4) {
            for (int z = -18; z <= -10; z += 4) {
                BlockState flower = Math.floorMod(x + z, 3) == 0
                        ? Blocks.DANDELION.defaultBlockState()
                        : Math.floorMod(x + z, 3) == 1
                        ? Blocks.POPPY.defaultBlockState()
                        : Blocks.CORNFLOWER.defaultBlockState();
                set(level, origin.offset(x, 0, z), flower);
            }
        }
        for (int x = 7; x <= 27; x++) {
            set(level, origin.offset(x, -1, -14), road(x, -14));
        }
        set(level, origin.offset(12, 0, -12), stair(Direction.EAST));
        set(level, origin.offset(13, 0, -12), stair(Direction.WEST));
        set(level, origin.offset(21, 0, -16), stair(Direction.EAST));
        set(level, origin.offset(22, 0, -16), stair(Direction.WEST));
    }

    public static void refreshSign(
            ServerLevel level,
            BlockPos origin,
            String ownerName,
            String restaurantName
    ) {
        String owner = ownerName == null || ownerName.isBlank() ? "새 주민" : ownerName;
        String title = restaurantName == null || restaurantName.isBlank()
                ? owner + "의 시골식당" : restaurantName;
        BlockPos signPos = PlayerEstateLayout.restaurantSign(origin);
        set(level, signPos, Blocks.OAK_WALL_SIGN.defaultBlockState()
                .setValue(WallSignBlock.FACING, Direction.NORTH));
        if (!(level.getBlockEntity(signPos) instanceof SignBlockEntity sign)) return;
        SignText text = sign.getFrontText()
                .setMessage(0, Component.literal(title))
                .setMessage(1, Component.literal("공동 식당"))
                .setMessage(2, Component.literal("주인 " + owner))
                .setMessage(3, Component.literal("영업 중에만 입장"));
        sign.setText(text, true);
        sign.setChanged();
    }

    public static void setOpen(ServerLevel level, BlockPos origin, boolean open) {
        setOpenProperty(level, PlayerEstateLayout.restaurantGate(origin), open);
        BlockPos lowerDoor = PlayerEstateLayout.restaurantDoor(origin);
        setOpenProperty(level, lowerDoor, open);
        setOpenProperty(level, lowerDoor.above(), open);
    }

    public static boolean isCurrentSharedRestaurant(ServerLevel level, BlockPos origin) {
        return level.getBlockState(PlayerEstateLayout.kitchenCounter(origin))
                        .is(ModBlocks.COUNTRY_KITCHEN_COUNTER.get())
                && level.getBlockState(PlayerEstateLayout.restaurantGate(origin)).is(Blocks.OAK_FENCE_GATE)
                && level.getBlockState(PlayerEstateLayout.restaurantDoor(origin)).is(Blocks.SPRUCE_DOOR);
    }

    private static boolean hasGeneratedRestaurant(ServerLevel level, BlockPos origin) {
        if (isCurrentSharedRestaurant(level, origin)) return true;
        if (level.getBlockState(origin.offset(10, 1, -14)).is(ModBlocks.COUNTRY_KITCHEN_COUNTER.get())) {
            return true;
        }
        for (int x = PlayerEstateLayout.RESTAURANT_MIN_X;
             x <= PlayerEstateLayout.RESTAURANT_MAX_X; x++) {
            for (int z = PlayerEstateLayout.RESTAURANT_MIN_Z;
                 z <= PlayerEstateLayout.RESTAURANT_MAX_Z; z++) {
                if (level.getBlockState(origin.offset(x, 1, z))
                        .is(ModBlocks.COUNTRY_KITCHEN_COUNTER.get())) return true;
            }
        }
        return false;
    }

    private static void clearRestaurantArea(ServerLevel level, BlockPos origin) {
        for (int x = PlayerEstateLayout.RESTAURANT_MIN_X;
             x <= PlayerEstateLayout.RESTAURANT_MAX_X; x++) {
            for (int z = PlayerEstateLayout.RESTAURANT_MIN_Z;
                 z <= PlayerEstateLayout.RESTAURANT_MAX_Z; z++) {
                for (int y = 0; y <= 9; y++) {
                    set(level, origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
                set(level, origin.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState());
            }
        }
    }

    private static void buildEntrancePath(ServerLevel level, BlockPos origin) {
        for (int x = 0; x <= 17; x++) {
            for (int z = -25; z <= -22; z++) {
                set(level, origin.offset(x, -1, z), road(x, z));
                set(level, origin.offset(x, 0, z), Blocks.AIR.defaultBlockState());
            }
        }
    }

    private static void buildShell(ServerLevel level, BlockPos base) {
        int width = 21;
        int depth = 13;
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                set(level, base.offset(x, -1, z), Blocks.COBBLESTONE.defaultBlockState());
                set(level, base.offset(x, 0, z), Blocks.BIRCH_PLANKS.defaultBlockState());
            }
        }

        for (int y = 1; y <= 5; y++) {
            for (int x = 0; x < width; x++) {
                wallBlock(level, base, x, y, 0, width, depth);
                wallBlock(level, base, x, y, depth - 1, width, depth);
            }
            for (int z = 1; z < depth - 1; z++) {
                wallBlock(level, base, 0, y, z, width, depth);
                wallBlock(level, base, width - 1, y, z, width, depth);
            }
        }

        for (int x = -1; x <= width; x++) {
            for (int z = -1; z <= depth; z++) {
                set(level, base.offset(x, 6, z), Blocks.BRICKS.defaultBlockState());
            }
        }
        for (int x = 1; x < width - 1; x++) {
            for (int z = 1; z < depth - 1; z++) {
                set(level, base.offset(x, 7, z), Blocks.BRICK_SLAB.defaultBlockState());
            }
        }

        set(level, base.offset(width / 2, 1, 0), Blocks.AIR.defaultBlockState());
        set(level, base.offset(width / 2, 2, 0), Blocks.AIR.defaultBlockState());
    }

    private static void wallBlock(
            ServerLevel level,
            BlockPos base,
            int x,
            int y,
            int z,
            int width,
            int depth
    ) {
        boolean corner = (x == 0 || x == width - 1) && (z == 0 || z == depth - 1);
        boolean frontWindow = z == 0 && (y == 2 || y == 3)
                && (x == 3 || x == 4 || x == 16 || x == 17);
        boolean rearWindow = z == depth - 1 && (y == 2 || y == 3)
                && (x == 2 || x == 3 || x == 17 || x == 18);
        boolean sideWindow = (x == 0 || x == width - 1) && (y == 2 || y == 3)
                && (z == 3 || z == 4 || z == 8 || z == 9);
        BlockState state = corner
                ? Blocks.STRIPPED_OAK_LOG.defaultBlockState()
                : frontWindow || rearWindow || sideWindow
                ? Blocks.GLASS.defaultBlockState()
                : Blocks.OAK_PLANKS.defaultBlockState();
        set(level, base.offset(x, y, z), state);
    }

    private static void buildRestaurantFence(ServerLevel level, BlockPos origin) {
        for (int x = 6; x <= 28; x++) {
            BlockPos pos = origin.offset(x, 0, -23);
            if (x == 17) {
                set(level, pos, Blocks.OAK_FENCE_GATE.defaultBlockState()
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                        .setValue(BlockStateProperties.OPEN, false));
            } else {
                set(level, pos, Blocks.OAK_FENCE.defaultBlockState());
            }
        }
        for (int z = -22; z <= -20; z++) {
            set(level, origin.offset(6, 0, z), Blocks.OAK_FENCE.defaultBlockState());
            set(level, origin.offset(28, 0, z), Blocks.OAK_FENCE.defaultBlockState());
        }
    }

    private static void diningTable(ServerLevel level, BlockPos table) {
        set(level, table, Blocks.OAK_FENCE.defaultBlockState());
        set(level, table.above(), Blocks.OAK_PRESSURE_PLATE.defaultBlockState());
        set(level, table.north(), stair(Direction.SOUTH));
        set(level, table.south(), stair(Direction.NORTH));
    }

    private static BlockState stair(Direction facing) {
        return Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, facing);
    }

    private static void placeDoor(ServerLevel level, BlockPos lower, Direction facing) {
        BlockState base = Blocks.SPRUCE_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, facing)
                .setValue(BlockStateProperties.OPEN, false);
        set(level, lower, base.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        set(level, lower.above(), base.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    private static void setOpenProperty(ServerLevel level, BlockPos pos, boolean open) {
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(BlockStateProperties.OPEN)) return;
        if (state.getValue(BlockStateProperties.OPEN) == open) return;
        set(level, pos, state.setValue(BlockStateProperties.OPEN, open));
    }

    private static BlockState road(int x, int z) {
        return Math.floorMod(x * 3 + z, 8) == 0
                ? Blocks.GRAVEL.defaultBlockState()
                : Blocks.PACKED_MUD.defaultBlockState();
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, FLAGS);
    }
}
