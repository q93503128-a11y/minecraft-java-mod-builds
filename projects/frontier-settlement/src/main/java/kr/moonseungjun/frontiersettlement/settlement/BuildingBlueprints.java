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

public final class BuildingBlueprints {
    public enum Phase {
        FLOOR,
        FRAME_AND_WALLS,
        ROOF,
        FINISH
    }

    public record Placement(BlockPos pos, BlockState state, Phase phase) {}

    private BuildingBlueprints() {}

    public static List<Placement> create(BuildingType type, BlockPos origin) {
        return switch (type) {
            case HOUSE -> house(origin);
            case LUMBER_CAMP -> lumberCamp(origin);
        };
    }

    private static List<Placement> house(BlockPos o) {
        BlueprintBuilder b = new BlueprintBuilder(o);

        // Raised timber floor over the prepared continuous stone foundation.
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                b.put(x, 0, z, Blocks.SPRUCE_PLANKS.defaultBlockState(), Phase.FLOOR);
            }
        }

        // Warm timber-frame walls. Full glass blocks are deliberately used instead of panes so
        // partially built windows never detach or visually disconnect while construction is active.
        for (int y = 1; y <= 4; y++) {
            for (int x = 0; x < 9; x++) {
                if (!isHouseDoorOpening(x, y, 0)) {
                    b.put(x, y, 0, houseWallState(x, y, 0), Phase.FRAME_AND_WALLS);
                }
                if (!isHouseDoorOpening(x, y, 8)) {
                    b.put(x, y, 8, houseWallState(x, y, 8), Phase.FRAME_AND_WALLS);
                }
            }
            for (int z = 1; z < 8; z++) {
                b.put(0, y, z, houseWallState(0, y, z), Phase.FRAME_AND_WALLS);
                b.put(8, y, z, houseWallState(8, y, z), Phase.FRAME_AND_WALLS);
            }
        }

        // Corner posts and a visible upper ring make every roof edge read as structurally supported.
        for (int y = 1; y <= 4; y++) {
            b.put(0, y, 0, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), Phase.FRAME_AND_WALLS);
            b.put(8, y, 0, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), Phase.FRAME_AND_WALLS);
            b.put(0, y, 8, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), Phase.FRAME_AND_WALLS);
            b.put(8, y, 8, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), Phase.FRAME_AND_WALLS);
        }
        for (int x = 0; x < 9; x++) {
            b.put(x, 4, 0, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), Phase.FRAME_AND_WALLS);
            b.put(x, 4, 8, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), Phase.FRAME_AND_WALLS);
        }

        // Timber-filled gables are completed before a single roof block is allowed to place.
        for (int x = 1; x <= 7; x++) {
            b.put(x, 5, 0, Blocks.OAK_PLANKS.defaultBlockState(), Phase.FRAME_AND_WALLS);
            b.put(x, 5, 8, Blocks.OAK_PLANKS.defaultBlockState(), Phase.FRAME_AND_WALLS);
        }
        for (int x = 2; x <= 6; x++) {
            b.put(x, 6, 0, Blocks.OAK_PLANKS.defaultBlockState(), Phase.FRAME_AND_WALLS);
            b.put(x, 6, 8, Blocks.OAK_PLANKS.defaultBlockState(), Phase.FRAME_AND_WALLS);
        }
        for (int x = 3; x <= 5; x++) {
            b.put(x, 7, 0, Blocks.OAK_PLANKS.defaultBlockState(), Phase.FRAME_AND_WALLS);
            b.put(x, 7, 8, Blocks.OAK_PLANKS.defaultBlockState(), Phase.FRAME_AND_WALLS);
        }
        b.put(4, 8, 0, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), Phase.FRAME_AND_WALLS);
        b.put(4, 8, 8, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), Phase.FRAME_AND_WALLS);

        BlockState leftRoof = Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
        BlockState rightRoof = Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST);
        for (int z = -1; z <= 9; z++) {
            for (int layer = 0; layer < 5; layer++) {
                int y = 4 + layer;
                b.put(-1 + layer, y, z, leftRoof, Phase.ROOF);
                b.put(9 - layer, y, z, rightRoof, Phase.ROOF);
            }
            b.put(4, 9, z, Blocks.SPRUCE_SLAB.defaultBlockState(), Phase.ROOF);
        }

        BlockState lowerDoor = Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.SOUTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        BlockState upperDoor = lowerDoor.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
        b.put(4, 1, 8, lowerDoor, Phase.FINISH);
        b.put(4, 2, 8, upperDoor, Phase.FINISH);

        // Four interior lanterns keep the 7x7 usable room safely illuminated with generous overlap.
        b.put(2, 1, 2, Blocks.LANTERN.defaultBlockState(), Phase.FINISH);
        b.put(6, 1, 2, Blocks.LANTERN.defaultBlockState(), Phase.FINISH);
        b.put(2, 1, 6, Blocks.LANTERN.defaultBlockState(), Phase.FINISH);
        b.put(6, 1, 6, Blocks.LANTERN.defaultBlockState(), Phase.FINISH);
        b.put(4, 1, 3, Blocks.CRAFTING_TABLE.defaultBlockState(), Phase.FINISH);

        return b.build();
    }

    private static boolean isHouseDoorOpening(int x, int y, int z) {
        return z == 8 && x == 4 && (y == 1 || y == 2);
    }

    private static BlockState houseWallState(int x, int y, int z) {
        boolean frontBackWindow = (z == 0 || z == 8)
                && (x == 2 || x == 6) && (y == 2 || y == 3);
        boolean sideWindow = (x == 0 || x == 8)
                && (z == 2 || z == 6) && (y == 2 || y == 3);
        if (frontBackWindow || sideWindow) return Blocks.GLASS.defaultBlockState();
        return Blocks.OAK_PLANKS.defaultBlockState();
    }

    private static List<Placement> lumberCamp(BlockPos o) {
        BlueprintBuilder b = new BlueprintBuilder(o);

        for (int x = 0; x < 11; x++) {
            for (int z = 0; z < 9; z++) {
                b.put(x, 0, z, Blocks.SPRUCE_PLANKS.defaultBlockState(), Phase.FLOOR);
            }
        }

        // Rear wall plus half-height side walls create a readable workshop without trapping the worker.
        for (int y = 1; y <= 4; y++) {
            for (int x = 0; x < 11; x++) {
                BlockState state = ((x == 2 || x == 5 || x == 8) && (y == 2 || y == 3))
                        ? Blocks.GLASS.defaultBlockState()
                        : Blocks.OAK_PLANKS.defaultBlockState();
                b.put(x, y, 0, state, Phase.FRAME_AND_WALLS);
            }
        }
        for (int y = 1; y <= 3; y++) {
            for (int z = 1; z <= 4; z++) {
                b.put(0, y, z, Blocks.OAK_PLANKS.defaultBlockState(), Phase.FRAME_AND_WALLS);
                b.put(10, y, z, Blocks.OAK_PLANKS.defaultBlockState(), Phase.FRAME_AND_WALLS);
            }
        }

        int[][] posts = new int[][] {{0, 0}, {5, 0}, {10, 0}, {0, 8}, {5, 8}, {10, 8}};
        for (int[] post : posts) {
            for (int y = 1; y <= 4; y++) {
                b.put(post[0], y, post[1], Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), Phase.FRAME_AND_WALLS);
            }
        }
        for (int x = 0; x < 11; x++) {
            b.put(x, 4, 0, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), Phase.FRAME_AND_WALLS);
            b.put(x, 4, 8, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), Phase.FRAME_AND_WALLS);
        }

        // Rear gable and front beam exist before the roof phase, preventing any detached roof pass.
        for (int layer = 0; layer < 5; layer++) {
            int y = 5 + layer;
            for (int x = 1 + layer; x <= 9 - layer; x++) {
                b.put(x, y, 0, Blocks.OAK_PLANKS.defaultBlockState(), Phase.FRAME_AND_WALLS);
            }
        }
        b.put(5, 9, 0, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), Phase.FRAME_AND_WALLS);

        BlockState leftRoof = Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
        BlockState rightRoof = Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST);
        for (int z = -1; z <= 9; z++) {
            for (int layer = 0; layer < 6; layer++) {
                int y = 4 + layer;
                b.put(-1 + layer, y, z, leftRoof, Phase.ROOF);
                b.put(11 - layer, y, z, rightRoof, Phase.ROOF);
            }
            b.put(5, 10, z, Blocks.SPRUCE_SLAB.defaultBlockState(), Phase.ROOF);
        }

        // Work fixtures and a four-point light grid. The open front remains bright rather than becoming
        // a covered hostile-mob pocket at night.
        b.put(5, 1, 3, Blocks.CRAFTING_TABLE.defaultBlockState(), Phase.FINISH);
        b.put(2, 1, 5, Blocks.STRIPPED_OAK_LOG.defaultBlockState(), Phase.FINISH);
        b.put(3, 1, 5, Blocks.STRIPPED_OAK_LOG.defaultBlockState(), Phase.FINISH);
        b.put(7, 1, 5, Blocks.STRIPPED_OAK_LOG.defaultBlockState(), Phase.FINISH);
        b.put(8, 1, 5, Blocks.STRIPPED_OAK_LOG.defaultBlockState(), Phase.FINISH);
        b.put(2, 1, 2, Blocks.LANTERN.defaultBlockState(), Phase.FINISH);
        b.put(8, 1, 2, Blocks.LANTERN.defaultBlockState(), Phase.FINISH);
        b.put(2, 1, 7, Blocks.LANTERN.defaultBlockState(), Phase.FINISH);
        b.put(8, 1, 7, Blocks.LANTERN.defaultBlockState(), Phase.FINISH);

        return b.build();
    }

    private static final class BlueprintBuilder {
        private final BlockPos origin;
        private final Map<BlockPos, Placement> placements = new LinkedHashMap<>();

        private BlueprintBuilder(BlockPos origin) {
            this.origin = origin;
        }

        private void put(int x, int y, int z, BlockState state, Phase phase) {
            BlockPos absolute = origin.offset(x, y, z);
            placements.remove(absolute);
            placements.put(absolute, new Placement(absolute, state, phase));
        }

        private List<Placement> build() {
            return new ArrayList<>(placements.values());
        }
    }
}
