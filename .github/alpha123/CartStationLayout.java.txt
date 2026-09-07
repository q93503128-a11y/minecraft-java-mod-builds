package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** Stable rotation-aware freight positions for the town cart station. */
public final class CartStationLayout {
    private static final int[][] BASE_FREIGHT = {
            {3, 1, 2}, {9, 1, 2}, {3, 1, 6}, {9, 1, 6}
    };
    private static final int[][] GRADE_II_FREIGHT = {
            {3, 2, 2}, {9, 2, 2}
    };
    private static final int[][] GRADE_III_FREIGHT = {
            {3, 2, 6}, {9, 2, 6}
    };

    private CartStationLayout() {}

    /** Base blueprint freight remains four barrels for save compatibility. */
    public static int freightSlotCount() { return BASE_FREIGHT.length; }

    public static List<BlockPos> freightPositions(BlockPos origin) {
        return positions(origin, BASE_FREIGHT);
    }

    public static List<BlockPos> freightPositions(BuildingRecord station) {
        return positions(station, BASE_FREIGHT);
    }

    /** Grade I/II/III = 4/6/8 physical freight barrels. */
    public static List<BlockPos> activeFreightPositions(BuildingRecord station) {
        int grade = Math.max(1, Math.min(3, station.upgradeGrade()));
        List<BlockPos> result = new ArrayList<>(grade == 1 ? 4 : grade == 2 ? 6 : 8);
        result.addAll(positions(station, BASE_FREIGHT));
        if (grade >= 2) result.addAll(positions(station, GRADE_II_FREIGHT));
        if (grade >= 3) result.addAll(positions(station, GRADE_III_FREIGHT));
        return result;
    }

    public static List<BlockPos> managedFreightPositions(BuildingRecord station) {
        List<BlockPos> result = new ArrayList<>(8);
        result.addAll(positions(station, BASE_FREIGHT));
        result.addAll(positions(station, GRADE_II_FREIGHT));
        result.addAll(positions(station, GRADE_III_FREIGHT));
        return result;
    }

    public static BlockPos loadingLane(BuildingRecord station) {
        return station.localToWorld(6, 1, 4);
    }

    private static List<BlockPos> positions(BlockPos origin, int[][] offsets) {
        List<BlockPos> result = new ArrayList<>(offsets.length);
        for (int[] local : offsets) result.add(origin.offset(local[0], local[1], local[2]));
        return result;
    }

    private static List<BlockPos> positions(BuildingRecord station, int[][] offsets) {
        List<BlockPos> result = new ArrayList<>(offsets.length);
        for (int[] local : offsets) result.add(station.localToWorld(local[0], local[1], local[2]));
        return result;
    }
}
