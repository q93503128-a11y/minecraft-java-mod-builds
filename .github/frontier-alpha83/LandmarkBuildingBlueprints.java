package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Alpha.83 late-game physical landmarks. These are ordinary Frontier construction plans:
 * the shared builder pays the BuildingType wood/stone totals through the existing real ItemStack
 * construction transaction. Decorative blocks do not create a second storage or resource authority.
 */
public final class LandmarkBuildingBlueprints {
    private LandmarkBuildingBlueprints() {}

    public static List<BuildingBlueprints.Placement> create(BuildingType type, BlockPos origin) {
        return switch (type) {
            case CIVIC_HALL -> civicHall(origin);
            case TRADE_HALL -> tradeHall(origin);
            case CITADEL -> citadel(origin);
            default -> throw new IllegalArgumentException("Not a landmark building: " + type);
        };
    }

    private static List<BuildingBlueprints.Placement> civicHall(BlockPos o) {
        Builder b = new Builder(o);
        for (int x = 0; x < 15; x++) for (int z = 0; z < 13; z++) {
            BlockState floor = (x == 7 || z == 6)
                    ? Blocks.POLISHED_ANDESITE.defaultBlockState()
                    : Blocks.STONE_BRICKS.defaultBlockState();
            b.put(x, 0, z, floor, BuildingBlueprints.Phase.FLOOR);
        }
        for (int y = 1; y <= 5; y++) {
            for (int x = 0; x < 15; x++) {
                if (!entrance(x, y, 12, 6, 8)) b.put(x, y, 12, civicWall(x, y), BuildingBlueprints.Phase.FRAME_AND_WALLS);
                b.put(x, y, 0, civicWall(x, y), BuildingBlueprints.Phase.FRAME_AND_WALLS);
            }
            for (int z = 1; z < 12; z++) {
                b.put(0, y, z, civicSide(z, y), BuildingBlueprints.Phase.FRAME_AND_WALLS);
                b.put(14, y, z, civicSide(z, y), BuildingBlueprints.Phase.FRAME_AND_WALLS);
            }
        }
        int[][] pillars = {{0,0},{14,0},{0,12},{14,12},{4,0},{10,0},{4,12},{10,12}};
        for (int[] p : pillars) for (int y = 1; y <= 6; y++) {
            b.put(p[0], y, p[1], Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
        }
        for (int x = 1; x < 14; x++) for (int z = 1; z < 12; z++) {
            b.put(x, 6, z, Blocks.STONE_BRICK_SLAB.defaultBlockState(), BuildingBlueprints.Phase.ROOF);
        }
        for (int x = 5; x <= 9; x++) for (int z = 4; z <= 8; z++) {
            b.put(x, 1, z, Blocks.POLISHED_ANDESITE.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        }
        b.put(7, 2, 6, Blocks.LECTERN.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(5, 2, 4, Blocks.LANTERN.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(9, 2, 4, Blocks.LANTERN.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(5, 2, 8, Blocks.LANTERN.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(9, 2, 8, Blocks.LANTERN.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        for (int x = 6; x <= 8; x++) b.put(x, 1, 12, Blocks.POLISHED_ANDESITE.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        return b.build();
    }

    private static BlockState civicWall(int x, int y) {
        if ((x == 2 || x == 7 || x == 12) && (y == 2 || y == 3)) return Blocks.GLASS_PANE.defaultBlockState();
        return Blocks.STONE_BRICKS.defaultBlockState();
    }

    private static BlockState civicSide(int z, int y) {
        if ((z == 3 || z == 6 || z == 9) && (y == 2 || y == 3)) return Blocks.GLASS_PANE.defaultBlockState();
        return Blocks.STONE_BRICKS.defaultBlockState();
    }

    private static List<BuildingBlueprints.Placement> tradeHall(BlockPos o) {
        Builder b = new Builder(o);
        for (int x = 0; x < 15; x++) for (int z = 0; z < 13; z++) {
            BlockState floor = (x == 7 || z == 6)
                    ? Blocks.SMOOTH_STONE.defaultBlockState()
                    : Blocks.SPRUCE_PLANKS.defaultBlockState();
            b.put(x, 0, z, floor, BuildingBlueprints.Phase.FLOOR);
        }
        for (int x : new int[]{0, 4, 10, 14}) for (int z : new int[]{0, 6, 12}) {
            for (int y = 1; y <= 5; y++) {
                b.put(x, y, z, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
            }
        }
        for (int x = 0; x < 15; x++) {
            b.put(x, 5, 0, Blocks.DARK_OAK_PLANKS.defaultBlockState(), BuildingBlueprints.Phase.ROOF);
            b.put(x, 5, 12, Blocks.DARK_OAK_PLANKS.defaultBlockState(), BuildingBlueprints.Phase.ROOF);
        }
        for (int z = 0; z < 13; z++) {
            b.put(0, 5, z, Blocks.DARK_OAK_PLANKS.defaultBlockState(), BuildingBlueprints.Phase.ROOF);
            b.put(14, 5, z, Blocks.DARK_OAK_PLANKS.defaultBlockState(), BuildingBlueprints.Phase.ROOF);
        }
        for (int x = 1; x < 14; x++) for (int z = 1; z < 12; z++) {
            if (((x + z) & 1) == 0) b.put(x, 6, z, Blocks.DARK_OAK_SLAB.defaultBlockState(), BuildingBlueprints.Phase.ROOF);
        }
        int[][] stalls = {{2,3},{5,3},{9,3},{12,3},{2,9},{5,9},{9,9},{12,9}};
        for (int[] s : stalls) {
            b.put(s[0], 1, s[1], Blocks.OAK_PLANKS.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
            b.put(s[0], 2, s[1], Blocks.OAK_FENCE.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
            b.put(s[0], 3, s[1], Blocks.LANTERN.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        }
        for (int x = 5; x <= 9; x++) b.put(x, 1, 6, Blocks.POLISHED_ANDESITE.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(7, 2, 6, Blocks.BELL.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        return b.build();
    }

    private static List<BuildingBlueprints.Placement> citadel(BlockPos o) {
        Builder b = new Builder(o);
        for (int x = 0; x < 17; x++) for (int z = 0; z < 15; z++) {
            BlockState floor = (x >= 6 && x <= 10 && z >= 5 && z <= 9)
                    ? Blocks.POLISHED_ANDESITE.defaultBlockState()
                    : Blocks.STONE_BRICKS.defaultBlockState();
            b.put(x, 0, z, floor, BuildingBlueprints.Phase.FLOOR);
        }
        for (int y = 1; y <= 5; y++) {
            for (int x = 0; x < 17; x++) {
                b.put(x, y, 0, Blocks.STONE_BRICKS.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
                if (!entrance(x, y, 14, 7, 9)) b.put(x, y, 14, Blocks.STONE_BRICKS.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
            }
            for (int z = 1; z < 14; z++) {
                b.put(0, y, z, Blocks.STONE_BRICKS.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
                b.put(16, y, z, Blocks.STONE_BRICKS.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
            }
        }
        int[][] towers = {{1,1},{13,1},{1,11},{13,11}};
        for (int[] t : towers) {
            for (int x = t[0]; x < t[0] + 3; x++) for (int z = t[1]; z < t[1] + 3; z++) {
                for (int y = 1; y <= 8; y++) {
                    boolean shell = x == t[0] || x == t[0] + 2 || z == t[1] || z == t[1] + 2;
                    if (shell) b.put(x, y, z, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), BuildingBlueprints.Phase.FRAME_AND_WALLS);
                }
                b.put(x, 8, z, Blocks.STONE_BRICK_SLAB.defaultBlockState(), BuildingBlueprints.Phase.ROOF);
            }
        }
        for (int x = 0; x < 17; x += 2) {
            b.put(x, 6, 0, Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), BuildingBlueprints.Phase.ROOF);
            b.put(x, 6, 14, Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), BuildingBlueprints.Phase.ROOF);
        }
        for (int z = 0; z < 15; z += 2) {
            b.put(0, 6, z, Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), BuildingBlueprints.Phase.ROOF);
            b.put(16, 6, z, Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), BuildingBlueprints.Phase.ROOF);
        }
        for (int x = 6; x <= 10; x++) for (int z = 5; z <= 9; z++) {
            if (x == 6 || x == 10 || z == 5 || z == 9) b.put(x, 1, z, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        }
        b.put(8, 1, 7, Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        b.put(8, 2, 7, Blocks.LANTERN.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        for (int x = 7; x <= 9; x++) b.put(x, 1, 14, Blocks.POLISHED_ANDESITE.defaultBlockState(), BuildingBlueprints.Phase.FINISH);
        return b.build();
    }

    private static boolean entrance(int x, int y, int z, int minX, int maxX) {
        return z >= 0 && x >= minX && x <= maxX && y >= 1 && y <= 4;
    }

    private static final class Builder {
        private final BlockPos origin;
        private final Map<BlockPos, BuildingBlueprints.Placement> placements = new LinkedHashMap<>();

        private Builder(BlockPos origin) {
            this.origin = origin;
        }

        private void put(int x, int y, int z, BlockState state, BuildingBlueprints.Phase phase) {
            BlockPos pos = origin.offset(x, y, z);
            placements.put(pos, new BuildingBlueprints.Placement(pos, state, phase));
        }

        private List<BuildingBlueprints.Placement> build() {
            return List.copyOf(new ArrayList<>(placements.values()));
        }
    }
}
