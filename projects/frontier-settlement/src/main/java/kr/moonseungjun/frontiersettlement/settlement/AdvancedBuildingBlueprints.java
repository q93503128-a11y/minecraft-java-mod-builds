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

public final class AdvancedBuildingBlueprints {
    private AdvancedBuildingBlueprints() {}

    public static List<BuildingBlueprints.Placement> create(BuildingType type, BlockPos origin) {
        return switch (type) {
            case BLACKSMITH -> blacksmith(origin);
            case GUARD_POST -> guardPost(origin);
            default -> throw new IllegalArgumentException("No advanced blueprint for " + type);
        };
    }

    private static List<BuildingBlueprints.Placement> blacksmith(BlockPos o) {
        Builder b = new Builder(o);
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                BlockState floor = (x == 0 || x == 8 || z == 0 || z == 8)
                        ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState();
                b.put(x, 0, z, floor, BuildingBlueprints.Phase.FLOOR);
            }
        }

        for (int y = 1; y <= 4; y++) {
            for (int x = 0; x < 9; x++) {
                if (!(x == 4 && (y == 1 || y == 2))) {
                    b.put(x, y, 8, wallState(x, y), BuildingBlueprints.Phase.FRAME_AND_WALLS);
                }
                b.put(x, y, 0, wallState(x, y), BuildingBlueprints.Phase.FRAME_AND_WALLS);
            }
            for (int z = 1; z < 8; z++) {
                b.put(0, y, z, wallState(z, y), BuildingBlueprints.Phase.FRAME_AND_WALLS);
                b.put(8, y, z, wallState(z, y), BuildingBlueprints.Phase.FRAME_AND_WALLS);
            }
        }

        int[][] posts = {{0,0},{8,0},{0,8},{8,8}};
        for (int[] p : posts) {
            for (int y = 1; y <= 4; y++) {
                b.put(p[0], y, p[1], Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
            }
        }
        for (int x = 0; x < 9; x++) {
            b.put(x, 4, 0, Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
            b.put(x, 4, 8, Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
        }

        BlockState left = Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
        BlockState right = Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST);
        for (int z = -1; z <= 9; z++) {
            for (int layer = 0; layer < 5; layer++) {
                b.put(-1 + layer, 4 + layer, z, left, BuildingBlueprints.Phase.ROOF);
                b.put(9 - layer, 4 + layer, z, right, BuildingBlueprints.Phase.ROOF);
            }
            b.put(4, 9, z, Blocks.DARK_OAK_SLAB.defaultBlockState(), BuildingBlueprints.Phase.ROOF);
        }

        BlockState lowerDoor = Blocks.DARK_OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.SOUTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        b.put(4, 1, 8, lowerDoor, BuildingBlueprints.Phase.FINISH);
        b.put(4, 2, 8, lowerDoor.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), BuildingBlueprints.Phase.FINISH);
        b.put(2, 1, 2, Blocks.ANVIL.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(3, 1, 2, Blocks.SMITHING_TABLE.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(6, 1, 2, Blocks.BLAST_FURNACE.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(6, 1, 3, Blocks.CAULDRON.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        for (int y = 1; y <= 7; y++) {
            b.put(7, y, 1, y <= 4 ? Blocks.BRICKS.defaultBlockState() : Blocks.BRICK_WALL.defaultBlockState(),
                    y <= 4 ? BuildingBlueprints.Phase.FRAME_AND_WALLS : BuildingBlueprints.Phase.ROOF);
        }
        int[][] lamps = {{1,1},{7,1},{1,7},{7,7},{4,2},{4,6}};
        for (int[] p : lamps) b.put(p[0], 1, p[1], Blocks.LANTERN.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        return b.build();
    }

    private static BlockState wallState(int coordinate, int y) {
        if ((coordinate == 2 || coordinate == 6) && (y == 2 || y == 3)) return Blocks.GLASS.defaultBlockState();
        return y <= 2 ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.SPRUCE_PLANKS.defaultBlockState();
    }

    private static List<BuildingBlueprints.Placement> guardPost(BlockPos o) {
        Builder b = new Builder(o);
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                BlockState floor = (x == 0 || x == 8 || z == 0 || z == 8)
                        ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.SPRUCE_PLANKS.defaultBlockState();
                b.put(x, 0, z, floor, BuildingBlueprints.Phase.FLOOR);
            }
        }

        for (int x = 0; x < 9; x++) {
            if (x < 3 || x > 5) {
                b.put(x, 1, 0, Blocks.STONE_BRICK_WALL.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
                b.put(x, 1, 8, Blocks.STONE_BRICK_WALL.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
            }
        }
        for (int z = 1; z < 8; z++) {
            b.put(0, 1, z, Blocks.STONE_BRICK_WALL.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
            b.put(8, 1, z, Blocks.STONE_BRICK_WALL.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
        }

        int[][] posts = {{1,1},{7,1},{1,7},{7,7},{4,1},{4,7}};
        for (int[] p : posts) {
            for (int y = 1; y <= 4; y++) {
                b.put(p[0], y, p[1], Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
            }
        }
        for (int x = 1; x <= 7; x++) {
            b.put(x, 4, 1, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
            b.put(x, 4, 7, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
        }
        for (int z = 1; z <= 7; z++) {
            b.put(1, 4, z, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
            b.put(7, 4, z, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
        }
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                b.put(x, 5, z, Blocks.SPRUCE_SLAB.defaultBlockState(), BuildingBlueprints.Phase.ROOF);
            }
        }
        b.put(4, 1, 4, Blocks.BARREL.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(3, 1, 4, Blocks.CRAFTING_TABLE.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        int[][] lamps = {{1,1},{7,1},{1,7},{7,7},{4,2},{4,6}};
        for (int[] p : lamps) b.put(p[0], 1, p[1], Blocks.LANTERN.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        return b.build();
    }

    private static final class Builder {
        private final BlockPos origin;
        private final Map<BlockPos, BuildingBlueprints.Placement> placements = new LinkedHashMap<>();

        private Builder(BlockPos origin) { this.origin = origin; }

        private void put(int x, int y, int z, BlockState state, BuildingBlueprints.Phase phase) {
            BlockPos absolute = origin.offset(x, y, z);
            placements.remove(absolute);
            placements.put(absolute, new BuildingBlueprints.Placement(absolute, state, phase));
        }

        private List<BuildingBlueprints.Placement> build() {
            return new ArrayList<>(placements.values());
        }
    }
}
