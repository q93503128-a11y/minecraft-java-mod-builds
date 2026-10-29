package dev.moonseungjun.augmentationsurvival.augment;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenance;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.combat.OffensiveTargetContext;
import dev.moonseungjun.augmentationsurvival.augment.presentation.WallSlamPresentationService;
import dev.moonseungjun.augmentationsurvival.integration.EntityCompatibilityProfileService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Canonical Wall Slam bridge for actual player melee. A successful melee hit opens a short,
 * forgiving server-authoritative wall-contact window; the derived impact resolves once the target
 * is touching or about to reach a wall in the impact direction.
 */
public final class WallSlamMeleeRuntime {
    private static final String WALL_SLAM_ID = AugmentationSurvival.MOD_ID + ":wall_slam";
    private static final double CONTACT_EPSILON = 0.18D;
    private static final double FORGIVING_SWEEP = 1.10D;
    private static final Map<UUID, Window> WINDOWS = new HashMap<>();

    private WallSlamMeleeRuntime() {
    }

    public static void arm(ServerPlayer attacker, LivingEntity target, float referenceDamage) {
        if (!(referenceDamage > 0.0F) || !Float.isFinite(referenceDamage)
                || attacker.isDeadOrDying()
                || !OffensiveTargetContext.isValid(attacker, target)
                || !EntityCompatibilityProfileService.allowWallImpact(target)) {
            return;
        }
        int ownedLevel = AugmentationPlayerStateService.state(attacker).level(WALL_SLAM_ID);
        if (ownedLevel <= 0) {
            return;
        }
        int level = Math.min(ownedLevel, AugmentCatalog.require(WALL_SLAM_ID).maxLevel());
        Vec3 impactDirection = horizontalDirection(attacker.position(), target.position(), attacker.getLookAngle());
        long now = target.level().getGameTime();
        WINDOWS.put(target.getUUID(), new Window(
                target,
                attacker.getUUID(),
                level,
                referenceDamage,
                impactDirection,
                now + FinalAugmentTuning.current().wallSlamCollisionWindowTicks()
        ));
    }

    public static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Window>> iterator = WINDOWS.entrySet().iterator();
        while (iterator.hasNext()) {
            Window window = iterator.next().getValue();
            LivingEntity target = window.target();
            long now = target.level().getGameTime();
            if (target.isRemoved() || target.isDeadOrDying() || now > window.expiresAtTick()) {
                iterator.remove();
                continue;
            }
            ServerPlayer owner = server.getPlayerList().getPlayer(window.ownerId());
            if (owner == null || owner.isDeadOrDying() || owner.level() != target.level()) {
                iterator.remove();
                continue;
            }
            if (!isAtImpactWall(target, window.impactDirection())) {
                continue;
            }

            iterator.remove();
            float damage = (float) (window.referenceDamage()
                    * FinalAugmentTuning.current().wallSlamDamageFraction(window.level()));
            if (!(damage > 0.0F) || !Float.isFinite(damage)) {
                continue;
            }

            int previousInvulnerableTime = target.invulnerableTime;
            boolean applied;
            target.invulnerableTime = 0;
            try {
                DamageSource wallSource = owner.damageSources().playerAttack(owner);
                applied = CombatProvenanceContext.callAs(
                        CombatProvenance.DERIVED_AOE,
                        () -> target.hurtServer((ServerLevel) target.level(), wallSource, damage)
                );
            } finally {
                target.invulnerableTime = Math.max(target.invulnerableTime, previousInvulnerableTime);
            }
            if (applied && !target.isRemoved()) {
                WallSlamPresentationService.impact(owner, target, window.impactDirection());
            }
        }
    }

    public static void clearPlayer(ServerPlayer player) {
        UUID playerId = player.getUUID();
        WINDOWS.entrySet().removeIf(entry -> entry.getValue().ownerId().equals(playerId)
                || entry.getKey().equals(playerId));
    }

    private static boolean isAtImpactWall(LivingEntity target, Vec3 impactDirection) {
        if (target.horizontalCollision) {
            return true;
        }
        AABB box = target.getBoundingBox();
        if (!target.level().noCollision(target, box.inflate(CONTACT_EPSILON, 0.0D, CONTACT_EPSILON))) {
            return true;
        }
        if (impactDirection.lengthSqr() > 1.0E-7D
                && !target.level().noCollision(
                target,
                box.expandTowards(impactDirection.scale(FORGIVING_SWEEP)))) {
            return true;
        }
        Vec3 motion = target.getDeltaMovement();
        Vec3 horizontalMotion = new Vec3(motion.x, 0.0D, motion.z);
        return horizontalMotion.lengthSqr() > 1.0E-7D
                && !target.level().noCollision(
                target,
                box.expandTowards(horizontalMotion.normalize().scale(FORGIVING_SWEEP)));
    }

    private static Vec3 horizontalDirection(Vec3 from, Vec3 to, Vec3 fallback) {
        Vec3 direction = new Vec3(to.x - from.x, 0.0D, to.z - from.z);
        if (direction.lengthSqr() <= 1.0E-7D) {
            direction = new Vec3(fallback.x, 0.0D, fallback.z);
        }
        return direction.lengthSqr() <= 1.0E-7D ? Vec3.ZERO : direction.normalize();
    }

    private record Window(
            LivingEntity target,
            UUID ownerId,
            int level,
            float referenceDamage,
            Vec3 impactDirection,
            long expiresAtTick
    ) {
    }
}
