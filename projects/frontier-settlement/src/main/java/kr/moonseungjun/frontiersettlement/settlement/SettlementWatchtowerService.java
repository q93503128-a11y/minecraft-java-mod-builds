package kr.moonseungjun.frontiersettlement.settlement;

/** Watchtower progression rule kept separate so every server entry point can enforce it. */
public final class SettlementWatchtowerService {
    private SettlementWatchtowerService() {}

    public static String lockedReason(SettlementData data) {
        if (data.buildingCount(BuildingType.GUARD_POST) < 1) {
            return "감시탑은 경비초소 1곳을 먼저 완성하면 열립니다.";
        }
        return null;
    }
}
