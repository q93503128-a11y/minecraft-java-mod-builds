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

/** Physical frontier-town barracks: quarters, armory and a small drill yard. */
public final class BarracksBuildingBlueprint {
    private BarracksBuildingBlueprint() {}

    public static List<BuildingBlueprints.Placement> create(BlockPos o) {
        Builder b = new Builder(o);

        for (int x = 0; x < 15; x++) {
            for (int z = 0; z < 11; z++) {
                BlockState floor = (x == 0 || x == 14 || z == 0 || z == 10)
                        ? Blocks.STONE_BRICKS.defaultBlockState()
                        : (z <= 6 ? Blocks.SPRUCE_PLANKS.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState());
                b.put(x, 0, z, floor, BuildingBlueprints.Phase.FLOOR);
            }
        }

        // Enclosed quarters/armory occupy the front seven blocks; rear remains a drill yard.
        for (int y = 1; y <= 4; y++) {
            for (int x = 0; x < 15; x++) {
                if (!(zDoor(x, y))) {
                    b.put(x, y, 0, wallState(x, y), BuildingBlueprints.Phase.FRAME_AND_WALLS);
                }
                if (x != 7 || y > 2) {
                    b.put(x, y, 6, wallState(x, y), BuildingBlueprints.Phase.FRAME_AND_WALLS);
                }
            }
            for (int z = 1; z < 6; z++) {
                b.put(0, y, z, wallState(z, y), BuildingBlueprints.Phase.FRAME_AND_WALLS);
                b.put(14, y, z, wallState(z, y), BuildingBlueprints.Phase.FRAME_AND_WALLS);
            }
        }

        int[][] posts = {{0,0},{7,0},{14,0},{0,6},{7,6},{14,6}};
        for (int[] p : posts) {
            for (int y = 1; y <= 5; y++) {
                b.put(p[0], y, p[1], Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
            }
        }

        BlockState left = Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
        BlockState right = Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST);
        for (int z = -1; z <= 7; z++) {
            for (int layer = 0; layer < 8; layer++) {
                int y = 4 + Math.min(layer, 4);
                if (layer <= 4) {
                    b.put(-1 + layer, y, z, left, BuildingBlueprints.Phase.ROOF);
                    b.put(15 - layer, y, z, right, BuildingBlueprints.Phase.ROOF);
                }
            }
            for (int x = 4; x <= 10; x++) {
                b.put(x, 8, z, Blocks.DARK_OAK_SLAB.defaultBlockState(), BuildingBlueprints.Phase.ROOF);
            }
        }

        BlockState lowerDoor = Blocks.IRON_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.SOUTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        b.put(7, 1, 0, lowerDoor, BuildingBlueprints.Phase.FINISH);
        b.put(7, 2, 0, lowerDoor.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), BuildingBlueprints.Phase.FINISH);
        b.put(6, 1, 1, Blocks.STONE_BUTTON.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(7, 1, 6, Blocks.SPRUCE_FENCE_GATE.defaultBlockState(), BuildingBlueprints.Phase.FINISH);

        // Three visible bunk stations match the three military housing slots.
        int[] bunkX = {2, 6, 10};
        for (int x : bunkX) {
            b.put(x, 1, 2, Blocks.SPRUCE_SLAB.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
            b.put(x + 1, 1, 2, Blocks.SPRUCE_SLAB.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
            b.put(x, 3, 2, Blocks.SPRUCE_SLAB.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
            b.put(x + 1, 3, 2, Blocks.SPRUCE_SLAB.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        }

        b.put(2, 1, 4, Blocks.BARREL.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(3, 1, 4, Blocks.BARREL.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(5, 1, 4, Blocks.SMITHING_TABLE.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(6, 1, 4, Blocks.GRINDSTONE.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(9, 1, 4, Blocks.CRAFTING_TABLE.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(10, 1, 4, Blocks.BLAST_FURNACE.defaultBlockState(), BuildingBlueprints.Phase.FINISH);

        // Rear drill yard and muster points.
        for (int x = 0; x < 15; x++) {
            if (x == 6 || x == 7 || x == 8) continue;
            b.put(x, 1, 10, Blocks.STONE_BRICK_WALL.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
        }
        for (int z = 7; z < 10; z++) {
            b.put(0, 1, z, Blocks.STONE_BRICK_WALL.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
            b.put(14, 1, z, Blocks.STONE_BRICK_WALL.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
        }
        b.put(3, 1, 9, Blocks.TARGET.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(11, 1, 9, Blocks.TARGET.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(7, 1, 9, Blocks.BELL.defaultBlockState(), BuildingBlueprints.Phase.FINISH);

        int[][] lamps = {{1,1},{13,1},{1,5},{13,5},{1,9},{13,9},{7,5}};
        for (int[] p : lamps) b.put(p[0], 2, p[1], Blocks.LANTERN.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        return b.build();
    }

    private static boolean zDoor(int x, int y) {
        return x == 7 && (y == 1 || y == 2);
    }

    private static BlockState wallState(int coordinate, int y) {
        if ((coordinate == 3 || coordinate == 11) && (y == 2 || y == 3)) {
            return Blocks.IRON_BARS.defaultBlockState();
        }
        return y <= 2 ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.SPRUCE_PLANKS.defaultBlockState();
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
