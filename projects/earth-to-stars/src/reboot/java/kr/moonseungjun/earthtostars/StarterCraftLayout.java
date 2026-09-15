package kr.moonseungjun.earthtostars;

import net.minecraft.core.BlockPos;

import java.util.List;

public final class StarterCraftLayout {
    // Local template origin is the center of the floor. The controller is one block above it and toward the nose.
    public static final BlockPos CONTROLLER_FROM_FLOOR = new BlockPos(0, 1, -1);

    // All offsets below are relative to the Octo Controller block and remain valid after VS relocates the ship.
    public static final BlockPos CORE = new BlockPos(0, -1, 0);
    public static final BlockPos BATTERY = new BlockPos(0, 0, 2);

    public static final List<BlockPos> FORWARD_NODES = List.of(
            new BlockPos(-1, 1, 4),
            new BlockPos(1, 1, 4)
    );
    public static final List<BlockPos> FORWARD_MODULATORS = List.of(
            new BlockPos(-1, 0, 4),
            new BlockPos(1, 0, 4)
    );

    public static final List<BlockPos> REVERSE_NODES = List.of(new BlockPos(0, 1, -2));
    public static final List<BlockPos> REVERSE_MODULATORS = List.of(new BlockPos(0, 0, -2));

    public static final List<BlockPos> ASCEND_NODES = List.of(
            new BlockPos(-1, 1, 2),
            new BlockPos(1, 1, 2)
    );
    public static final List<BlockPos> ASCEND_MODULATORS = List.of(
            new BlockPos(-1, 0, 2),
            new BlockPos(1, 0, 2)
    );

    public static final List<BlockPos> DESCEND_NODES = List.of(new BlockPos(0, 1, 3));
    public static final List<BlockPos> DESCEND_MODULATORS = List.of(new BlockPos(0, 1, 2));

    public static final List<BlockPos> STRAFE_LEFT_NODES = List.of(new BlockPos(2, 1, 2));
    public static final List<BlockPos> STRAFE_LEFT_MODULATORS = List.of(new BlockPos(2, 0, 2));

    public static final List<BlockPos> STRAFE_RIGHT_NODES = List.of(new BlockPos(-2, 1, 2));
    public static final List<BlockPos> STRAFE_RIGHT_MODULATORS = List.of(new BlockPos(-2, 0, 2));

    public static final List<BlockPos> YAW_LEFT_NODES = List.of(new BlockPos(-1, 1, 1));
    public static final List<BlockPos> YAW_RIGHT_NODES = List.of(new BlockPos(1, 1, 1));

    public static final List<BlockPos> ALL_MODULATORS = List.of(
            new BlockPos(-1, 0, 4),
            new BlockPos(1, 0, 4),
            new BlockPos(0, 0, -2),
            new BlockPos(-1, 0, 2),
            new BlockPos(1, 0, 2),
            new BlockPos(0, 1, 2),
            new BlockPos(2, 0, 2),
            new BlockPos(-2, 0, 2)
    );

    private StarterCraftLayout() {
    }

    public static BlockPos controllerFromFloor(BlockPos floorCenter) {
        return floorCenter.offset(CONTROLLER_FROM_FLOOR);
    }
}
