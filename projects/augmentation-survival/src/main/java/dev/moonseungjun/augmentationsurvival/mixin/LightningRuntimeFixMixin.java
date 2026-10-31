package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatExpansionService;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenance;
import dev.moonseungjun.augmentationsurvival.augment.combat.ExpansionTuning;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.presentation.LightningPresentationService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Repairs Lightning's live proc contract: reaching threshold is always visible on the primary
 * target, while chain damage is consumed only when an additional hostile target exists.
 */
@Mixin(value = CombatExpansionService.class, remap = false)
abstract class LightningRuntimeFixMixin {
    private static final String LIGHTNING_ID = AugmentationSurvival.MOD_ID + ":lightning";

    @Inject(method = "triggerLightning", at = @At("HEAD"), cancellable = true)
    private static void augmentationSurvival$repairLightning(
            ServerPlayer attacker,
            LivingEntity primaryTarget,
            AugmentationPlayerState state,
            float actualDamage,
            CallbackInfo ci
    ) {
        int augmentLevel = Math.max(0, Math.min(3, state.level(LIGHTNING_ID)));
        if (augmentLevel <= 0) return;
        ci.cancel();

        ExpansionTuning.Snapshot tuning = ExpansionTuning.current();
        Map<UUID, Integer> counters = CombatExpansionServiceInvoker.augmentationSurvival$lightningHits();
        int trigger = tuning.lightningTriggerHits(augmentLevel);
        int previous = counters.getOrDefault(attacker.getUUID(), 0);
        int hits = Math.min(trigger, previous + 1);
        counters.put(attacker.getUUID(), hits);
        if (hits < trigger) return;

        boolean newlyPrimed = previous < trigger;
        if (newlyPrimed) {
            // The threshold itself must be readable even in a one-target test. Presentation only:
            // the actual augment damage still follows the documented additional-target chain rule.
            LightningPresentationService.primaryThresholdStrike(attacker, primaryTarget);
        }

        double radius = tuning.lightningRadius();
        double radiusSquared = radius * radius;
        List<LivingEntity> candidates = attacker.level().getEntitiesOfClass(
                LivingEntity.class,
                primaryTarget.getBoundingBox().inflate(radius),
                candidate -> candidate != primaryTarget
                        && CompanionAugmentService.isHostileTarget(attacker, candidate)
                        && primaryTarget.distanceToSqr(candidate) <= radiusSquared
        );
        candidates.sort(Comparator.comparingDouble(primaryTarget::distanceToSqr));
        int limit = Math.min(tuning.lightningTargetCount(augmentLevel), candidates.size());
        if (limit <= 0) {
            // Stay primed. The next valid direct hit can discharge as soon as a chain target exists.
            counters.put(attacker.getUUID(), trigger);
            return;
        }

        counters.put(attacker.getUUID(), 0);
        float damage = (float) (actualDamage * tuning.lightningDamageFraction(augmentLevel));
        Vec3 chainOrigin = primaryTarget.position().add(0.0D, Math.max(0.5D, primaryTarget.getBbHeight() * 0.55D), 0.0D);
        for (int index = 0; index < limit; index++) {
            LivingEntity target = candidates.get(index);
            CombatExpansionServiceInvoker.augmentationSurvival$applyDerivedDamage(
                    attacker,
                    target,
                    damage,
                    CombatProvenance.DERIVED_CHAIN
            );
            LightningPresentationService.chainStrike(attacker, chainOrigin, target);
        }
    }
}
