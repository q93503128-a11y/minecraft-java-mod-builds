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

/** Compact production workshop with one deliberate player-facing maintenance barrel. */
public final class WorkshopBuildingBlueprint {
    private WorkshopBuildingBlueprint() {}

    public static List<BuildingBlueprints.Placement> create(BlockPos origin) {
        Builder b = new Builder(origin);

        for (int x = 0; x < 11; x++) {
            for (int z = 0; z < 9; z++) {
                BlockState floor = (x == 0 || x == 10 || z == 0 || z == 8)
                        ? Blocks.STONE_BRICKS.defaultBlockState()
                        : Blocks.SPRUCE_PLANKS.defaultBlockState();
                b.put(x, 0, z, floor, BuildingBlueprints.Phase.FLOOR);
            }
        }

        for (int y = 1; y <= 4; y++) {
            for (int x = 0; x < 11; x++) {
                if (!(x == 5 && (y == 1 || y == 2))) {
                    b.put(x, y, 8, wallState(x, y), BuildingBlueprints.Phase.FRAME_AND_WALLS);
                }
                b.put(x, y, 0, wallState(x, y), BuildingBlueprints.Phase.FRAME_AND_WALLS);
            }
            for (int z = 1; z < 8; z++) {
                b.put(0, y, z, wallState(z, y), BuildingBlueprints.Phase.FRAME_AND_WALLS);
                b.put(10, y, z, wallState(z, y), BuildingBlueprints.Phase.FRAME_AND_WALLS);
            }
        }

        int[][] posts = {{0,0},{5,0},{10,0},{0,8},{5,8},{10,8}};
        for (int[] p : posts) {
            for (int y = 1; y <= 4; y++) {
                b.put(p[0], y, p[1], Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(),
                        BuildingBlueprints.Phase.FRAME_AND_WALLS);
            }
        }
        for (int x = 0; x < 11; x++) {
            b.put(x, 4, 0, Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
            b.put(x, 4, 8, Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
        }

        BlockState left = Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
        BlockState right = Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST);
        for (int z = -1; z <= 9; z++) {
            for (int layer = 0; layer < 6; layer++) {
                b.put(-1 + layer, 4 + layer, z, left, BuildingBlueprints.Phase.ROOF);
                b.put(11 - layer, 4 + layer, z, right, BuildingBlueprints.Phase.ROOF);
            }
            b.put(5, 10, z, Blocks.DARK_OAK_SLAB.defaultBlockState(), BuildingBlueprints.Phase.ROOF);
        }

        BlockState lowerDoor = Blocks.DARK_OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.SOUTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        b.put(5, 1, 8, lowerDoor, BuildingBlueprints.Phase.FINISH);
        b.put(5, 2, 8, lowerDoor.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), BuildingBlueprints.Phase.FINISH);

        b.putAbsolute(WorkshopLayout.serviceCrate(origin), Blocks.BARREL.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(3, 1, 3, Blocks.CRAFTING_TABLE.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(4, 1, 3, Blocks.GRINDSTONE.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(6, 1, 3, Blocks.SMITHING_TABLE.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(7, 1, 3, Blocks.ANVIL.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(8, 1, 3, Blocks.BLAST_FURNACE.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        int[][] lamps = {{1,1},{9,1},{1,7},{9,7}};
        for (int[] p : lamps) b.put(p[0], 1, p[1], Blocks.LANTERN.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        return b.build();
    }

    private static BlockState wallState(int coordinate, int y) {
        if ((coordinate == 2 || coordinate == 8) && (y == 2 || y == 3)) {
            return Blocks.GLASS.defaultBlockState();
        }
        return y <= 2 ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.SPRUCE_PLANKS.defaultBlockState();
    }

    private static final class Builder {
        private final BlockPos origin;
        private final Map<BlockPos, BuildingBlueprints.Placement> placements = new LinkedHashMap<>();

        private Builder(BlockPos origin) { this.origin = origin; }

        private void put(int x, int y, int z, BlockState state, BuildingBlueprints.Phase phase) {
            putAbsolute(origin.offset(x, y, z), state, phase);
        }

        private void putAbsolute(BlockPos pos, BlockState state, BuildingBlueprints.Phase phase) {
            placements.remove(pos);
            placements.put(pos, new BuildingBlueprints.Placement(pos, state, phase));
        }

        private List<BuildingBlueprints.Placement> build() {
            return new ArrayList<>(placements.values());
        }
    }
}
