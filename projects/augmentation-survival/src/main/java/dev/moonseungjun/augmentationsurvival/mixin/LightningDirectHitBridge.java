package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
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

/** One authoritative Lightning counter operation per successful root direct hit. */
public final class LightningDirectHitBridge {
    private static final String LIGHTNING_ID = AugmentationSurvival.MOD_ID + ":lightning";
    private static final ThreadLocal<Boolean> ROOT_MELEE_ATTACK = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private LightningDirectHitBridge() {
    }

    public static void enterRootMeleeAttack() {
        ROOT_MELEE_ATTACK.set(Boolean.TRUE);
    }

    public static void leaveRootMeleeAttack() {
        ROOT_MELEE_ATTACK.remove();
    }

    /** True while vanilla/Better Combat Player.attack is inside the wrapped hurt call. */
    public static boolean rootMeleeAttackInProgress() {
        return Boolean.TRUE.equals(ROOT_MELEE_ATTACK.get());
    }

    public static void onDirectHit(
            ServerPlayer attacker,
            LivingEntity primaryTarget,
            AugmentationPlayerState state,
            float actualDamage
    ) {
        int augmentLevel = Math.max(0, Math.min(3, state.level(LIGHTNING_ID)));
        if (augmentLevel <= 0 || primaryTarget == null || primaryTarget.isDeadOrDying()) return;

        ExpansionTuning.Snapshot tuning = ExpansionTuning.current();
        Map<UUID, Integer> counters = CombatExpansionServiceInvoker.augmentationSurvival$lightningHits();
        int trigger = tuning.lightningTriggerHits(augmentLevel);
        int previous = counters.getOrDefault(attacker.getUUID(), 0);
        int hits = Math.min(trigger, previous + 1);
        counters.put(attacker.getUUID(), hits);
        if (hits < trigger) return;

        if (previous < trigger) {
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
            counters.put(attacker.getUUID(), trigger);
            return;
        }

        counters.put(attacker.getUUID(), 0);
        float damage = (float) (Math.max(0.0F, actualDamage) * tuning.lightningDamageFraction(augmentLevel));
        Vec3 chainOrigin = primaryTarget.position().add(
                0.0D,
                Math.max(0.5D, primaryTarget.getBbHeight() * 0.55D),
                0.0D
        );
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
