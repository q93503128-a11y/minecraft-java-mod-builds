package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

public enum BuildingRotation {
    NONE(0, Rotation.NONE),
    CLOCKWISE_90(1, Rotation.CLOCKWISE_90),
    CLOCKWISE_180(2, Rotation.CLOCKWISE_180),
    COUNTERCLOCKWISE_90(3, Rotation.COUNTERCLOCKWISE_90);

    private final int id;
    private final Rotation vanillaRotation;

    BuildingRotation(int id, Rotation vanillaRotation) {
        this.id = id;
        this.vanillaRotation = vanillaRotation;
    }

    public int id() { return id; }

    public BuildingRotation next() {
        return fromId(id + 1);
    }

    public int rotatedWidth(BuildingType type) {
        return (this == CLOCKWISE_90 || this == COUNTERCLOCKWISE_90) ? type.depth() : type.width();
    }

    public int rotatedDepth(BuildingType type) {
        return (this == CLOCKWISE_90 || this == COUNTERCLOCKWISE_90) ? type.width() : type.depth();
    }

    public BlockPos rotateLocal(BlockPos origin, BlockPos absolute, int width, int depth) {
        int x = absolute.getX() - origin.getX();
        int y = absolute.getY() - origin.getY();
        int z = absolute.getZ() - origin.getZ();
        int rx;
        int rz;
        switch (this) {
            case NONE -> { rx = x; rz = z; }
            case CLOCKWISE_90 -> { rx = z; rz = width - 1 - x; }
            case CLOCKWISE_180 -> { rx = width - 1 - x; rz = depth - 1 - z; }
            case COUNTERCLOCKWISE_90 -> { rx = depth - 1 - z; rz = x; }
            default -> throw new IllegalStateException("Unexpected rotation " + this);
        }
        return origin.offset(rx, y, rz);
    }

    public BlockState rotateState(BlockState state) {
        return state.rotate(vanillaRotation);
    }

    public static BuildingRotation fromId(int id) {
        int normalized = Math.floorMod(id, 4);
        for (BuildingRotation rotation : values()) {
            if (rotation.id == normalized) return rotation;
        }
        return NONE;
    }

    /** Base blueprints face south. Start with the entrance facing back toward the player. */
    public static BuildingRotation facingPlayerFrom(Direction playerLookDirection) {
        Direction front = playerLookDirection.getOpposite();
        return switch (front) {
            case SOUTH -> NONE;
            case WEST -> CLOCKWISE_90;
            case NORTH -> CLOCKWISE_180;
            case EAST -> COUNTERCLOCKWISE_90;
            default -> NONE;
        };
    }
}
