package kr.moonseungjun.villageguardians;

/**
 * Shared 100-day campaign pacing contract.
 *
 * Day controls when new pressure layers appear, while raw stat growth is deliberately slower
 * than the old 20-day curve. Player XP and equipment generations reference the same timeline so
 * the campaign cannot outgrow its rewards.
 */
public final class VillageCampaignProgression {
    public static final int CAMPAIGN_END_DAY = 100;

    private VillageCampaignProgression() {}

    public static int campaignDay(int day) {
        return Math.max(1, Math.min(CAMPAIGN_END_DAY, day));
    }

    /**
     * Expected player level at the end of a day. The first twenty days preserve the old Lv.30
     * destination; the remaining eighty days accelerate visible progression toward Lv.300.
     */
    public static int targetPlayerLevel(int day) {
        int safe = campaignDay(day);
        if (safe <= 20) {
            return 1 + Math.round((safe - 1) * 29.0f / 19.0f);
        }
        return Math.min(RpgProgress.MAX_LEVEL,
                30 + Math.round((safe - 20) * 270.0f / 80.0f));
    }

    /**
     * Number of visible levels the authored campaign expects to gain on this day.
     * Day 21+ intentionally averages roughly three to four levels so level-ups remain frequent.
     */
    public static int targetLevelsForDay(int day) {
        int safe = campaignDay(day);
        if (safe <= 1) return 0;
        return Math.max(1, targetPlayerLevel(safe) - targetPlayerLevel(safe - 1));
    }

    /**
     * Opening-campaign threat budget retained for the established Lv.1-30 curve.
     * Day 21+ XP is normalized against the whole planned night's roster in VillageRaidSystem
     * so seven-wave late nights and multiplayer roster scaling do not accelerate leveling.
     */
    public static int expectedThreatsPerLevel(int day) {
        int safe = campaignDay(day);
        return Math.min(115, 52 + safe / 2);
    }

    /**
     * Daily stat tiers rise much slower than the former day/3 and day/5 potion ladder.
     * New enemy roles, wave doctrines, fronts and bosses provide most of the later difficulty.
     */
    public static int enemyHealthTier(int day, int wave) {
        int safe = campaignDay(day);
        int base = (safe - 1) / 8 + Math.max(0, wave - 1) / 6;
        int lateBonus = safe <= 20 ? 0 : (safe - 20) / 20;
        return Math.min(14, Math.max(0, base + lateBonus));
    }

    /**
     * Raw enemy body durability keeps growing through day 100 instead of flattening when
     * Health Boost tiers approach their cap. The opening twenty days remain unchanged.
     */
    public static float enemyBaseHealthMultiplier(int day) {
        int safe = campaignDay(day);
        if (safe <= 20) return 1.0f;
        return 1.0f + (safe - 20) / 80.0f;
    }

    public static int enemyStrengthTier(int day, int wave) {
        int safe = campaignDay(day);
        return Math.min(5, Math.max(0, (safe - 1) / 18 + Math.max(0, wave - 3) / 5));
    }

    /**
     * Used for day-scaled special attacks. After day 20, eighty real days contribute only
     * thirty-two extra effective days, keeping unavoidable-looking ability damage readable.
     */
    public static float effectiveCombatDay(int day) {
        int safe = campaignDay(day);
        if (safe <= 20) return safe;
        return 20.0f + (safe - 20) * 0.40f;
    }

    /**
     * Roster growth is intentionally bounded. Late difficulty comes from composition, fronts
     * and mechanics rather than hundreds of disposable entities.
     */
    public static int rosterDayBonus(int day) {
        int safe = campaignDay(day);
        return Math.min(36, 4 + safe / 2);
    }

    public static int equipmentTierForDay(int day) {
        int safe = campaignDay(day);
        if (safe >= 90) return 10;
        if (safe >= 80) return 9;
        if (safe >= 70) return 8;
        if (safe >= 55) return 7;
        if (safe >= 40) return 6;
        if (safe >= 25) return 5;
        if (safe >= 15) return 4;
        if (safe >= 10) return 3;
        if (safe >= 5) return 2;
        return 1;
    }

    public static boolean isFinalSiege(int day) {
        return day == CAMPAIGN_END_DAY;
    }

    public static int promotionTierForLevel(int level) {
        int safe = Math.max(1, Math.min(RpgProgress.MAX_LEVEL, level));
        if (safe >= 60) return 2;
        if (safe >= 30) return 1;
        return 0;
    }
}
