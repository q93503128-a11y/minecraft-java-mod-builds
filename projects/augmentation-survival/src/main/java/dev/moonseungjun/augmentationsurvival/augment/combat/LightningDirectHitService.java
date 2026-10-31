package dev.moonseungjun.augmentationsurvival.augment.combat;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.presentation.LightningPresentationService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** One server-authoritative Lightning counter operation per successful root-direct hit. */
public final class LightningDirectHitService {
    private static final String LIGHTNING_ID = AugmentationSurvival.MOD_ID + ":lightning";
    private static final ThreadLocal<Boolean> ROOT_MELEE_ATTACK = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final Map<UUID, Integer> HITS = new HashMap<>();
    private static boolean initialized;

    private LightningDirectHitService() {
    }

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        ServerPlayerEvents.JOIN.register(LightningDirectHitService::clearPlayer);
        ServerPlayerEvents.LEAVE.register(LightningDirectHitService::clearPlayer);
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> clearPlayer(newPlayer));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> HITS.clear());
    }

    public static void enterRootMeleeAttack() {
        ROOT_MELEE_ATTACK.set(Boolean.TRUE);
    }

    public static void leaveRootMeleeAttack() {
        ROOT_MELEE_ATTACK.remove();
    }

    /** True only while Player.attack is inside the wrapped root-melee hurt call. */
    public static boolean rootMeleeAttackInProgress() {
        return Boolean.TRUE.equals(ROOT_MELEE_ATTACK.get());
    }

    public static int hitCount(ServerPlayer player) {
        return player == null ? 0 : HITS.getOrDefault(player.getUUID(), 0);
    }

    public static void onDirectHit(
            ServerPlayer attacker,
            LivingEntity primaryTarget,
            AugmentationPlayerState state,
            float actualDamage
    ) {
        int augmentLevel = Math.max(0, Math.min(3, state.level(LIGHTNING_ID)));
        if (augmentLevel <= 0 || primaryTarget == null) return;

        ExpansionTuning.Snapshot tuning = ExpansionTuning.current();
        int trigger = tuning.lightningTriggerHits(augmentLevel);
        int previous = HITS.getOrDefault(attacker.getUUID(), 0);
        int hits = Math.min(trigger, previous + 1);
        HITS.put(attacker.getUUID(), hits);
        if (hits < trigger) return;

        if (previous < trigger) {
            // One-target fights still get a readable threshold strike. This bolt is visual-only.
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
            // Stay primed until a later direct hit has a valid secondary target.
            HITS.put(attacker.getUUID(), trigger);
            return;
        }

        HITS.put(attacker.getUUID(), 0);
        float damage = (float) (Math.max(0.0F, actualDamage) * tuning.lightningDamageFraction(augmentLevel));
        Vec3 chainOrigin = primaryTarget.position().add(
                0.0D,
                Math.max(0.5D, primaryTarget.getBbHeight() * 0.55D),
                0.0D
        );
        for (int index = 0; index < limit; index++) {
            LivingEntity target = candidates.get(index);
            applyDerivedDamage(attacker, target, damage);
            LightningPresentationService.chainStrike(attacker, chainOrigin, target);
        }
    }

    private static void applyDerivedDamage(ServerPlayer attacker, LivingEntity target, float amount) {
        if (!(amount > 0.0F) || !Float.isFinite(amount) || target.isDeadOrDying()) {
            return;
        }
        DamageSource source = attacker.damageSources().playerAttack(attacker);
        CombatProvenanceContext.runAs(
                CombatProvenance.DERIVED_CHAIN,
                () -> target.hurtOrSimulate(source, amount)
        );
    }

    private static void clearPlayer(ServerPlayer player) {
        if (player != null) HITS.remove(player.getUUID());
    }
}
