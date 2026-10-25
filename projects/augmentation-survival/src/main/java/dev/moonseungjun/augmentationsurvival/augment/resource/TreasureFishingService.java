package dev.moonseungjun.augmentationsurvival.augment.resource;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/** Exact absolute treasure-chance correction for vanilla 26.2 fishing selection. */
public final class TreasureFishingService {
    private static final String TREASURE_FISHING_ID = AugmentationSurvival.MOD_ID + ":treasure_fishing";

    private TreasureFishingService() {
    }

    public static void initialize() {
        AugmentCatalog.require(TREASURE_FISHING_ID);
    }

    /**
     * Performs an extra pre-roll so the combined result is exactly vanilla treasure chance plus
     * the canonical +2/+4/+6 percentage points. Vanilla's open-water eligibility is preserved.
     */
    public static boolean shouldForceTreasure(
            ServerPlayer player,
            float lootLuck,
            boolean openWater,
            RandomSource random
    ) {
        if (!openWater) {
            return false;
        }
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        int level = Math.max(0, Math.min(
                state.level(TREASURE_FISHING_ID),
                AugmentCatalog.require(TREASURE_FISHING_ID).maxLevel()
        ));
        if (level <= 0) {
            return false;
        }

        double bonus = ConsumptionAugmentTuning.current().treasureChanceBonus(level);
        if (bonus <= 0.0D) {
            return false;
        }

        int junkWeight = Math.max(Mth.floor(10.0F - 2.0F * lootLuck), 0);
        int treasureWeight = Math.max(Mth.floor(5.0F + 2.0F * lootLuck), 0);
        int fishWeight = Math.max(Mth.floor(85.0F - lootLuck), 0);
        int total = junkWeight + treasureWeight + fishWeight;
        if (total <= 0) {
            return false;
        }

        double baseChance = (double) treasureWeight / total;
        double desiredChance = Math.min(1.0D, baseChance + bonus);
        if (desiredChance <= baseChance || baseChance >= 1.0D) {
            return false;
        }

        double forceChance = (desiredChance - baseChance) / (1.0D - baseChance);
        return random.nextDouble() < forceChance;
    }
}
