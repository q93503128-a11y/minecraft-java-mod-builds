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

public final class ConstructionOfficeBuildingBlueprint {
    private ConstructionOfficeBuildingBlueprint() {}

    public static List<BuildingBlueprints.Placement> create(BlockPos origin) {
        Builder b = new Builder(origin);
        for (int x = 0; x < 13; x++) {
            for (int z = 0; z < 9; z++) {
                BlockState floor = (x == 0 || x == 12 || z == 0 || z == 8)
                        ? Blocks.STONE_BRICKS.defaultBlockState()
                        : Blocks.SPRUCE_PLANKS.defaultBlockState();
                b.put(x, 0, z, floor, BuildingBlueprints.Phase.FLOOR);
            }
        }

        for (int y = 1; y <= 4; y++) {
            for (int x = 0; x < 13; x++) {
                if (!(x == 6 && (y == 1 || y == 2))) {
                    b.put(x, y, 8, wall(x, y), BuildingBlueprints.Phase.FRAME_AND_WALLS);
                }
                b.put(x, y, 0, wall(x, y), BuildingBlueprints.Phase.FRAME_AND_WALLS);
            }
            for (int z = 1; z < 8; z++) {
                b.put(0, y, z, wall(z, y), BuildingBlueprints.Phase.FRAME_AND_WALLS);
                b.put(12, y, z, wall(z, y), BuildingBlueprints.Phase.FRAME_AND_WALLS);
            }
        }

        int[][] posts = {{0,0},{6,0},{12,0},{0,8},{6,8},{12,8}};
        for (int[] post : posts) {
            for (int y = 1; y <= 5; y++) {
                b.put(post[0], y, post[1], Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
            }
        }

        BlockState westRoof = Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
        BlockState eastRoof = Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST);
        for (int z = -1; z <= 9; z++) {
            for (int layer = 0; layer <= 6; layer++) {
                int y = 5 + layer;
                b.put(-1 + layer, y, z, westRoof, BuildingBlueprints.Phase.ROOF);
                b.put(13 - layer, y, z, eastRoof, BuildingBlueprints.Phase.ROOF);
            }
            b.put(6, 12, z, Blocks.SPRUCE_SLAB.defaultBlockState(), BuildingBlueprints.Phase.ROOF);
        }

        int[][] barrels = {{2,1,2},{4,1,2},{8,1,2},{10,1,2}};
        for (int[] slot : barrels) b.put(slot[0], slot[1], slot[2], Blocks.BARREL.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(6, 1, 2, Blocks.CRAFTING_TABLE.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(5, 1, 4, Blocks.STONECUTTER.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(7, 1, 4, Blocks.SCAFFOLDING.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(2, 1, 6, Blocks.CHEST.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(10, 1, 6, Blocks.CARTOGRAPHY_TABLE.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(3, 2, 6, Blocks.LANTERN.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(9, 2, 6, Blocks.LANTERN.defaultBlockState(), BuildingBlueprints.Phase.FINISH);

        BlockState lowerDoor = Blocks.SPRUCE_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.SOUTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        b.put(6, 1, 8, lowerDoor, BuildingBlueprints.Phase.FINISH);
        b.put(6, 2, 8, lowerDoor.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), BuildingBlueprints.Phase.FINISH);
        return b.build();
    }

    private static BlockState wall(int index, int y) {
        if ((index == 2 || index == 4 || index == 8 || index == 10) && (y == 2 || y == 3)) {
            return Blocks.GLASS_PANE.defaultBlockState();
        }
        return Blocks.OAK_PLANKS.defaultBlockState();
    }

    private static final class Builder {
        private final BlockPos origin;
        private final Map<BlockPos, BuildingBlueprints.Placement> placements = new LinkedHashMap<>();
        private Builder(BlockPos origin) { this.origin = origin; }
        private void put(int x, int y, int z, BlockState state, BuildingBlueprints.Phase phase) {
            BlockPos pos = origin.offset(x, y, z);
            placements.remove(pos);
            placements.put(pos, new BuildingBlueprints.Placement(pos, state, phase));
        }
        private List<BuildingBlueprints.Placement> build() { return new ArrayList<>(placements.values()); }
    }
}
