package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Roadside freight depot: covered loading platforms, central cart lane and four physical freight barrels. */
public final class CartStationBuildingBlueprint {
    private CartStationBuildingBlueprint() {}

    public static List<BuildingBlueprints.Placement> create(BlockPos origin) {
        Builder b = new Builder(origin);

        for (int x = 0; x < 13; x++) {
            for (int z = 0; z < 9; z++) {
                boolean edge = x == 0 || x == 12 || z == 0 || z == 8;
                boolean lane = x >= 5 && x <= 7;
                BlockState floor = edge || lane
                        ? Blocks.STONE_BRICKS.defaultBlockState()
                        : Blocks.SPRUCE_PLANKS.defaultBlockState();
                b.put(x, 0, z, floor, BuildingBlueprints.Phase.FLOOR);
            }
        }

        int[][] posts = {
                {1,1},{4,1},{8,1},{11,1},
                {1,7},{4,7},{8,7},{11,7}
        };
        for (int[] p : posts) {
            for (int y = 1; y <= 4; y++) {
                b.put(p[0], y, p[1], Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(),
                        BuildingBlueprints.Phase.FRAME_AND_WALLS);
            }
        }

        for (int x = 1; x <= 4; x++) {
            for (int z = 1; z <= 7; z++) {
                b.put(x, 4, z, Blocks.SPRUCE_SLAB.defaultBlockState(), BuildingBlueprints.Phase.ROOF);
            }
        }
        for (int x = 8; x <= 11; x++) {
            for (int z = 1; z <= 7; z++) {
                b.put(x, 4, z, Blocks.SPRUCE_SLAB.defaultBlockState(), BuildingBlueprints.Phase.ROOF);
            }
        }

        // A short north/south loading lane makes the logistics building visually legible without
        // introducing a second moving-cart authority alongside the road-bound transport worker.
        for (int z = 1; z <= 7; z++) {
            b.put(6, 1, z, Blocks.RAIL.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        }

        for (BlockPos freight : CartStationLayout.freightPositions(origin)) {
            b.putAbsolute(freight, Blocks.BARREL.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        }

        b.put(2, 1, 4, Blocks.CRAFTING_TABLE.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(10, 1, 4, Blocks.CHEST.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(6, 1, 0, Blocks.BELL.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        int[][] lamps = {{1,1},{4,1},{8,1},{11,1},{1,7},{4,7},{8,7},{11,7}};
        for (int[] p : lamps) {
            b.put(p[0], 3, p[1], Blocks.LANTERN.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        }
        return b.build();
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
