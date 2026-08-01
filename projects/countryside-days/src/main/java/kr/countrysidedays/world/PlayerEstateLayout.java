package kr.countrysidedays.world;

import net.minecraft.core.BlockPos;

/** Fixed non-overlapping geometry shared by generation, protection, HUD and tests. */
public final class PlayerEstateLayout {
    public static final int MIN_X = -30;
    public static final int MAX_X = 30;
    public static final int MIN_Z = -26;
    public static final int MAX_Z = 30;

    private static final int SLOT_DISTANCE = 112;

    private PlayerEstateLayout() {
    }

    public static BlockPos originForIndex(BlockPos villageOrigin, int index) {
        int ring = Math.max(1, index / 8 + 1);
        int slot = Math.floorMod(index, 8);
        int radius = SLOT_DISTANCE * ring;
        int dx = switch (slot) {
            case 0, 4 -> 0;
            case 1, 2, 3 -> radius;
            default -> -radius;
        };
        int dz = switch (slot) {
            case 0, 1, 7 -> radius;
            case 2, 6 -> 0;
            default -> -radius;
        };
        return villageOrigin.offset(dx, 0, dz);
    }

    public static BlockPos home(BlockPos origin) {
        return origin.offset(-21, 1, -13);
    }

    public static BlockPos homeDoor(BlockPos origin) {
        return origin.offset(-21, 1, -8);
    }

    public static BlockPos restaurant(BlockPos origin) {
        return origin.offset(17, 1, -14);
    }

    public static BlockPos restaurantDoor(BlockPos origin) {
        return origin.offset(17, 1, -8);
    }

    public static BlockPos kitchenCounter(BlockPos origin) {
        return origin.offset(10, 1, -14);
    }

    public static BlockPos farm(BlockPos origin) {
        return origin.offset(-17, 1, 11);
    }

    public static BlockPos farmGate(BlockPos origin) {
        return origin.offset(-7, 1, 10);
    }

    public static BlockPos ranch(BlockPos origin) {
        return origin.offset(17, 1, 16);
    }

    public static BlockPos ranchGate(BlockPos origin) {
        return origin.offset(6, 1, 14);
    }

    public static boolean contains(BlockPos origin, BlockPos pos) {
        return pos.getX() >= origin.getX() + MIN_X && pos.getX() <= origin.getX() + MAX_X
                && pos.getY() >= origin.getY() - 6 && pos.getY() <= origin.getY() + 18
                && pos.getZ() >= origin.getZ() + MIN_Z && pos.getZ() <= origin.getZ() + MAX_Z;
    }
}
