package kr.moonseungjun.frontiersettlement.settlement;

/**
 * Deterministic exploration-to-settlement feedback.
 * First-time survey/conquest metadata never becomes currency or item authority: it only improves
 * existing physical market, maintenance, forging and outpost systems.
 */
public final class SettlementExplorationBenefitService {
    public static final int MAX_SURVEY_LEVEL = 3;
    public static final int MAX_CONQUEST_LEVEL = 2;
    public static final long OUTPOST_WOOD_DISCOUNT_PER_CONQUEST = 4L;
    public static final long OUTPOST_STONE_DISCOUNT_PER_CONQUEST = 2L;
    public static final int MARKET_EMERALD_BONUS_PER_SURVEY = 1;
    public static final int MARKET_EMERALD_BONUS_PER_CONQUEST = 2;
    public static final int REPAIR_BONUS_PER_SURVEY = 16;
    public static final int REPAIR_BONUS_PER_CONQUEST = 8;
    public static final int FORGE_POWER_BONUS_PER_SURVEY = 2;
    public static final int FORGE_POWER_BONUS_PER_CONQUEST = 2;

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

    public static int marketPayoutBonus(SettlementData data) {
        return surveyLevel(data) * MARKET_EMERALD_BONUS_PER_SURVEY
                + conquestLevel(data) * MARKET_EMERALD_BONUS_PER_CONQUEST;
    }

    public static int marketPayout(SettlementData data, int basePayout) {
        return Math.max(0, basePayout) + marketPayoutBonus(data);
    }

    public static int repairPerMetal(SettlementData data) {
        return SettlementWorkshopService.BASE_REPAIR_PER_METAL
                + surveyLevel(data) * REPAIR_BONUS_PER_SURVEY
                + conquestLevel(data) * REPAIR_BONUS_PER_CONQUEST;
    }

    public static int forgePower(SettlementData data) {
        return SettlementAdvancedWorkshopService.ENCHANTMENT_POWER
                + surveyLevel(data) * FORGE_POWER_BONUS_PER_SURVEY
                + conquestLevel(data) * FORGE_POWER_BONUS_PER_CONQUEST;
    }

    public static int reforgePower(SettlementData data) {
        return SettlementAdvancedWorkshopService.REFORGE_POWER
                + surveyLevel(data) * FORGE_POWER_BONUS_PER_SURVEY
                + conquestLevel(data) * FORGE_POWER_BONUS_PER_CONQUEST;
    }

    public static String supportSummary(SettlementData data) {
        return "조사 " + surveyLevel(data) + "/" + MAX_SURVEY_LEVEL
                + " · 정복 " + conquestLevel(data) + "/" + MAX_CONQUEST_LEVEL
                + " · 시장 +" + marketPayoutBonus(data)
                + " · 수리 " + repairPerMetal(data) + "/금속"
                + " · 제작 " + forgePower(data) + "/" + reforgePower(data);
    }

    public static int oreEvidenceBonus(SettlementData data) { return surveyLevel(data) >= 2 ? 1 : 0; }
    public static int logEvidenceBonus(SettlementData data) { return surveyLevel(data) * 2; }
    public static int fieldEvidenceBonus(SettlementData data) { return surveyLevel(data) * 8; }
    public static int stoneEvidenceBonus(SettlementData data) { return surveyLevel(data) * 2; }
}
