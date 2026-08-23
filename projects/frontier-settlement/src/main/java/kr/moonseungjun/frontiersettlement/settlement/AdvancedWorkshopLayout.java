package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;

/** Rotation-aware functional positions inside the advanced workshop blueprint. */
public final class AdvancedWorkshopLayout {
    private AdvancedWorkshopLayout() {}

    public static BlockPos commissionCrate(BuildingRecord workshop) {
        return workshop.localToWorld(7, 1, 3);
    }

    public static BlockPos artisanHome(BuildingRecord workshop) {
        return workshop.localToWorld(7, 1, 6);
    }
}
