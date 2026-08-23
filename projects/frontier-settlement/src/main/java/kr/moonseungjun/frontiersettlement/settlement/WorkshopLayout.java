package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;

/** Stable local positions for the player-facing workshop service station. */
public final class WorkshopLayout {
    private static final int SERVICE_X = 5;
    private static final int SERVICE_Y = 1;
    private static final int SERVICE_Z = 4;

    private WorkshopLayout() {}

    public static BlockPos serviceCrate(BlockPos origin) {
        return origin.offset(SERVICE_X, SERVICE_Y, SERVICE_Z);
    }

    public static BlockPos serviceCrate(BuildingRecord workshop) {
        return workshop.localToWorld(SERVICE_X, SERVICE_Y, SERVICE_Z);
    }
}
