package kr.moonseungjun.frontiersettlement.settlement;

/**
 * Existing production buildings scale vertically with settlement maturity instead of requiring
 * duplicate footprints. This state is deliberately derived from the canonical settlement tier:
 * no parallel save ledger, upgrade currency, hidden worker count or extra placement authority.
 */
public final class SettlementProductionEfficiencyService {
    private SettlementProductionEfficiencyService() {}

    public static int grade(SettlementData data) {
        return switch (SettlementTier.current(data)) {
            case CAMP, HAMLET -> 1;
            case VILLAGE -> 2;
            case FRONTIER_TOWN -> 3;
            case DOMAIN, FRONTIER_CAPITAL -> 4;
        };
    }

    public static String gradeLabel(int grade) {
        return switch (clampGrade(grade)) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> "IV";
        };
    }

    public static int lumberWorkPeriod(int grade) {
        return switch (clampGrade(grade)) { case 1 -> 100; case 2 -> 90; case 3 -> 80; default -> 70; };
    }

    public static int lumberBatch(int grade) {
        return switch (clampGrade(grade)) { case 1 -> 16; case 2 -> 20; case 3 -> 24; default -> 32; };
    }

    public static int farmWorkPeriod(int grade) {
        return switch (clampGrade(grade)) { case 1 -> 120; case 2 -> 100; case 3 -> 80; default -> 80; };
    }

    /** Number of deterministic crop cohorts; one cohort receives +1 age each tending pass. */
    public static int farmGrowthModulo(int grade) {
        return switch (clampGrade(grade)) { case 1 -> 3; case 2, 3 -> 2; default -> 1; };
    }

    public static int quarryWorkPeriod(int grade) {
        return switch (clampGrade(grade)) { case 1 -> 80; case 2 -> 70; case 3 -> 60; default -> 50; };
    }

    public static int quarryBatch(int grade) {
        return switch (clampGrade(grade)) { case 1 -> 16; case 2 -> 20; case 3 -> 24; default -> 32; };
    }

    public static int mineWorkPeriod(int grade) {
        return switch (clampGrade(grade)) { case 1 -> 160; case 2 -> 130; case 3 -> 100; default -> 80; };
    }

    public static String detail(BuildingType type, SettlementData data) {
        int grade = grade(data);
        String prefix = "개량 " + gradeLabel(grade) + " · ";
        return switch (type) {
            case LUMBER_CAMP -> prefix + "자동 벌목 · 작업 묶음 " + lumberBatch(grade);
            case FARM -> prefix + "자동 식량 생산 · 작물 성장 관리 "
                    + (farmGrowthModulo(grade) == 1 ? "전 구획" : farmGrowthModulo(grade) + "구획 순환");
            case QUARRY -> prefix + "자동 채석 · 작업 묶음 " + quarryBatch(grade);
            case MINE -> prefix + "유한 광석 채굴 · 작업 주기 " + mineWorkPeriod(grade) + "틱";
            default -> "";
        };
    }

    private static int clampGrade(int grade) {
        return Math.max(1, Math.min(4, grade));
    }
}
