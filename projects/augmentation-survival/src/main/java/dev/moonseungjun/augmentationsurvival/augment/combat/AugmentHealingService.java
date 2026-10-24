package dev.moonseungjun.augmentationsurvival.augment.combat;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;

/** Server-owned entry point for augment-generated, non-natural healing. */
public final class AugmentHealingService {
    private static final String HEALING_AMPLIFICATION_ID = id("healing_amplification");
    private static final String OVERFLOW_HEALING_ID = id("overflow_healing");
    private static final String HEALING_SHARE_ID = id("healing_share");

    private AugmentHealingService() {
    }

    /**
     * Applies a primary non-natural heal using the canonical order:
     * amplification -> actual HP -> overflow absorption -> healing share.
     */
    public static float healPrimary(ServerPlayer player, float baseAmount) {
        if (!(baseAmount > 0.0F) || !Float.isFinite(baseAmount) || player.isDeadOrDying()) {
            return 0.0F;
        }

        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        float amplified = amplify(player, state, baseAmount);
        float before = player.getHealth();
        player.heal(amplified);
        float actualHp = Math.max(0.0F, player.getHealth() - before);

        float overflow = Math.max(0.0F, amplified - actualHp);
        int overflowLevel = boundedLevel(state, OVERFLOW_HEALING_ID);
        if (overflowLevel > 0 && overflow > 0.0F) {
            float cap = (float) (
                    CombatTuning.current().overflowHealingAbsorptionCapPerLevel() * overflowLevel
            );
            ProtectionAugmentService.grantAbsorption(
                    player,
                    ProtectionAugmentService.OVERFLOW_HEALING_POOL,
                    overflow,
                    cap
            );
        }

        int shareLevel = boundedLevel(state, HEALING_SHARE_ID);
        if (shareLevel > 0 && actualHp > 0.0F) {
            shareActualHealing(player, actualHp, shareLevel);
        }
        return actualHp;
    }

    /** Shared heals may use the recipient's amplification, but never overflow or share again. */
    public static float healShared(LivingEntity target, float baseAmount) {
        if (!(baseAmount > 0.0F) || !Float.isFinite(baseAmount) || target.isDeadOrDying()) {
            return 0.0F;
        }
        float amount = baseAmount;
        if (target instanceof ServerPlayer player) {
            amount = amplify(player, AugmentationPlayerStateService.state(player), baseAmount);
        }
        float before = target.getHealth();
        target.heal(amount);
        return Math.max(0.0F, target.getHealth() - before);
    }

    private static void shareActualHealing(
            ServerPlayer source,
            float actualHp,
            int shareLevel
    ) {
        CombatTuning.Snapshot tuning = CombatTuning.current();
        float sharedAmount = (float) (actualHp * tuning.healingSharePerLevel() * shareLevel);
        if (!(sharedAmount > 0.0F)) {
            return;
        }

        double radius = tuning.healingShareRadius();
        double radiusSquared = radius * radius;
        ServerLevel level = (ServerLevel) source.level();
        List<LivingEntity> nearby = level.getEntitiesOfClass(
                LivingEntity.class,
                source.getBoundingBox().inflate(radius),
                candidate -> candidate != source
                        && !candidate.isDeadOrDying()
                        && source.distanceToSqr(candidate) <= radiusSquared
                        && isHealingAlly(source, candidate)
        );
        for (LivingEntity target : nearby) {
            healShared(target, sharedAmount);
        }
    }

    private static boolean isHealingAlly(ServerPlayer source, LivingEntity candidate) {
        if (source.isAlliedTo(candidate)) {
            return true;
        }
        return candidate instanceof TamableAnimal tamable
                && tamable.isTame()
                && tamable.isOwnedBy(source);
    }

    private static float amplify(
            ServerPlayer player,
            AugmentationPlayerState state,
            float baseAmount
    ) {
        int level = boundedLevel(state, HEALING_AMPLIFICATION_ID);
        double multiplier = 1.0D + CombatTuning.current().healingAmplificationPerLevel() * level;
        double amplified = baseAmount * multiplier;
        if (!Double.isFinite(amplified)) {
            return baseAmount;
        }
        return (float) Math.min(Float.MAX_VALUE, amplified);
    }

    private static int boundedLevel(AugmentationPlayerState state, String augmentId) {
        int level = state.level(augmentId);
        if (level <= 0) {
            return 0;
        }
        return Math.min(level, AugmentCatalog.require(augmentId).maxLevel());
    }

    private static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }
}
