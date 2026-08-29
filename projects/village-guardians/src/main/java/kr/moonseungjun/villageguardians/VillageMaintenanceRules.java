package kr.moonseungjun.villageguardians;

/** One authoritative server-side gate for actions that belong to the daytime preparation phase. */
public final class VillageMaintenanceRules {
    private VillageMaintenanceRules() {}

    public static String blockReason(String action) {
        if (VillageProgressionSystem.isGameOver()) {
            return "게임 오버 상태에서는 " + action + "을(를) 실행할 수 없습니다. 재시작을 먼저 선택하세요.";
        }
        if (VillageRaidSystem.isRaidLocked() || VillageCouncilState.currentPhase() != VillageTimePhase.DAY) {
            return action + "은(는) 낮 정비 시간에만 가능합니다.";
        }
        return null;
    }
}
