package dev.moonseungjun.fishinggame.profile;

import dev.moonseungjun.fishinggame.progression.FishingRods;

public final class FishingPrestige {
    public static final int BASE_REBIRTH_COST = 10_000;
    public static final int REBIRTH_COST_STEP = 1_500;
    public static final double SALE_BONUS_PER_REBIRTH = 0.30;
    public static final int MAX_REBIRTHS = 999;

    private FishingPrestige() {
    }

    public static int nextCost(int rebirths) {
        long clamped = Math.max(0, Math.min(MAX_REBIRTHS, rebirths));
        return (int) Math.min(Integer.MAX_VALUE, BASE_REBIRTH_COST + clamped * REBIRTH_COST_STEP);
    }

    public static double saleMultiplier(int rebirths) {
        int clamped = Math.max(0, Math.min(MAX_REBIRTHS, rebirths));
        return 1.0 + clamped * SALE_BONUS_PER_REBIRTH;
    }

    public static int boostedSaleValue(int baseValue, int rebirths) {
        long boosted = Math.round(Math.max(0, baseValue) * saleMultiplier(rebirths));
        return (int) Math.min(Integer.MAX_VALUE, boosted);
    }

    public static boolean canRebirth(PlayerFishingProfile profile) {
        return profile.rodTier() >= FishingRods.maxTier()
                && profile.catches().isEmpty()
                && profile.coins() >= nextCost(profile.rebirths())
                && profile.rebirths() < MAX_REBIRTHS;
    }
}
