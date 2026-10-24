package dev.moonseungjun.augmentationsurvival.augment.combat;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import net.minecraft.server.level.ServerPlayer;

/** Server-owned entry point for augment-generated, non-natural healing. */
public final class AugmentHealingService {
    private static final String HEALING_AMPLIFICATION_ID =
            AugmentationSurvival.MOD_ID + ":healing_amplification";

    private AugmentHealingService() {
    }

    /**
     * Applies a primary non-natural heal. This path is intentionally reusable by lifesteal,
     * healing-wave and later active effects. Natural hunger/saturation regeneration must not call it.
     */
    public static float healPrimary(ServerPlayer player, float baseAmount) {
        if (!(baseAmount > 0.0F) || !Float.isFinite(baseAmount) || player.isDeadOrDying()) {
            return 0.0F;
        }
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        int level = boundedLevel(state, HEALING_AMPLIFICATION_ID);
        float amplified = baseAmount * (float) (
                1.0D + CombatTuning.current().healingAmplificationPerLevel() * level
        );
        float before = player.getHealth();
        player.heal(amplified);
        return Math.max(0.0F, player.getHealth() - before);
    }

    private static int boundedLevel(AugmentationPlayerState state, String augmentId) {
        int level = state.level(augmentId);
        if (level <= 0) {
            return 0;
        }
        return Math.min(level, AugmentCatalog.require(augmentId).maxLevel());
    }
}
