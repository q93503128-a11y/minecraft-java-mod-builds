package kr.moonseungjun.frontiersettlement.settlement;

/** Production throughput is persisted per physical production building. Settlement tier only unlocks the next ceiling. */
public final class SettlementProductionEfficiencyService {
    private SettlementProductionEfficiencyService() {}

    public static boolean isProductionBuilding(BuildingType type) {
        return type == BuildingType.LUMBER_CAMP || type == BuildingType.FARM
                || type == BuildingType.QUARRY || type == BuildingType.MINE;
    }

    public static int maxGrade(SettlementData data) {
        return switch (SettlementTier.current(data)) {
            case CAMP, HAMLET -> 1;
            case VILLAGE -> 2;
            case FRONTIER_TOWN -> 3;
            case DOMAIN, FRONTIER_CAPITAL -> 4;
        };
    }

    /** Legacy grade 0 temporarily mirrors the old derived tier until the one-way migration freezes it. */
    public static int grade(SettlementData data, BuildingRecord building) {
        if (building == null || !isProductionBuilding(building.buildingType())) return 1;
        if (building.upgradeGrade() <= 0) return maxGrade(data);
        return clampGrade(building.upgradeGrade());
    }

    public static String gradeLabel(int grade) {
        return switch (clampGrade(grade)) { case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; default -> "IV"; };
    }
    public static int lumberWorkPeriod(int grade) { return switch (clampGrade(grade)) { case 1 -> 100; case 2 -> 90; case 3 -> 80; default -> 70; }; }
    public static int lumberBatch(int grade) { return switch (clampGrade(grade)) { case 1 -> 16; case 2 -> 20; case 3 -> 24; default -> 32; }; }
    public static int farmWorkPeriod(int grade) { return switch (clampGrade(grade)) { case 1 -> 120; case 2 -> 100; case 3 -> 80; default -> 80; }; }
    public static int farmBatch(int grade) { return switch (clampGrade(grade)) { case 1 -> 12; case 2 -> 16; case 3 -> 20; default -> 24; }; }
    public static int farmGrowthModulo(int grade) { return switch (clampGrade(grade)) { case 1 -> 3; case 2, 3 -> 2; default -> 1; }; }
    public static int quarryWorkPeriod(int grade) { return switch (clampGrade(grade)) { case 1 -> 80; case 2 -> 70; case 3 -> 60; default -> 50; }; }
    public static int quarryBatch(int grade) { return switch (clampGrade(grade)) { case 1 -> 16; case 2 -> 20; case 3 -> 24; default -> 32; }; }
    public static int mineWorkPeriod(int grade) { return switch (clampGrade(grade)) { case 1 -> 160; case 2 -> 130; case 3 -> 100; default -> 80; }; }
    public static int worksiteBufferCount(int grade) { return switch (clampGrade(grade)) { case 1 -> 1; case 2 -> 2; default -> 3; }; }

    public static String detail(BuildingType type, SettlementData data, BuildingRecord building) {
        int grade = grade(data, building);
        String prefix = "개량 " + gradeLabel(grade) + " · ";
        String buffer = " · 현장 버퍼 " + worksiteBufferCount(grade) + "통";
        String investment = " · " + SettlementProductionUpgradeService.upgradeHint(data, building);
        return switch (type) {
            case LUMBER_CAMP -> prefix + "자동 벌목 · 작업 묶음 " + lumberBatch(grade) + buffer + investment;
            case FARM -> prefix + "자동 식량 생산 · 수확 묶음 " + farmBatch(grade) + " · 작물 성장 관리 "
                    + (farmGrowthModulo(grade) == 1 ? "전 구획" : farmGrowthModulo(grade) + "구획 순환") + buffer + investment;
            case QUARRY -> prefix + "자동 채석 · 작업 묶음 " + quarryBatch(grade) + buffer + investment;
            case MINE -> prefix + "유한 광석 채굴 · 작업 주기 " + mineWorkPeriod(grade) + "틱" + buffer + investment;
            default -> "";
        };
    }

    private static int clampGrade(int grade) { return Math.max(1, Math.min(4, grade)); }
}
