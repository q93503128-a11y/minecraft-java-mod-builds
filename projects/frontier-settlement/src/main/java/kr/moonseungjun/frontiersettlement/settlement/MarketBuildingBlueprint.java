package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Open civic market with four covered stalls and one deliberate player-facing trade crate. */
public final class MarketBuildingBlueprint {
    private MarketBuildingBlueprint() {}

    public static List<BuildingBlueprints.Placement> create(BlockPos origin) {
        Builder b = new Builder(origin);

        for (int x = 0; x < 11; x++) {
            for (int z = 0; z < 11; z++) {
                boolean edge = x == 0 || x == 10 || z == 0 || z == 10;
                boolean cross = x == 5 || z == 5;
                BlockState floor = edge || cross
                        ? Blocks.STONE_BRICKS.defaultBlockState()
                        : Blocks.SPRUCE_PLANKS.defaultBlockState();
                b.put(x, 0, z, floor, BuildingBlueprints.Phase.FLOOR);
            }
        }

        int[][] pavilionPosts = {{3,3},{7,3},{3,7},{7,7}};
        for (int[] p : pavilionPosts) {
            for (int y = 1; y <= 4; y++) {
                b.put(p[0], y, p[1], Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(),
                        BuildingBlueprints.Phase.FRAME_AND_WALLS);
            }
        }
        for (int x = 3; x <= 7; x++) {
            b.put(x, 4, 3, Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
            b.put(x, 4, 7, Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
        }
        for (int z = 3; z <= 7; z++) {
            b.put(3, 4, z, Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
            b.put(7, 4, z, Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
        }
        for (int x = 2; x <= 8; x++) {
            for (int z = 2; z <= 8; z++) {
                b.put(x, 5, z, Blocks.SPRUCE_SLAB.defaultBlockState(), BuildingBlueprints.Phase.ROOF);
            }
        }

        int[][] stallPosts = {{1,1},{3,1},{7,1},{9,1},{1,9},{3,9},{7,9},{9,9}};
        for (int[] p : stallPosts) {
            for (int y = 1; y <= 3; y++) {
                b.put(p[0], y, p[1], Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(),
                        BuildingBlueprints.Phase.FRAME_AND_WALLS);
            }
        }
        for (int x = 1; x <= 3; x++) {
            b.put(x, 3, 1, Blocks.SPRUCE_SLAB.defaultBlockState(), BuildingBlueprints.Phase.ROOF);
            b.put(x, 3, 9, Blocks.SPRUCE_SLAB.defaultBlockState(), BuildingBlueprints.Phase.ROOF);
        }
        for (int x = 7; x <= 9; x++) {
            b.put(x, 3, 1, Blocks.SPRUCE_SLAB.defaultBlockState(), BuildingBlueprints.Phase.ROOF);
            b.put(x, 3, 9, Blocks.SPRUCE_SLAB.defaultBlockState(), BuildingBlueprints.Phase.ROOF);
        }

        int[][] counters = {{2,1},{8,1},{2,9},{8,9}};
        for (int[] p : counters) {
            b.put(p[0], 1, p[1], Blocks.SPRUCE_SLAB.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        }

        b.putAbsolute(MarketLayout.tradeCrate(origin), Blocks.BARREL.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(5, 1, 3, Blocks.CRAFTING_TABLE.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(5, 1, 7, Blocks.BELL.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        int[][] lamps = {{3,2},{7,2},{3,8},{7,8}};
        for (int[] p : lamps) {
            b.put(p[0], 1, p[1], Blocks.LANTERN.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
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
