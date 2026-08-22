package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public final class OutpostBlueprints {
    public static final int LENGTH = 9;
    public static final int WIDTH = 9;
    public static final int CLEAR_HEIGHT = 6;

    private OutpostBlueprints() {}

    public record Placement(BlockPos pos, BlockState state) {}

    public static List<Placement> create(OutpostConstructionState state) {
        List<Placement> placements = new ArrayList<>();
        addFloor(placements, state);
        addPerimeterAndShelter(placements, state);
        addRoof(placements, state);
        addFixtures(placements, state);
        return placements;
    }

    public static BlockPos center(OutpostConstructionState state) {
        return local(state, 4, 0, 0);
    }

    public static BlockPos stockpile(OutpostConstructionState state) {
        return local(state, 7, 0, 1);
    }

    private static void addFloor(List<Placement> out, OutpostConstructionState state) {
        for (int forward = 0; forward < LENGTH; forward++) {
            for (int side = -4; side <= 4; side++) {
                boolean edge = forward == 0 || forward == LENGTH - 1 || Math.abs(side) == 4;
                add(out, state, forward, side, 0,
                        edge ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState());
            }
        }
    }

    private static void addPerimeterAndShelter(List<Placement> out, OutpostConstructionState state) {
        for (int side = -4; side <= 4; side++) {
            if (Math.abs(side) > 1) add(out, state, 0, side, 1, Blocks.OAK_FENCE.defaultBlockState());
        }
        for (int forward = 1; forward < LENGTH; forward++) {
            add(out, state, forward, -4, 1, Blocks.OAK_FENCE.defaultBlockState());
            add(out, state, forward, 4, 1, Blocks.OAK_FENCE.defaultBlockState());
        }
        for (int side = -3; side <= 3; side++) {
            if (Math.abs(side) == 3) continue;
            for (int y = 1; y <= 3; y++) {
                BlockState wall = (y == 2 && (side == -1 || side == 1))
                        ? Blocks.GLASS.defaultBlockState()
                        : Blocks.SPRUCE_PLANKS.defaultBlockState();
                add(out, state, 8, side, y, wall);
            }
        }

        for (int forward = 5; forward <= 7; forward++) {
            for (int sign : new int[] {-1, 1}) {
                for (int y = 1; y <= 3; y++) {
                    BlockState wall = (forward == 6 && y == 2)
                            ? Blocks.GLASS.defaultBlockState()
                            : Blocks.SPRUCE_PLANKS.defaultBlockState();
                    add(out, state, forward, 3 * sign, y, wall);
                }
            }
        }

        for (int forward : new int[] {5, 8}) {
            for (int side : new int[] {-3, 3}) {
                for (int y = 1; y <= 3; y++) {
                    add(out, state, forward, side, y, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());
                }
            }
        }

        for (int side : new int[] {-3, 3}) {
            for (int y = 1; y <= 2; y++) {
                add(out, state, 1, side, y,
                        y == 1 ? Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState()
                                : Blocks.LANTERN.defaultBlockState());
                add(out, state, 3, side, y,
                        y == 1 ? Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState()
                                : Blocks.LANTERN.defaultBlockState());
            }
        }
    }

    private static void addRoof(List<Placement> out, OutpostConstructionState state) {
        for (int forward = 4; forward <= 8; forward++) {
            for (int side = -4; side <= 4; side++) {
                add(out, state, forward, side, 4, Blocks.SPRUCE_SLAB.defaultBlockState());
            }
        }
    }

    private static void addFixtures(List<Placement> out, OutpostConstructionState state) {
        add(out, state, 6, -1, 1, Blocks.LANTERN.defaultBlockState());
        add(out, state, 6, 1, 1, Blocks.LANTERN.defaultBlockState());
        add(out, state, 7, 0, 1, Blocks.BARREL.defaultBlockState());
        add(out, state, 7, 2, 1, Blocks.CRAFTING_TABLE.defaultBlockState());
    }

    private static void add(List<Placement> out, OutpostConstructionState state,
                            int forward, int side, int y, BlockState blockState) {
        out.add(new Placement(local(state, forward, side, y), blockState));
    }

    public static BlockPos local(OutpostConstructionState state, int forward, int side, int y) {
        int x = state.gateX() + state.directionX() * forward - state.directionZ() * side;
        int z = state.gateZ() + state.directionZ() * forward + state.directionX() * side;
        return new BlockPos(x, state.gateY() + y, z);
    }
}
