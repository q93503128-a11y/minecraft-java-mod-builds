package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;

/** Stable functional positions inside the market blueprint. */
public final class MarketLayout {
    private static final int TRADE_X = 5;
    private static final int TRADE_Y = 1;
    private static final int TRADE_Z = 5;

    private MarketLayout() {}

    public static BlockPos tradeCrate(BlockPos origin) {
        return origin.offset(TRADE_X, TRADE_Y, TRADE_Z);
    }

    public static BlockPos tradeCrate(BuildingRecord market) {
        return market.localToWorld(TRADE_X, TRADE_Y, TRADE_Z);
    }
}
