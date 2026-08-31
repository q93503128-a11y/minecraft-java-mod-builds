package kr.moonseungjun.frontiersettlement.settlement;

import kr.moonseungjun.frontiersettlement.content.FrontierContent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public final class SupplyDepotRegistryService {
    public static final int SETTLEMENT_LINK_RADIUS = 128;

    private SupplyDepotRegistryService() {}

    public static boolean tryRegister(ServerLevel level, BlockPos pos) {
        if (!level.dimension().equals(Level.OVERWORLD)) return false;
        SettlementData settlement = SettlementData.get(level.getServer());
        if (!settlement.founded() || !withinLinkRadius(settlement.centerPos(), pos) || !isDepot(level, pos)) return false;
        return SharedSupplyDepotData.get(level.getServer()).add(pos.immutable());
    }

    public static void unregister(ServerLevel level, BlockPos pos) {
        if (!level.dimension().equals(Level.OVERWORLD)) return;
        SharedSupplyDepotData.get(level.getServer()).remove(pos);
    }

    public static List<BlockPos> loadedPositions(ServerLevel level, SettlementData settlement) {
        if (!settlement.founded() || !level.dimension().equals(Level.OVERWORLD)) return List.of();
        SharedSupplyDepotData registry = SharedSupplyDepotData.get(level.getServer());
        List<BlockPos> result = new ArrayList<>();
        for (BlockPos pos : registry.positions()) {
            if (!withinLinkRadius(settlement.centerPos(), pos)) {
                registry.remove(pos);
                continue;
            }
            if (!level.hasChunkAt(pos)) continue;
            if (!isDepot(level, pos)) {
                registry.remove(pos);
                continue;
            }
            result.add(pos);
        }
        return result;
    }

    private static boolean isDepot(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(FrontierContent.SUPPLY_DEPOT.get())
                && level.getBlockEntity(pos) instanceof Container;
    }

    private static boolean withinLinkRadius(BlockPos center, BlockPos pos) {
        long dx = (long) center.getX() - pos.getX();
        long dy = (long) center.getY() - pos.getY();
        long dz = (long) center.getZ() - pos.getZ();
        return dx * dx + dy * dy + dz * dz <= (long) SETTLEMENT_LINK_RADIUS * SETTLEMENT_LINK_RADIUS;
    }
}
