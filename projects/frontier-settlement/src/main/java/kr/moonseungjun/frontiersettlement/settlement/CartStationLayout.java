package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** Stable rotation-aware freight positions for the town cart station. */
public final class CartStationLayout {
    private static final int[][] FREIGHT = {
            {3, 1, 2}, {9, 1, 2}, {3, 1, 6}, {9, 1, 6}
    };

    private CartStationLayout() {}

    public static List<BlockPos> freightPositions(BlockPos origin) {
        List<BlockPos> result = new ArrayList<>(FREIGHT.length);
        for (int[] local : FREIGHT) result.add(origin.offset(local[0], local[1], local[2]));
        return result;
    }

    public static List<BlockPos> freightPositions(BuildingRecord station) {
        List<BlockPos> result = new ArrayList<>(FREIGHT.length);
        for (int[] local : FREIGHT) result.add(station.localToWorld(local[0], local[1], local[2]));
        return result;
    }

    public static BlockPos loadingLane(BuildingRecord station) {
        return station.localToWorld(6, 1, 4);
    }
}
