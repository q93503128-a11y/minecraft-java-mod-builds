package dev.moonseungjun.augmentationsurvival.augment.utility;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

/** Server-authoritative probability transforms for real vanilla taming rolls. */
public final class TamingAugmentService {
    private static final String TAMING_KNOWHOW_ID = AugmentationSurvival.MOD_ID + ":taming_knack";
    private static final double[] MULTIPLIERS = {1.0D, 1.25D, 1.50D, 2.00D};

    private TamingAugmentService() {
    }

    public static void initialize() {
        AugmentCatalog.require(TAMING_KNOWHOW_ID);
    }

    /**
     * Preserves the species' original {@code nextInt(bound) == 0} success rule and adds only the
     * exact extra probability needed to reach the canonical multiplier.
     */
    public static int boostZeroRoll(Player player, RandomSource random, int bound) {
        int roll = random.nextInt(bound);
        double multiplier = multiplier(player);
        if (multiplier <= 1.0D || roll == 0 || bound <= 1) {
            return roll;
        }
        double base = 1.0D / bound;
        double desired = Math.min(1.0D, base * multiplier);
        double extraGivenFailure = (desired - base) / (1.0D - base);
        return random.nextDouble() < extraGivenFailure ? 0 : roll;
    }

    /**
     * Produces an integer threshold whose expected success probability is exactly
     * min(1, originalThreshold / bound * multiplier). Used by the horse-family temper roll.
     */
    public static int boostThreshold(Player player, RandomSource random, int originalThreshold, int bound) {
        if (bound <= 0 || originalThreshold <= 0) {
            return Math.max(0, originalThreshold);
        }
        double multiplier = multiplier(player);
        if (multiplier <= 1.0D) {
            return originalThreshold;
        }
        double desiredThreshold = Math.min((double) bound, originalThreshold * multiplier);
        int whole = (int) Math.floor(desiredThreshold);
        double fraction = desiredThreshold - whole;
        if (whole < bound && fraction > 0.0D && random.nextDouble() < fraction) {
            whole++;
        }
        return Math.min(bound, whole);
    }

    private static double multiplier(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return 1.0D;
        }
        AugmentationPlayerState state = AugmentationPlayerStateService.state(serverPlayer);
        int level = Math.max(0, Math.min(
                state.level(TAMING_KNOWHOW_ID),
                AugmentCatalog.require(TAMING_KNOWHOW_ID).maxLevel()
        ));
        return MULTIPLIERS[level];
    }
}
