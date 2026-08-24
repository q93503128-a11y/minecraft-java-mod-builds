package kr.moonseungjun.frontiersettlement.settlement;

/**
 * Small, deterministic settlement benefits derived only from Alpha.45 first-time milestones.
 * No new currency, inventory, reward chest, world scan or saved authority is introduced.
 */
public final class SettlementExplorationBenefitService {
    public static final int MAX_SURVEY_LEVEL = 3;
    public static final int MAX_CONQUEST_LEVEL = 2;
    public static final long OUTPOST_WOOD_DISCOUNT_PER_CONQUEST = 4L;
    public static final long OUTPOST_STONE_DISCOUNT_PER_CONQUEST = 2L;

    private SettlementExplorationBenefitService() {}

    public static int surveyLevel(SettlementData data) {
        return Math.min(MAX_SURVEY_LEVEL, data.discoveredExternalStructures().size());
    }

    public static int conquestLevel(SettlementData data) {
        return Math.min(MAX_CONQUEST_LEVEL, data.defeatedExternalBosses().size());
    }

    public static long outpostWoodCost(SettlementData data) {
        return SettlementOutpostService.WOOD_COST - conquestLevel(data) * OUTPOST_WOOD_DISCOUNT_PER_CONQUEST;
    }

    public static long outpostStoneCost(SettlementData data) {
        return SettlementOutpostService.STONE_COST - conquestLevel(data) * OUTPOST_STONE_DISCOUNT_PER_CONQUEST;
    }

    public static int oreEvidenceBonus(SettlementData data) {
        return surveyLevel(data) >= 2 ? 1 : 0;
    }

    public static int logEvidenceBonus(SettlementData data) {
        return surveyLevel(data) * 2;
    }

    public static int fieldEvidenceBonus(SettlementData data) {
        return surveyLevel(data) * 8;
    }

    public static int stoneEvidenceBonus(SettlementData data) {
        return surveyLevel(data) * 2;
    }
}
