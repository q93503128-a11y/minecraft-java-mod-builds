package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

public final class WarehouseLayout {
    private static final int[][] STORAGE_OFFSETS = new int[][] {
            {2, 1, 2}, {5, 1, 2}, {8, 1, 2},
            {2, 1, 6}, {5, 1, 6}, {8, 1, 6}
    };

    private WarehouseLayout() {}

    public static List<BlockPos> storagePositions(BlockPos origin) {
        List<BlockPos> result = new ArrayList<>(STORAGE_OFFSETS.length);
        for (int[] offset : STORAGE_OFFSETS) result.add(origin.offset(offset[0], offset[1], offset[2]));
        return result;
    }

    public static List<BlockPos> storagePositions(BuildingRecord warehouse) {
        return storagePositions(warehouse.origin());
    }
}
