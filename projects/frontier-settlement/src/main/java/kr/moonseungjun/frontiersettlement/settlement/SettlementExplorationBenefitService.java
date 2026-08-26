package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.server.MinecraftServer;

import java.util.Locale;

/**
 * Deterministic exploration-to-settlement feedback.
 * First-time survey/conquest metadata never becomes currency or item authority: it only improves
 * existing physical market, maintenance, forging, military and outpost systems.
 *
 * Alpha.75 also interprets the path of already-recorded external structure ids into a few broad
 * gameplay archetypes. This is deliberately a soft bridge: no companion class, registry constant,
 * loot table or asset is hard-referenced, and an unknown structure simply keeps the generic survey
 * benefit it already had before this pass.
 */
public final class SettlementExplorationBenefitService {
    public static final int MAX_SURVEY_LEVEL = 3;
    public static final int MAX_CONQUEST_LEVEL = 2;
    public static final int MAX_THREAT_LEVEL = 3;
    public static final int MAX_STRUCTURE_ARCHETYPE_LEVEL = 2;
    public static final long RECRUIT_FOOD_DISCOUNT_PER_THREAT = 1L;
    public static final double BARRACKS_THREAT_RADIUS_BONUS_PER_LEVEL = 4.0D;
    public static final double BARRACKS_FORTIFIED_RADIUS_BONUS_PER_LEVEL = 2.0D;
    public static final long OUTPOST_WOOD_DISCOUNT_PER_CONQUEST = 4L;
    public static final long OUTPOST_STONE_DISCOUNT_PER_CONQUEST = 2L;
    public static final long OUTPOST_STONE_DISCOUNT_PER_FORTIFIED = 1L;
    public static final int MARKET_EMERALD_BONUS_PER_SURVEY = 1;
    public static final int MARKET_EMERALD_BONUS_PER_CONQUEST = 2;
    public static final int MARKET_EMERALD_BONUS_PER_TRADE = 1;
    public static final int REPAIR_BONUS_PER_SURVEY = 16;
    public static final int REPAIR_BONUS_PER_CONQUEST = 8;
    public static final int REPAIR_BONUS_PER_INDUSTRIAL = 12;
    public static final int FORGE_POWER_BONUS_PER_SURVEY = 2;
    public static final int FORGE_POWER_BONUS_PER_CONQUEST = 2;
    public static final int FORGE_POWER_BONUS_PER_RELIC = 2;

    private enum StructureArchetype {
        FORTIFIED,
        TRADE,
        INDUSTRIAL,
        RELIC,
        OTHER
    }

    private SettlementExplorationBenefitService() {}

    public static int surveyLevel(SettlementData data) {
        return Math.min(MAX_SURVEY_LEVEL, data.discoveredExternalStructures().size());
    }

    public static int conquestLevel(SettlementData data) {
        return Math.min(MAX_CONQUEST_LEVEL, data.defeatedExternalBosses().size());
    }

    public static int threatLevel(MinecraftServer server) {
        return SettlementThreatKnowledgeData.get(server).threatLevel();
    }

    public static int fortifiedKnowledge(SettlementData data) {
        return archetypeLevel(data, StructureArchetype.FORTIFIED);
    }

    public static int tradeKnowledge(SettlementData data) {
        return archetypeLevel(data, StructureArchetype.TRADE);
    }

    public static int industrialKnowledge(SettlementData data) {
        return archetypeLevel(data, StructureArchetype.INDUSTRIAL);
    }

    public static int relicKnowledge(SettlementData data) {
        return archetypeLevel(data, StructureArchetype.RELIC);
    }

    public static long barracksRecruitFoodCost(MinecraftServer server) {
        return Math.max(5L, SettlementBarracksService.RECRUIT_FOOD_COST
                - threatLevel(server) * RECRUIT_FOOD_DISCOUNT_PER_THREAT);
    }

    public static double barracksThreatRadius(MinecraftServer server) {
        SettlementData data = SettlementData.get(server);
        return SettlementBarracksService.BASE_THREAT_RADIUS
                + threatLevel(server) * BARRACKS_THREAT_RADIUS_BONUS_PER_LEVEL
                + fortifiedKnowledge(data) * BARRACKS_FORTIFIED_RADIUS_BONUS_PER_LEVEL;
    }

    public static String threatSupportSummary(MinecraftServer server) {
        SettlementData data = SettlementData.get(server);
        return "위협정보 " + threatLevel(server) + "/" + MAX_THREAT_LEVEL
                + " · 요새지식 " + fortifiedKnowledge(data) + "/" + MAX_STRUCTURE_ARCHETYPE_LEVEL
                + " · 병사 식량 " + barracksRecruitFoodCost(server)
                + " · 감시반경 " + (int) barracksThreatRadius(server);
    }

    public static long outpostWoodCost(SettlementData data) {
        return SettlementOutpostService.WOOD_COST - conquestLevel(data) * OUTPOST_WOOD_DISCOUNT_PER_CONQUEST;
    }

    public static long outpostStoneCost(SettlementData data) {
        return SettlementOutpostService.STONE_COST
                - conquestLevel(data) * OUTPOST_STONE_DISCOUNT_PER_CONQUEST
                - fortifiedKnowledge(data) * OUTPOST_STONE_DISCOUNT_PER_FORTIFIED;
    }

    public static int marketPayoutBonus(SettlementData data) {
        return surveyLevel(data) * MARKET_EMERALD_BONUS_PER_SURVEY
                + conquestLevel(data) * MARKET_EMERALD_BONUS_PER_CONQUEST
                + tradeKnowledge(data) * MARKET_EMERALD_BONUS_PER_TRADE;
    }

    public static int marketPayout(SettlementData data, int basePayout) {
        return Math.max(0, basePayout) + marketPayoutBonus(data);
    }

    public static int repairPerMetal(SettlementData data) {
        return SettlementWorkshopService.BASE_REPAIR_PER_METAL
                + surveyLevel(data) * REPAIR_BONUS_PER_SURVEY
                + conquestLevel(data) * REPAIR_BONUS_PER_CONQUEST
                + industrialKnowledge(data) * REPAIR_BONUS_PER_INDUSTRIAL;
    }

    public static int forgePower(SettlementData data) {
        return SettlementAdvancedWorkshopService.ENCHANTMENT_POWER
                + surveyLevel(data) * FORGE_POWER_BONUS_PER_SURVEY
                + conquestLevel(data) * FORGE_POWER_BONUS_PER_CONQUEST
                + relicKnowledge(data) * FORGE_POWER_BONUS_PER_RELIC;
    }

    public static int reforgePower(SettlementData data) {
        return SettlementAdvancedWorkshopService.REFORGE_POWER
                + surveyLevel(data) * FORGE_POWER_BONUS_PER_SURVEY
                + conquestLevel(data) * FORGE_POWER_BONUS_PER_CONQUEST
                + relicKnowledge(data) * FORGE_POWER_BONUS_PER_RELIC;
    }

    public static String supportSummary(SettlementData data) {
        return "조사 " + surveyLevel(data) + "/" + MAX_SURVEY_LEVEL
                + " · 정복 " + conquestLevel(data) + "/" + MAX_CONQUEST_LEVEL
                + " · 구조지식 요새" + fortifiedKnowledge(data)
                + "/교역" + tradeKnowledge(data)
                + "/산업" + industrialKnowledge(data)
                + "/유적" + relicKnowledge(data)
                + " · 시장 +" + marketPayoutBonus(data)
                + " · 수리 " + repairPerMetal(data) + "/금속"
                + " · 제작 " + forgePower(data) + "/" + reforgePower(data);
    }

    public static int oreEvidenceBonus(SettlementData data) { return surveyLevel(data) >= 2 ? 1 : 0; }
    public static int logEvidenceBonus(SettlementData data) { return surveyLevel(data) * 2; }
    public static int fieldEvidenceBonus(SettlementData data) { return surveyLevel(data) * 8; }
    public static int stoneEvidenceBonus(SettlementData data) { return surveyLevel(data) * 2; }

    private static int archetypeLevel(SettlementData data, StructureArchetype archetype) {
        int count = 0;
        for (String id : data.discoveredExternalStructures()) {
            if (classifyStructure(id) != archetype) continue;
            count++;
            if (count >= MAX_STRUCTURE_ARCHETYPE_LEVEL) return MAX_STRUCTURE_ARCHETYPE_LEVEL;
        }
        return count;
    }

    private static StructureArchetype classifyStructure(String rawId) {
        if (rawId == null || rawId.isBlank()) return StructureArchetype.OTHER;
        String id = rawId.toLowerCase(Locale.ROOT);
        int separator = id.indexOf(':');
        String path = separator >= 0 && separator + 1 < id.length() ? id.substring(separator + 1) : id;

        if (containsAny(path, "fortress", "fort_", "_fort", "castle", "keep", "stronghold", "outpost", "watchtower", "watch_tower", "bastion")) {
            return StructureArchetype.FORTIFIED;
        }
        if (containsAny(path, "tavern", "inn", "market", "village", "town", "trading", "merchant", "caravan")) {
            return StructureArchetype.TRADE;
        }
        if (containsAny(path, "mine", "mineshaft", "quarry", "forge", "smith", "workshop", "factory", "mill")) {
            return StructureArchetype.INDUSTRIAL;
        }
        if (containsAny(path, "temple", "shrine", "ruin", "dungeon", "crypt", "tomb", "catacomb", "pyramid", "sanctum", "altar")) {
            return StructureArchetype.RELIC;
        }
        return StructureArchetype.OTHER;
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) if (value.contains(needle)) return true;
        return false;
    }
}
