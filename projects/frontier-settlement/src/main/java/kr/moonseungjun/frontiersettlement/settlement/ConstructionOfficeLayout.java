package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;

import java.util.List;

/** Rotation-aware physical material bays inside a completed construction office. */
public final class ConstructionOfficeLayout {
    private static final int[][] MATERIAL_SLOTS = {
            {2, 1, 2}, {4, 1, 2}, {8, 1, 2}, {10, 1, 2}
    };

    private ConstructionOfficeLayout() {}

    public static List<BlockPos> materialPositions(BuildingRecord office) {
        return java.util.Arrays.stream(MATERIAL_SLOTS)
                .map(slot -> office.localToWorld(slot[0], slot[1], slot[2]))
                .toList();
    }
}
