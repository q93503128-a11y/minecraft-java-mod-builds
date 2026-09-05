package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;

import java.util.List;

/** Server-authoritative construction lane and concurrency policy. */
public final class SettlementProjectAuthority {
    public enum ProjectLane { BUILDING, ROAD, OUTPOST }

    public static final int MAX_PARALLEL_MANAGED_PROJECTS = 3;
    public static final int MIN_PARALLEL_SEPARATION = 24;
    private static final long MIN_PARALLEL_SEPARATION_SQR = (long) MIN_PARALLEL_SEPARATION * MIN_PARALLEL_SEPARATION;

    private SettlementProjectAuthority() {}

    public static int parallelProjectLimit(SettlementData data) {
        int offices = Math.max(0, data.buildingCount(BuildingType.CONSTRUCTION_OFFICE));
        int expansion = offices + data.outposts().size() / 2;
        return Math.min(MAX_PARALLEL_MANAGED_PROJECTS, 1 + Math.min(2, expansion));
    }

    public static int activeManagedProjectCount(SettlementData data) {
        int active = 0;
        if (data.construction().active()) active++;
        if (data.roadConstruction().active()) active++;
        if (data.outpostConstruction().active()) active++;
        return active;
    }

    public static boolean laneActive(SettlementData data, ProjectLane lane) {
        return switch (lane) {
            case BUILDING -> data.construction().active();
            case ROAD -> data.roadConstruction().active();
            case OUTPOST -> data.outpostConstruction().active();
        };
    }

    public static String startBlockReason(MinecraftServer server, SettlementData data, ProjectLane lane) {
        if (SettlementCivilWorkData.get(server).project().active()) {
            return "대규모 토목 평탄화가 끝난 뒤 다른 공사를 시작해 주세요.";
        }
        if (laneActive(data, lane)) {
            return switch (lane) {
                case BUILDING -> "이미 본진 건물 공사가 진행 중입니다.";
                case ROAD -> "이미 도로 공사가 진행 중입니다.";
                case OUTPOST -> "이미 전초기지 공사가 진행 중입니다.";
            };
        }
        int limit = parallelProjectLimit(data);
        if (activeManagedProjectCount(data) >= limit) {
            return "동시 공사 슬롯 " + limit + "개가 모두 사용 중입니다. 건설소나 전초기지를 늘리면 최대 3개까지 확장됩니다.";
        }
        return null;
    }

    public static boolean separatedFromOtherActive(SettlementData data, ProjectLane lane, BlockPos point) {
        if (lane != ProjectLane.BUILDING && data.construction().active()
                && horizontalDistanceSqr(point, data.construction().origin()) < MIN_PARALLEL_SEPARATION_SQR) return false;
        if (lane != ProjectLane.ROAD && data.roadConstruction().active()) {
            for (BlockPos center : data.roadConstruction().centers()) {
                if (horizontalDistanceSqr(point, center) < MIN_PARALLEL_SEPARATION_SQR) return false;
            }
        }
        if (lane != ProjectLane.OUTPOST && data.outpostConstruction().active()
                && horizontalDistanceSqr(point, data.outpostConstruction().gate()) < MIN_PARALLEL_SEPARATION_SQR) return false;
        return true;
    }

    public static boolean routeSeparatedFromOtherActive(SettlementData data, ProjectLane lane, List<BlockPos> route) {
        for (BlockPos point : route) if (!separatedFromOtherActive(data, lane, point)) return false;
        return true;
    }

    private static long horizontalDistanceSqr(BlockPos a, BlockPos b) {
        long dx = (long) a.getX() - b.getX();
        long dz = (long) a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    public static boolean anyActive(MinecraftServer server, SettlementData data) {
        return activeManagedProjectCount(data) > 0 || SettlementCivilWorkData.get(server).project().active();
    }
}
