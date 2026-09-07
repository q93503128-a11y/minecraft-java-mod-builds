package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** Stable rotation-aware physical central-storage positions for warehouse expansion. */
public final class WarehouseLayout {
    private static final int[][] BASE_STORAGE = {
            {2, 1, 2}, {5, 1, 2}, {8, 1, 2},
            {2, 1, 6}, {5, 1, 6}, {8, 1, 6}
    };
    private static final int[][] GRADE_II_STORAGE = {
            {2, 2, 2}, {8, 2, 2}, {2, 2, 6}, {8, 2, 6}
    };
    private static final int[][] GRADE_III_STORAGE = {
            {2, 3, 2}, {8, 3, 2}, {2, 3, 6}, {8, 3, 6}
    };

    private WarehouseLayout() {}

    /** Blueprint-owned Alpha.122-and-earlier base barrels. */
    public static List<BlockPos> storagePositions(BlockPos origin) {
        return positions(origin, BASE_STORAGE);
    }

    public static List<BlockPos> storagePositions(BuildingRecord warehouse) {
        return positions(warehouse, BASE_STORAGE);
    }

    /** Every cell Frontier may ever manage for this warehouse; inactive cells are not ledger authority. */
    public static List<BlockPos> managedStoragePositions(BuildingRecord warehouse) {
        List<BlockPos> result = new ArrayList<>(14);
        result.addAll(positions(warehouse, BASE_STORAGE));
        result.addAll(positions(warehouse, GRADE_II_STORAGE));
        result.addAll(positions(warehouse, GRADE_III_STORAGE));
        return result;
    }

    /** Grade I/II/III = 6/10/14 real barrels. */
    public static List<BlockPos> activeStoragePositions(BuildingRecord warehouse) {
        int grade = Math.max(1, Math.min(3, warehouse.upgradeGrade()));
        List<BlockPos> result = new ArrayList<>(grade == 1 ? 6 : grade == 2 ? 10 : 14);
        result.addAll(positions(warehouse, BASE_STORAGE));
        if (grade >= 2) result.addAll(positions(warehouse, GRADE_II_STORAGE));
        if (grade >= 3) result.addAll(positions(warehouse, GRADE_III_STORAGE));
        return result;
    }

    private static List<BlockPos> positions(BlockPos origin, int[][] offsets) {
        List<BlockPos> result = new ArrayList<>(offsets.length);
        for (int[] local : offsets) result.add(origin.offset(local[0], local[1], local[2]));
        return result;
    }

    private static List<BlockPos> positions(BuildingRecord warehouse, int[][] offsets) {
        List<BlockPos> result = new ArrayList<>(offsets.length);
        for (int[] local : offsets) result.add(warehouse.localToWorld(local[0], local[1], local[2]));
        return result;
    }
}
