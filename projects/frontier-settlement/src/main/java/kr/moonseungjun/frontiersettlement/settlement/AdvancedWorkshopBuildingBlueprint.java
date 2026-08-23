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

/** Physical 15x11 late-game smithing/enchanting hall. */
public final class AdvancedWorkshopBuildingBlueprint {
    private AdvancedWorkshopBuildingBlueprint() {}

    public static List<BuildingBlueprints.Placement> create(BlockPos origin) {
        Builder b = new Builder(origin);
        for (int x = 0; x < 15; x++) {
            for (int z = 0; z < 11; z++) {
                BlockState floor = (x == 0 || x == 14 || z == 0 || z == 10)
                        ? Blocks.STONE_BRICKS.defaultBlockState()
                        : Blocks.SPRUCE_PLANKS.defaultBlockState();
                b.put(x, 0, z, floor, BuildingBlueprints.Phase.FLOOR);
            }
        }

        for (int y = 1; y <= 5; y++) {
            for (int x = 0; x < 15; x++) {
                if (!doorOpening(x, y, 10)) {
                    b.put(x, y, 10, wallState(x, y, 10), BuildingBlueprints.Phase.FRAME_AND_WALLS);
                }
                b.put(x, y, 0, wallState(x, y, 0), BuildingBlueprints.Phase.FRAME_AND_WALLS);
            }
            for (int z = 1; z < 10; z++) {
                b.put(0, y, z, wallState(0, y, z), BuildingBlueprints.Phase.FRAME_AND_WALLS);
                b.put(14, y, z, wallState(14, y, z), BuildingBlueprints.Phase.FRAME_AND_WALLS);
            }
        }

        int[][] posts = {{0,0},{7,0},{14,0},{0,10},{7,10},{14,10},{3,3},{11,3},{3,7},{11,7}};
        for (int[] p : posts) {
            for (int y = 1; y <= 6; y++) {
                b.put(p[0], y, p[1], Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
            }
        }
        for (int x = 0; x < 15; x++) {
            b.put(x, 6, 0, Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
            b.put(x, 6, 10, Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
        }

        BlockState leftRoof = Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
        BlockState rightRoof = Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST);
        for (int z = -1; z <= 11; z++) {
            for (int layer = 0; layer < 8; layer++) {
                b.put(-1 + layer, 6 + layer, z, leftRoof, BuildingBlueprints.Phase.ROOF);
                b.put(15 - layer, 6 + layer, z, rightRoof, BuildingBlueprints.Phase.ROOF);
            }
            b.put(7, 14, z, Blocks.DARK_OAK_SLAB.defaultBlockState(), BuildingBlueprints.Phase.ROOF);
        }

        BlockState lowerDoor = Blocks.SPRUCE_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.SOUTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        b.put(7, 1, 10, lowerDoor, BuildingBlueprints.Phase.FINISH);
        b.put(7, 2, 10, lowerDoor.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), BuildingBlueprints.Phase.FINISH);

        b.put(7, 1, 3, Blocks.BARREL.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(5, 1, 3, Blocks.SMITHING_TABLE.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(9, 1, 3, Blocks.ANVIL.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(7, 1, 5, Blocks.ENCHANTING_TABLE.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(4, 1, 6, Blocks.GRINDSTONE.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(10, 1, 6, Blocks.BLAST_FURNACE.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(2, 1, 8, Blocks.CRAFTING_TABLE.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(12, 1, 8, Blocks.STONECUTTER.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        for (int x : new int[] {2, 12}) for (int z : new int[] {2, 8}) {
            b.put(x, 2, z, Blocks.LANTERN.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        }
        return b.build();
    }

    private static boolean doorOpening(int x, int y, int z) {
        return z == 10 && x == 7 && (y == 1 || y == 2);
    }

    private static BlockState wallState(int x, int y, int z) {
        boolean window = y >= 2 && y <= 3 && ((z == 0 || z == 10) && (x == 3 || x == 11)
                || (x == 0 || x == 14) && (z == 3 || z == 7));
        return window ? Blocks.GLASS_PANE.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState();
    }

    private static final class Builder {
        private final BlockPos origin;
        private final Map<BlockPos, BuildingBlueprints.Placement> placements = new LinkedHashMap<>();
        private Builder(BlockPos origin) { this.origin = origin; }
        private void put(int x, int y, int z, BlockState state, BuildingBlueprints.Phase phase) {
            BlockPos pos = origin.offset(x, y, z);
            placements.put(pos, new BuildingBlueprints.Placement(pos, state, phase));
        }
        private List<BuildingBlueprints.Placement> build() { return new ArrayList<>(placements.values()); }
    }
}
