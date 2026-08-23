package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tall but compact watchtower. The existing construction-scaffold system builds it physically. */
public final class WatchtowerBuildingBlueprint {
    private WatchtowerBuildingBlueprint() {}

    public static List<BuildingBlueprints.Placement> create(BlockPos origin) {
        Builder b = new Builder(origin);

        for (int x = 0; x < 7; x++) {
            for (int z = 0; z < 7; z++) {
                BlockState floor = (x == 0 || x == 6 || z == 0 || z == 6)
                        ? Blocks.STONE_BRICKS.defaultBlockState()
                        : Blocks.COBBLESTONE.defaultBlockState();
                b.put(x, 0, z, floor, BuildingBlueprints.Phase.FLOOR);
            }
        }

        // Low fortified base, open toward the front center.
        for (int y = 1; y <= 2; y++) {
            for (int x = 0; x < 7; x++) {
                b.put(x, y, 0, Blocks.STONE_BRICKS.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
                if (!(x == 3 && (y == 1 || y == 2))) {
                    b.put(x, y, 6, Blocks.STONE_BRICKS.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
                }
            }
            for (int z = 1; z < 6; z++) {
                b.put(0, y, z, Blocks.STONE_BRICKS.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
                b.put(6, y, z, Blocks.STONE_BRICKS.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
            }
        }

        int[][] supports = {{1,1},{5,1},{1,5},{5,5}};
        for (int[] support : supports) {
            for (int y = 1; y <= 9; y++) {
                b.put(support[0], y, support[1], Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(),
                        BuildingBlueprints.Phase.FRAME_AND_WALLS);
            }
        }

        // Cross braces make the height readable instead of four floating pillars.
        for (int y : new int[] {4, 7}) {
            for (int x = 1; x <= 5; x++) {
                b.put(x, y, 1, Blocks.SPRUCE_PLANKS.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
                b.put(x, y, 5, Blocks.SPRUCE_PLANKS.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
            }
            for (int z = 2; z <= 4; z++) {
                b.put(1, y, z, Blocks.SPRUCE_PLANKS.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
                b.put(5, y, z, Blocks.SPRUCE_PLANKS.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
            }
        }

        // Central spine + ladder gives the player a real climb to the lookout deck.
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
        for (int y = 1; y <= 9; y++) {
            b.put(3, y, 2, Blocks.STRIPPED_OAK_LOG.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
            b.put(3, y, 3, ladder, BuildingBlueprints.Phase.FINISH);
        }

        // Observation deck; ladder opening is intentionally left clear at (3, 9, 3).
        for (int x = 0; x < 7; x++) {
            for (int z = 0; z < 7; z++) {
                if (x == 3 && z == 3) continue;
                b.put(x, 9, z, Blocks.SPRUCE_PLANKS.defaultBlockState(), BuildingBlueprints.Phase.ROOF);
            }
        }
        for (int x = 0; x < 7; x++) {
            b.put(x, 10, 0, Blocks.SPRUCE_FENCE.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
            b.put(x, 10, 6, Blocks.SPRUCE_FENCE.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        }
        for (int z = 1; z < 6; z++) {
            b.put(0, 10, z, Blocks.SPRUCE_FENCE.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
            b.put(6, 10, z, Blocks.SPRUCE_FENCE.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        }

        int[][] roofPosts = {{0,0},{6,0},{0,6},{6,6}};
        for (int[] post : roofPosts) {
            for (int y = 10; y <= 12; y++) {
                b.put(post[0], y, post[1], Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(), BuildingBlueprints.Phase.ROOF);
            }
        }
        for (int x = -1; x <= 7; x++) {
            for (int z = -1; z <= 7; z++) {
                b.put(x, 13, z, Blocks.DARK_OAK_SLAB.defaultBlockState(), BuildingBlueprints.Phase.ROOF);
            }
        }

        b.put(1, 10, 1, Blocks.LANTERN.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(5, 10, 1, Blocks.LANTERN.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(1, 10, 5, Blocks.LANTERN.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(5, 10, 5, Blocks.LANTERN.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(3, 10, 1, Blocks.BELL.defaultBlockState(), BuildingBlueprints.Phase.FINISH);

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
