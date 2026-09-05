package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/** Cart-station rules that deliberately do not become a second transport navigation authority. */
public final class SettlementCartStationService {
    public static final int MAX_ROAD_DISTANCE = 12;
    private static final long MAX_ROAD_DISTANCE_SQR = (long) MAX_ROAD_DISTANCE * MAX_ROAD_DISTANCE;

    private SettlementCartStationService() {}

    public static String lockedReason(SettlementData data) {
        if (SettlementTier.current(data).ordinal() < SettlementTier.VILLAGE.ordinal()) {
            return "수레 정거장은 마을 단계에 도달한 뒤 열립니다.";
        }
        if (data.roads().isEmpty() || data.outposts().isEmpty()) {
            return "수레 정거장은 도로와 연결된 전초기지를 만든 뒤 열립니다.";
        }
        return null;
    }

    public static String placementReason(SettlementData data, BlockPos selectedCenter) {
        long best = Long.MAX_VALUE;
        for (RoadSegment road : data.roads()) {
            for (BlockPos roadPos : road.centers()) {
                long dx = (long) selectedCenter.getX() - roadPos.getX();
                long dz = (long) selectedCenter.getZ() - roadPos.getZ();
                best = Math.min(best, dx * dx + dz * dz);
                if (best <= MAX_ROAD_DISTANCE_SQR) return null;
            }
        }
        return "수레 정거장은 기존 도로에서 " + MAX_ROAD_DISTANCE + "블록 안쪽에 배치해 주세요.";
    }

    public static void onBreakBlock(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        MinecraftServer server = level.getServer();
        if (level != server.overworld()) return;
        if (!event.getState().is(Blocks.BARREL)) return;
        SettlementData data = SettlementData.get(server);
        if (!data.founded() || !level.getBlockState(event.getPos()).is(Blocks.BARREL)) return;

        BlockPos pos = event.getPos();
        for (BuildingRecord station : data.buildings()) {
            if (station.buildingType() != BuildingType.CART_STATION) continue;
            if (!CartStationLayout.freightPositions(station).contains(pos)) continue;
            event.setCanceled(true);
            event.setNotifyClient(true);
            return;
        }
    }
}
