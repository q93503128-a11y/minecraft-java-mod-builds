package kr.countrysidedays.world;

import net.minecraft.core.BlockPos;

/** Fixed non-overlapping geometry shared by generation, protection, HUD, AI and tests. */
public final class PlayerEstateLayout {
    public static final int MIN_X = -30;
    public static final int MAX_X = 30;
    public static final int MIN_Z = -26;
    public static final int MAX_Z = 30;
    public static final int PROTECTED_MIN_Z = MIN_Z - 2;

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

    public static BlockPos restaurantSign(BlockPos origin) {
        return origin.offset(11, 2, -8);
    }

    public static BlockPos customerSeat(BlockPos origin) {
        return customerSeat(origin, 0);
    }

    public static BlockPos customerSeat(BlockPos origin, int slot) {
        return switch (Math.floorMod(slot, CountrysideWorldData.DAILY_CUSTOMER_CAP)) {
            case 0 -> origin.offset(14, 1, -12);
            case 1 -> origin.offset(13, 1, -13);
            default -> origin.offset(21, 1, -12);
        };
    }

    public static BlockPos kitchenCounter(BlockPos origin) {
        return origin.offset(10, 1, -14);
    }

    public static BlockPos ownerGate(BlockPos origin) {
        return origin.offset(0, 0, MIN_Z);
    }

    public static BlockPos ownerSign(BlockPos origin) {
        return origin.offset(3, 0, MIN_Z - 1);
    }

    public static BlockPos farm(BlockPos origin) {
        return origin.offset(-17, 1, 11);
    }

    public static BlockPos farmGate(BlockPos origin) {
        return origin.offset(-7, 0, 10);
    }

    public static BlockPos farmStorageBarrel(BlockPos origin) {
        return origin.offset(-9, 0, 18);
    }

    public static BlockPos ranch(BlockPos origin) {
        return origin.offset(17, 1, 16);
    }

    public static BlockPos ranchGate(BlockPos origin) {
        return origin.offset(7, 0, 2);
    }

    public static BlockPos ranchSupplyBarrel(BlockPos origin) {
        return origin.offset(12, 0, 17);
    }

    public static BlockPos ranchCollectionBarrel(BlockPos origin) {
        return origin.offset(24, 0, 17);
    }

    public static BlockPos hayFeeder(BlockPos origin) {
        return origin.offset(23, 0, 22);
    }

    public static BlockPos waterTrough(BlockPos origin) {
        return origin.offset(9, 0, 23);
    }

    public static boolean contains(BlockPos origin, BlockPos pos) {
        return pos.getX() >= origin.getX() + MIN_X && pos.getX() <= origin.getX() + MAX_X
                && pos.getY() >= origin.getY() - 6 && pos.getY() <= origin.getY() + 18
                && pos.getZ() >= origin.getZ() + PROTECTED_MIN_Z
                && pos.getZ() <= origin.getZ() + MAX_Z;
    }
}
