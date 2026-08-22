package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BuildingBlueprints {
    public enum Phase {
        FLOOR,
        FRAME_AND_WALLS,
        ROOF,
        FINISH
    }

    public record Placement(BlockPos pos, BlockState state, Phase phase) {}

    private BuildingBlueprints() {}

    public static List<Placement> create(BuildingType type, BlockPos origin) {
        return switch (type) {
            case HOUSE -> house(origin);
            case LUMBER_CAMP -> lumberCamp(origin);
            case FARM -> farm(origin);
            case QUARRY -> quarry(origin);
            case MINE -> mine(origin);
            case WAREHOUSE -> warehouse(origin);
        };
    }

    private static List<Placement> house(BlockPos o) {
        BlueprintBuilder b = new BlueprintBuilder(o);
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) b.put(x, 0, z, Blocks.SPRUCE_PLANKS.defaultBlockState(), Phase.FLOOR);
        }
        for (int y = 1; y <= 4; y++) {
            for (int x = 0; x < 9; x++) {
                if (!isHouseDoorOpening(x, y, 0)) b.put(x, y, 0, houseWallState(x, y, 0), Phase.FRAME_AND_WALLS);
                if (!isHouseDoorOpening(x, y, 8)) b.put(x, y, 8, houseWallState(x, y, 8), Phase.FRAME_AND_WALLS);
            }
            for (int z = 1; z < 8; z++) {
                b.put(0, y, z, houseWallState(0, y, z), Phase.FRAME_AND_WALLS);
                b.put(8, y, z, houseWallState(8, y, z), Phase.FRAME_AND_WALLS);
            }
        }
        for (int y = 1; y <= 4; y++) {
            b.put(0, y, 0, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), Phase.FRAME_AND_WALLS);
            b.put(8, y, 0, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), Phase.FRAME_AND_WALLS);
            b.put(0, y, 8, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), Phase.FRAME_AND_WALLS);
            b.put(8, y, 8, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), Phase.FRAME_AND_WALLS);
        }
        for (int x = 0; x < 9; x++) {
            b.put(x, 4, 0, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), Phase.FRAME_AND_WALLS);
            b.put(x, 4, 8, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), Phase.FRAME_AND_WALLS);
        }
        for (int x = 1; x <= 7; x++) {
            b.put(x, 5, 0, Blocks.OAK_PLANKS.defaultBlockState(), Phase.FRAME_AND_WALLS);
            b.put(x, 5, 8, Blocks.OAK_PLANKS.defaultBlockState(), Phase.FRAME_AND_WALLS);
        }
        for (int x = 2; x <= 6; x++) {
            b.put(x, 6, 0, Blocks.OAK_PLANKS.defaultBlockState(), Phase.FRAME_AND_WALLS);
            b.put(x, 6, 8, Blocks.OAK_PLANKS.defaultBlockState(), Phase.FRAME_AND_WALLS);
        }
        for (int x = 3; x <= 5; x++) {
            b.put(x, 7, 0, Blocks.OAK_PLANKS.defaultBlockState(), Phase.FRAME_AND_WALLS);
            b.put(x, 7, 8, Blocks.OAK_PLANKS.defaultBlockState(), Phase.FRAME_AND_WALLS);
        }
        b.put(4, 8, 0, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), Phase.FRAME_AND_WALLS);
        b.put(4, 8, 8, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), Phase.FRAME_AND_WALLS);

        BlockState leftRoof = Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
        BlockState rightRoof = Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST);
        for (int z = -1; z <= 9; z++) {
            for (int layer = 0; layer < 5; layer++) {
                int y = 4 + layer;
                b.put(-1 + layer, y, z, leftRoof, Phase.ROOF);
                b.put(9 - layer, y, z, rightRoof, Phase.ROOF);
            }
            b.put(4, 9, z, Blocks.SPRUCE_SLAB.defaultBlockState(), Phase.ROOF);
        }

        BlockState lowerDoor = Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.SOUTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        BlockState upperDoor = lowerDoor.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
        b.put(4, 1, 8, lowerDoor, Phase.FINISH);
        b.put(4, 2, 8, upperDoor, Phase.FINISH);
        b.put(2, 1, 2, Blocks.LANTERN.defaultBlockState(), Phase.FINISH);
        b.put(6, 1, 2, Blocks.LANTERN.defaultBlockState(), Phase.FINISH);
        b.put(2, 1, 6, Blocks.LANTERN.defaultBlockState(), Phase.FINISH);
        b.put(6, 1, 6, Blocks.LANTERN.defaultBlockState(), Phase.FINISH);
        b.put(4, 1, 3, Blocks.CRAFTING_TABLE.defaultBlockState(), Phase.FINISH);
        return b.build();
    }

    private static boolean isHouseDoorOpening(int x, int y, int z) {
        return z == 8 && x == 4 && (y == 1 || y == 2);
    }

    private static BlockState houseWallState(int x, int y, int z) {
        boolean frontBackWindow = (z == 0 || z == 8) && (x == 2 || x == 6) && (y == 2 || y == 3);
        boolean sideWindow = (x == 0 || x == 8) && (z == 2 || z == 6) && (y == 2 || y == 3);
        if (frontBackWindow || sideWindow) return Blocks.GLASS.defaultBlockState();
        return Blocks.OAK_PLANKS.defaultBlockState();
    }

    private static List<Placement> lumberCamp(BlockPos o) {
        BlueprintBuilder b = new BlueprintBuilder(o);
        for (int x = 0; x < 11; x++) {
            for (int z = 0; z < 9; z++) b.put(x, 0, z, Blocks.SPRUCE_PLANKS.defaultBlockState(), Phase.FLOOR);
        }
        for (int y = 1; y <= 4; y++) {
            for (int x = 0; x < 11; x++) {
                BlockState state = ((x == 2 || x == 5 || x == 8) && (y == 2 || y == 3))
                        ? Blocks.GLASS.defaultBlockState() : Blocks.OAK_PLANKS.defaultBlockState();
                b.put(x, y, 0, state, Phase.FRAME_AND_WALLS);
            }
        }
        for (int y = 1; y <= 3; y++) {
            for (int z = 1; z <= 4; z++) {
                b.put(0, y, z, Blocks.OAK_PLANKS.defaultBlockState(), Phase.FRAME_AND_WALLS);
                b.put(10, y, z, Blocks.OAK_PLANKS.defaultBlockState(), Phase.FRAME_AND_WALLS);
            }
        }
        int[][] posts = new int[][] {{0, 0}, {5, 0}, {10, 0}, {0, 8}, {5, 8}, {10, 8}};
        for (int[] post : posts) {
            for (int y = 1; y <= 4; y++) b.put(post[0], y, post[1], Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), Phase.FRAME_AND_WALLS);
        }
        for (int x = 0; x < 11; x++) {
            b.put(x, 4, 0, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), Phase.FRAME_AND_WALLS);
            b.put(x, 4, 8, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), Phase.FRAME_AND_WALLS);
        }
        for (int layer = 0; layer < 5; layer++) {
            int y = 5 + layer;
            for (int x = 1 + layer; x <= 9 - layer; x++) b.put(x, y, 0, Blocks.OAK_PLANKS.defaultBlockState(), Phase.FRAME_AND_WALLS);
        }
        b.put(5, 9, 0, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), Phase.FRAME_AND_WALLS);
        BlockState leftRoof = Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
        BlockState rightRoof = Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST);
        for (int z = -1; z <= 9; z++) {
            for (int layer = 0; layer < 6; layer++) {
                int y = 4 + layer;
                b.put(-1 + layer, y, z, leftRoof, Phase.ROOF);
                b.put(11 - layer, y, z, rightRoof, Phase.ROOF);
            }
            b.put(5, 10, z, Blocks.SPRUCE_SLAB.defaultBlockState(), Phase.ROOF);
        }
        b.put(5, 1, 3, Blocks.CRAFTING_TABLE.defaultBlockState(), Phase.FINISH);
        b.put(2, 1, 5, Blocks.STRIPPED_OAK_LOG.defaultBlockState(), Phase.FINISH);
        b.put(3, 1, 5, Blocks.STRIPPED_OAK_LOG.defaultBlockState(), Phase.FINISH);
        b.put(7, 1, 5, Blocks.STRIPPED_OAK_LOG.defaultBlockState(), Phase.FINISH);
        b.put(8, 1, 5, Blocks.STRIPPED_OAK_LOG.defaultBlockState(), Phase.FINISH);
        b.put(2, 1, 2, Blocks.LANTERN.defaultBlockState(), Phase.FINISH);
        b.put(8, 1, 2, Blocks.LANTERN.defaultBlockState(), Phase.FINISH);
        b.put(2, 1, 7, Blocks.LANTERN.defaultBlockState(), Phase.FINISH);
        b.put(8, 1, 7, Blocks.LANTERN.defaultBlockState(), Phase.FINISH);
        return b.build();
    }

    private static List<Placement> farm(BlockPos o) {
        BlueprintBuilder b = new BlueprintBuilder(o);
        for (int x = 0; x < 13; x++) {
            for (int z = 0; z < 11; z++) {
                boolean path = x == 0 || x == 12 || z == 0 || z == 10 || x == 6;
                if (path) b.put(x, 0, z, Blocks.DIRT_PATH.defaultBlockState(), Phase.FLOOR);
                else b.put(x, 0, z, Blocks.FARMLAND.defaultBlockState(), Phase.FLOOR);
            }
        }
        for (int z = 2; z <= 8; z += 3) {
            b.put(3, 0, z, Blocks.WATER.defaultBlockState(), Phase.FLOOR);
            b.put(9, 0, z, Blocks.WATER.defaultBlockState(), Phase.FLOOR);
        }
        for (int x = 0; x < 13; x++) {
            if (x < 5 || x > 7) {
                b.put(x, 1, 0, Blocks.OAK_FENCE.defaultBlockState(), Phase.FRAME_AND_WALLS);
                b.put(x, 1, 10, Blocks.OAK_FENCE.defaultBlockState(), Phase.FRAME_AND_WALLS);
            }
        }
        for (int z = 1; z < 10; z++) {
            b.put(0, 1, z, Blocks.OAK_FENCE.defaultBlockState(), Phase.FRAME_AND_WALLS);
            b.put(12, 1, z, Blocks.OAK_FENCE.defaultBlockState(), Phase.FRAME_AND_WALLS);
        }
        int[][] lamps = new int[][] {{1,1},{11,1},{1,9},{11,9}};
        for (int[] lamp : lamps) {
            b.put(lamp[0], 1, lamp[1], Blocks.STRIPPED_OAK_LOG.defaultBlockState(), Phase.FRAME_AND_WALLS);
            b.put(lamp[0], 2, lamp[1], Blocks.LANTERN.defaultBlockState(), Phase.FINISH);
        }
        for (int x = 1; x < 12; x++) {
            if (x == 6) continue;
            for (int z = 1; z < 10; z++) {
                if ((x == 3 || x == 9) && (z == 2 || z == 5 || z == 8)) continue;
                b.put(x, 1, z, Blocks.WHEAT.defaultBlockState(), Phase.FINISH);
            }
        }
        b.put(6, 1, 2, Blocks.BARREL.defaultBlockState(), Phase.FINISH);
        b.put(6, 1, 3, Blocks.COMPOSTER.defaultBlockState(), Phase.FINISH);
        return b.build();
    }

    private static List<Placement> quarry(BlockPos o) {
        BlueprintBuilder b = new BlueprintBuilder(o);
        for (int x = 0; x < 11; x++) {
            for (int z = 0; z < 11; z++) {
                if (x <= 1 || x >= 9 || z <= 1 || z >= 9) b.put(x, 0, z, Blocks.COBBLESTONE.defaultBlockState(), Phase.FLOOR);
                else b.put(x, 0, z, Blocks.STONE.defaultBlockState(), Phase.FLOOR);
            }
        }
        int[][] posts = new int[][] {{1,1},{9,1},{1,9},{9,9},{5,1},{5,9}};
        for (int[] post : posts) {
            for (int y = 1; y <= 4; y++) b.put(post[0], y, post[1], Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), Phase.FRAME_AND_WALLS);
        }
        for (int x = 1; x <= 9; x++) {
            b.put(x, 4, 1, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), Phase.FRAME_AND_WALLS);
            b.put(x, 4, 9, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), Phase.FRAME_AND_WALLS);
        }
        for (int z = 0; z <= 3; z++) {
            for (int x = 0; x < 11; x++) b.put(x, 5, z, Blocks.SPRUCE_SLAB.defaultBlockState(), Phase.ROOF);
        }
        for (int z = 7; z <= 10; z++) {
            for (int x = 0; x < 11; x++) b.put(x, 5, z, Blocks.SPRUCE_SLAB.defaultBlockState(), Phase.ROOF);
        }
        b.put(1, 1, 1, Blocks.LANTERN.defaultBlockState(), Phase.FINISH);
        b.put(9, 1, 1, Blocks.LANTERN.defaultBlockState(), Phase.FINISH);
        b.put(1, 1, 9, Blocks.LANTERN.defaultBlockState(), Phase.FINISH);
        b.put(9, 1, 9, Blocks.LANTERN.defaultBlockState(), Phase.FINISH);
        b.put(5, 1, 2, Blocks.BARREL.defaultBlockState(), Phase.FINISH);
        b.put(4, 1, 2, Blocks.STONECUTTER.defaultBlockState(), Phase.FINISH);
        return b.build();
    }

    private static List<Placement> mine(BlockPos o) {
        BlueprintBuilder b = new BlueprintBuilder(o);
        for (int x = 0; x < 11; x++) {
            for (int z = 0; z < 11; z++) b.put(x, 0, z, Blocks.COBBLESTONE.defaultBlockState(), Phase.FLOOR);
        }
        int[][] posts = new int[][] {{1,1},{9,1},{1,9},{9,9},{3,3},{7,3},{3,9},{7,9}};
        for (int[] post : posts) {
            for (int y = 1; y <= 5; y++) b.put(post[0], y, post[1], Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(), Phase.FRAME_AND_WALLS);
        }
        for (int x = 1; x <= 9; x++) {
            b.put(x, 5, 1, Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(), Phase.FRAME_AND_WALLS);
            b.put(x, 5, 9, Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(), Phase.FRAME_AND_WALLS);
        }
        for (int z = 1; z <= 9; z++) {
            b.put(1, 5, z, Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(), Phase.FRAME_AND_WALLS);
            b.put(9, 5, z, Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(), Phase.FRAME_AND_WALLS);
        }
        BlockState leftRoof = Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
        BlockState rightRoof = Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST);
        for (int z = 0; z <= 10; z++) {
            for (int layer = 0; layer < 6; layer++) {
                b.put(layer, 5 + layer, z, leftRoof, Phase.ROOF);
                b.put(10 - layer, 5 + layer, z, rightRoof, Phase.ROOF);
            }
        }
        b.put(2, 1, 2, Blocks.LANTERN.defaultBlockState(), Phase.FINISH);
        b.put(8, 1, 2, Blocks.LANTERN.defaultBlockState(), Phase.FINISH);
        b.put(2, 1, 8, Blocks.LANTERN.defaultBlockState(), Phase.FINISH);
        b.put(8, 1, 8, Blocks.LANTERN.defaultBlockState(), Phase.FINISH);
        b.put(5, 1, 2, Blocks.BARREL.defaultBlockState(), Phase.FINISH);
        b.put(4, 1, 2, Blocks.CRAFTING_TABLE.defaultBlockState(), Phase.FINISH);
        b.put(6, 1, 2, Blocks.FURNACE.defaultBlockState(), Phase.FINISH);
        return b.build();
    }

    private static List<Placement> warehouse(BlockPos o) {
        BlueprintBuilder b = new BlueprintBuilder(o);
        for (int x = 0; x < 11; x++) {
            for (int z = 0; z < 9; z++) b.put(x, 0, z, Blocks.SPRUCE_PLANKS.defaultBlockState(), Phase.FLOOR);
        }

        for (int y = 1; y <= 4; y++) {
            for (int x = 0; x < 11; x++) {
                if (!isWarehouseDoorOpening(x, y, 8)) {
                    b.put(x, y, 8, warehouseWallState(x, y, 8), Phase.FRAME_AND_WALLS);
                }
                b.put(x, y, 0, warehouseWallState(x, y, 0), Phase.FRAME_AND_WALLS);
            }
            for (int z = 1; z < 8; z++) {
                b.put(0, y, z, warehouseWallState(0, y, z), Phase.FRAME_AND_WALLS);
                b.put(10, y, z, warehouseWallState(10, y, z), Phase.FRAME_AND_WALLS);
            }
        }

        int[][] posts = new int[][] {{0,0},{5,0},{10,0},{0,8},{5,8},{10,8}};
        for (int[] post : posts) {
            for (int y = 1; y <= 4; y++) b.put(post[0], y, post[1], Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), Phase.FRAME_AND_WALLS);
        }
        for (int x = 0; x < 11; x++) {
            b.put(x, 4, 0, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), Phase.FRAME_AND_WALLS);
            b.put(x, 4, 8, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), Phase.FRAME_AND_WALLS);
        }
        for (int layer = 0; layer < 5; layer++) {
            int y = 5 + layer;
            for (int x = 1 + layer; x <= 9 - layer; x++) {
                b.put(x, y, 0, Blocks.OAK_PLANKS.defaultBlockState(), Phase.FRAME_AND_WALLS);
                b.put(x, y, 8, Blocks.OAK_PLANKS.defaultBlockState(), Phase.FRAME_AND_WALLS);
            }
        }
        b.put(5, 9, 0, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), Phase.FRAME_AND_WALLS);
        b.put(5, 9, 8, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), Phase.FRAME_AND_WALLS);

        BlockState leftRoof = Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
        BlockState rightRoof = Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST);
        for (int z = -1; z <= 9; z++) {
            for (int layer = 0; layer < 6; layer++) {
                b.put(-1 + layer, 4 + layer, z, leftRoof, Phase.ROOF);
                b.put(11 - layer, 4 + layer, z, rightRoof, Phase.ROOF);
            }
            b.put(5, 10, z, Blocks.SPRUCE_SLAB.defaultBlockState(), Phase.ROOF);
        }

        BlockState lowerDoor = Blocks.SPRUCE_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.SOUTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        b.put(5, 1, 8, lowerDoor, Phase.FINISH);
        b.put(5, 2, 8, lowerDoor.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), Phase.FINISH);

        // Storage positions are shared with SettlementStorageService. Keeping one source of truth
        // prevents invisible inventory slots or a barrel that the HUD counts but the player cannot open.
        for (BlockPos storage : WarehouseLayout.storagePositions(o)) {
            b.putAbsolute(storage, Blocks.BARREL.defaultBlockState(), Phase.FINISH);
        }
        b.put(1, 1, 1, Blocks.LANTERN.defaultBlockState(), Phase.FINISH);
        b.put(9, 1, 1, Blocks.LANTERN.defaultBlockState(), Phase.FINISH);
        b.put(1, 1, 7, Blocks.LANTERN.defaultBlockState(), Phase.FINISH);
        b.put(9, 1, 7, Blocks.LANTERN.defaultBlockState(), Phase.FINISH);
        b.put(5, 3, 2, Blocks.LANTERN.defaultBlockState(), Phase.FINISH);
        b.put(5, 3, 6, Blocks.LANTERN.defaultBlockState(), Phase.FINISH);
        b.put(5, 1, 4, Blocks.CRAFTING_TABLE.defaultBlockState(), Phase.FINISH);
        return b.build();
    }

    private static boolean isWarehouseDoorOpening(int x, int y, int z) {
        return z == 8 && x == 5 && (y == 1 || y == 2);
    }

    private static BlockState warehouseWallState(int x, int y, int z) {
        boolean frontBackWindow = (z == 0 || z == 8) && (x == 2 || x == 8) && (y == 2 || y == 3);
        boolean sideWindow = (x == 0 || x == 10) && (z == 2 || z == 6) && (y == 2 || y == 3);
        return frontBackWindow || sideWindow ? Blocks.GLASS.defaultBlockState() : Blocks.OAK_PLANKS.defaultBlockState();
    }

    private static final class BlueprintBuilder {
        private final BlockPos origin;
        private final Map<BlockPos, Placement> placements = new LinkedHashMap<>();

        private BlueprintBuilder(BlockPos origin) { this.origin = origin; }

        private void put(int x, int y, int z, BlockState state, Phase phase) {
            putAbsolute(origin.offset(x, y, z), state, phase);
        }

        private void putAbsolute(BlockPos absolute, BlockState state, Phase phase) {
            placements.remove(absolute);
            placements.put(absolute, new Placement(absolute, state, phase));
        }

        private List<Placement> build() { return new ArrayList<>(placements.values()); }
    }
}
