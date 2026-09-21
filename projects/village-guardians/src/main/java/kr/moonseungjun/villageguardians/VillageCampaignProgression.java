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
        int safe = Math.max(1, day);
        int authored = Math.min(CAMPAIGN_END_DAY, safe);
        int base = (authored - 1) / 8 + Math.max(0, wave - 1) / 6;
        int lateBonus = authored <= 20 ? 0 : (authored - 20) / 20;
        int endlessBonus = safe <= CAMPAIGN_END_DAY
                ? 0 : (int) Math.floor(Math.sqrt(safe - CAMPAIGN_END_DAY) / 4.0);
        return Math.max(0, base + lateBonus + endlessBonus);
    }

    /**
     * Raw enemy body durability has no hard stat ceiling. The authored campaign grows linearly
     * to x2.0 at day 100; endless war keeps rising with a square-root tail so very long worlds
     * continue progressing without turning each extra day into another large stat jump.
     */
    public static float enemyBaseHealthMultiplier(int day) {
        int safe = Math.max(1, day);
        if (safe <= 20) return 1.0f;
        if (safe <= CAMPAIGN_END_DAY) return 1.0f + (safe - 20) / 80.0f;
        return 2.0f + (float) Math.sqrt(safe - CAMPAIGN_END_DAY) * 0.05f;
    }

    public static int enemyStrengthTier(int day, int wave) {
        int safe = Math.max(1, day);
        int authored = Math.min(CAMPAIGN_END_DAY, safe);
        int base = (authored - 1) / 18 + Math.max(0, wave - 3) / 5;
        int endlessBonus = safe <= CAMPAIGN_END_DAY
                ? 0 : (int) Math.floor(Math.sqrt(safe - CAMPAIGN_END_DAY) / 12.0);
        return Math.max(0, base + endlessBonus);
    }

    /**
     * Used for day-scaled special attacks. Day 21-100 keeps the authored 0.40x pace. Endless war
     * remains uncapped but switches to a square-root tail so ability damage grows indefinitely
     * without making late telegraphed attacks jump sharply from one night to the next.
     */
    public static float effectiveCombatDay(int day) {
        int safe = Math.max(1, day);
        if (safe <= 20) return safe;
        if (safe <= CAMPAIGN_END_DAY) return 20.0f + (safe - 20) * 0.40f;
        return 52.0f + (float) Math.sqrt(safe - CAMPAIGN_END_DAY) * 0.80f;
    }

    public static int enemyAbsorptionAmplifier(int day) {
        int safe = Math.max(1, day);
        if (safe < 30) return -1;
        if (safe <= CAMPAIGN_END_DAY) return Math.max(0, (safe - 20) / 30);
        return 2 + (int) Math.floor(Math.sqrt(safe - CAMPAIGN_END_DAY) / 10.0);
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
