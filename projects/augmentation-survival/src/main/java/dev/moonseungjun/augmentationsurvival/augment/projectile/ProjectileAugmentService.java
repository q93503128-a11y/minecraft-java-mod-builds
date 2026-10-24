package dev.moonseungjun.augmentationsurvival.augment.projectile;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenance;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.mixin.AbstractArrowAugmentAccessor;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Server-owned physical projectile graph: segments, homing, ricochet, piercing and chain damage. */
public final class ProjectileAugmentService {
    private static final String PIERCING_ARROW_ID = id("piercing_arrow");
    private static final String CHAIN_ARROW_ID = id("chain_arrow");
    private static final String TRACKING_ARROW_ID = id("tracking_arrow");
    private static final String RICOCHET_ID = id("ricochet");

    private ProjectileAugmentService() {
    }

    public static void initialize() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register(ProjectileAugmentService::afterDamage);
    }

    public static void beforeArrowTick(AbstractArrow arrow) {
        if (!(arrow.level() instanceof ServerLevel)) {
            return;
        }
        ProjectileRuntimeState runtime = ensureRuntime(arrow);
        if (runtime == null) {
            return;
        }
        if (!ownerStillMatches(arrow, runtime)) {
            markReflectedAndRestoreVanillaPierce(arrow, runtime);
            return;
        }
        applyTracking(arrow, runtime);
    }

    public static boolean hurtFromArrow(
            AbstractArrow arrow,
            Entity target,
            DamageSource source,
            float damage
    ) {
        ProjectileRuntimeState runtime = ensureRuntime(arrow);
        if (runtime == null) {
            return target.hurtOrSimulate(source, damage);
        }
        if (!ownerStillMatches(arrow, runtime)) {
            markReflectedAndRestoreVanillaPierce(arrow, runtime);
        }

        ProjectileTuning.Snapshot tuning = ProjectileTuning.current();
        double multiplier = runtime.baseDamageScale();
        if (!runtime.reflected() && runtime.ricochetLevel() > 0 && runtime.bounceCount() > 0) {
            multiplier *= 1.0D
                    + tuning.ricochetDamageBonus(runtime.ricochetLevel()) * runtime.bounceCount();
        }
        if (!runtime.reflected() && runtime.piercingLevel() > 0 && runtime.segmentHitCount() > 0) {
            double reduction = tuning.piercingFollowupReduction(runtime.piercingLevel());
            multiplier *= Math.pow(Math.max(0.0D, 1.0D - reduction), runtime.segmentHitCount());
        }

        float scaled = safeScale(damage, multiplier);
        boolean hurt = CombatProvenanceContext.callAs(
                runtime.provenance(),
                () -> target.hurtOrSimulate(source, scaled)
        );
        if (hurt) {
            runtime.recordSegmentHit(target.getId());
        }
        return hurt;
    }

    public static boolean tryRicochet(AbstractArrow arrow, BlockHitResult hitResult) {
        if (!(arrow.level() instanceof ServerLevel serverLevel) || hitResult.isWorldBorderHit()) {
            return false;
        }
        ProjectileRuntimeState runtime = ensureRuntime(arrow);
        if (runtime == null
                || runtime.reflected()
                || !ownerStillMatches(arrow, runtime)
                || runtime.ricochetLevel() <= 0
                || runtime.bounceCount() >= runtime.ricochetLevel()) {
            return false;
        }

        Vec3 movement = arrow.getDeltaMovement();
        if (movement.lengthSqr() <= 1.0E-7D) {
            return false;
        }

        BlockState blockState = serverLevel.getBlockState(hitResult.getBlockPos());
        blockState.onProjectileHit(serverLevel, blockState, hitResult, arrow);
        ItemStack weapon = arrow.getWeaponItem();
        AbstractArrowAugmentAccessor accessor = (AbstractArrowAugmentAccessor) arrow;
        if (weapon != null && !weapon.isEmpty()) {
            accessor.augmentationSurvival$invokeHitBlockEnchantmentEffects(
                    serverLevel,
                    hitResult,
                    weapon
            );
        }

        Vec3 normal = hitResult.getDirection().getUnitVec3();
        double dot = movement.dot(normal);
        Vec3 reflected = movement.subtract(normal.scale(2.0D * dot));
        if (reflected.lengthSqr() <= 1.0E-7D) {
            return false;
        }

        runtime.recordBounce();
        accessor.augmentationSurvival$invokeResetPiercedEntities();
        accessor.augmentationSurvival$invokeSetPierceLevel(combinedPierce(runtime));
        accessor.augmentationSurvival$invokeSetInGround(false);
        double epsilon = ProjectileTuning.current().ricochetSurfaceEpsilon();
        arrow.setPos(hitResult.getLocation().add(normal.scale(epsilon)));
        arrow.setDeltaMovement(reflected);
        return true;
    }

    public static CombatProvenance provenance(DamageSource source) {
        if (!(source.getDirectEntity() instanceof AbstractArrow arrow)) {
            return null;
        }
        ProjectileRuntimeState runtime = runtime(arrow);
        if (runtime == null || !runtime.initialized()) {
            return null;
        }
        if (!ownerStillMatches(arrow, runtime)) {
            return CombatProvenance.REFLECTED;
        }
        return runtime.provenance();
    }

    public static void markDerivedProjectile(AbstractArrow arrow, ServerPlayer owner) {
        if (arrow instanceof ThrownTrident) {
            return;
        }
        ProjectileRuntimeState runtime = runtime(arrow);
        if (runtime == null) {
            runtime = new ProjectileRuntimeState();
            carrier(arrow).augmentationSurvival$setProjectileRuntime(runtime);
        }
        runtime.setProvenance(CombatProvenance.DERIVED_PROJECTILE);
        runtime.setBaseDamageScale(ProjectileTuning.current().scatterDamageMultiplier());
        ensureRuntime(arrow);
    }

    private static ProjectileRuntimeState ensureRuntime(AbstractArrow arrow) {
        if (arrow instanceof ThrownTrident) {
            return null;
        }
        ProjectileRuntimeCarrier carrier = carrier(arrow);
        ProjectileRuntimeState runtime = carrier.augmentationSurvival$getProjectileRuntime();
        if (runtime == null) {
            runtime = new ProjectileRuntimeState();
            carrier.augmentationSurvival$setProjectileRuntime(runtime);
        }
        if (runtime.initialized()) {
            return runtime;
        }
        if (!(arrow.getOwner() instanceof ServerPlayer owner)) {
            return null;
        }

        AugmentationPlayerState state = AugmentationPlayerStateService.state(owner);
        runtime.initialize(
                owner.getUUID(),
                arrow.getPierceLevel(),
                boundedLevel(state, PIERCING_ARROW_ID),
                boundedLevel(state, TRACKING_ARROW_ID),
                boundedLevel(state, RICOCHET_ID),
                boundedLevel(state, CHAIN_ARROW_ID)
        );
        ((AbstractArrowAugmentAccessor) arrow)
                .augmentationSurvival$invokeSetPierceLevel(combinedPierce(runtime));
        return runtime;
    }

    private static void applyTracking(AbstractArrow arrow, ProjectileRuntimeState runtime) {
        int level = runtime.trackingLevel();
        if (level <= 0 || arrow.isNoPhysics()) {
            return;
        }
        Vec3 movement = arrow.getDeltaMovement();
        double speed = movement.length();
        if (speed <= 1.0E-5D) {
            return;
        }

        ProjectileTuning.Snapshot tuning = ProjectileTuning.current();
        double range = tuning.trackingRange();
        double minDot = Math.cos(Math.toRadians(tuning.trackingConeDegrees()));
        Vec3 currentDirection = movement.scale(1.0D / speed);
        AABB search = arrow.getBoundingBox().inflate(range);
        List<LivingEntity> candidates = arrow.level().getEntitiesOfClass(
                LivingEntity.class,
                search,
                candidate -> candidate instanceof Enemy
                        && candidate.isAlive()
                        && candidate != arrow.getOwner()
                        && !runtime.hasHitInCurrentSegment(candidate.getId())
        );

        LivingEntity target = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates) {
            Vec3 toTarget = candidate.getBoundingBox().getCenter().subtract(arrow.position());
            double distanceSqr = toTarget.lengthSqr();
            if (distanceSqr <= 1.0E-7D || distanceSqr > range * range) {
                continue;
            }
            Vec3 desired = toTarget.normalize();
            if (currentDirection.dot(desired) < minDot) {
                continue;
            }
            if (candidate.getId() == runtime.trackingTargetId()) {
                target = candidate;
                break;
            }
            if (distanceSqr < bestDistance) {
                bestDistance = distanceSqr;
                target = candidate;
            }
        }
        if (target == null) {
            runtime.setTrackingTargetId(-1);
            return;
        }

        Vec3 desired = target.getBoundingBox().getCenter().subtract(arrow.position()).normalize();
        Vec3 turned = turnTowards(
                currentDirection,
                desired,
                Math.toRadians(tuning.trackingTurnDegrees(level))
        );
        arrow.setDeltaMovement(turned.scale(speed));
        runtime.setTrackingTargetId(target.getId());
    }

    private static Vec3 turnTowards(Vec3 current, Vec3 desired, double maxAngle) {
        double dot = Math.max(-1.0D, Math.min(1.0D, current.dot(desired)));
        double angle = Math.acos(dot);
        if (angle <= maxAngle || angle <= 1.0E-7D) {
            return desired;
        }
        Vec3 axis = current.cross(desired);
        if (axis.lengthSqr() <= 1.0E-12D) {
            return desired;
        }
        axis = axis.normalize();
        double cos = Math.cos(maxAngle);
        double sin = Math.sin(maxAngle);
        return current.scale(cos)
                .add(axis.cross(current).scale(sin))
                .add(axis.scale(axis.dot(current) * (1.0D - cos)))
                .normalize();
    }

    private static void afterDamage(
            LivingEntity entity,
            DamageSource source,
            float baseDamageTaken,
            float damageTaken,
            boolean blocked
    ) {
        if (blocked
                || !(damageTaken > 0.0F)
                || !(entity instanceof Enemy)
                || !(source.getDirectEntity() instanceof AbstractArrow arrow)) {
            return;
        }
        ProjectileRuntimeState runtime = ensureRuntime(arrow);
        if (runtime == null
                || runtime.reflected()
                || runtime.chainLevel() <= 0
                || runtime.chainUsed()
                || runtime.segmentHitCount() != 0
                || !(arrow.getOwner() instanceof ServerPlayer owner)
                || !ownerStillMatches(arrow, runtime)
                || !(entity.level() instanceof ServerLevel level)) {
            return;
        }

        runtime.markChainUsed();
        Set<Integer> visited = new HashSet<>();
        visited.add(entity.getId());
        visited.add(owner.getId());
        LivingEntity anchor = entity;
        float chainDamage = damageTaken;
        double range = ProjectileTuning.current().chainRange();

        for (int hop = 0; hop < runtime.chainLevel(); hop++) {
            LivingEntity next = nearestChainTarget(level, anchor, visited, range);
            if (next == null) {
                break;
            }
            visited.add(next.getId());
            final float amount = chainDamage;
            boolean hurt = CombatProvenanceContext.callAs(
                    CombatProvenance.DERIVED_CHAIN,
                    () -> next.hurtOrSimulate(source, amount)
            );
            if (!hurt) {
                break;
            }
            anchor = next;
            chainDamage = safeScale(
                    chainDamage,
                    ProjectileTuning.current().chainNextMultiplier()
            );
        }
    }

    private static LivingEntity nearestChainTarget(
            ServerLevel level,
            LivingEntity anchor,
            Set<Integer> visited,
            double range
    ) {
        LivingEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity candidate : level.getEntitiesOfClass(
                LivingEntity.class,
                anchor.getBoundingBox().inflate(range),
                entity -> entity instanceof Enemy && entity.isAlive() && !visited.contains(entity.getId())
        )) {
            double distance = anchor.distanceToSqr(candidate);
            if (distance <= range * range && distance < best) {
                best = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    private static void markReflectedAndRestoreVanillaPierce(
            AbstractArrow arrow,
            ProjectileRuntimeState runtime
    ) {
        if (runtime.reflected()) {
            return;
        }
        runtime.markReflected();
        AbstractArrowAugmentAccessor accessor = (AbstractArrowAugmentAccessor) arrow;
        accessor.augmentationSurvival$invokeResetPiercedEntities();
        accessor.augmentationSurvival$invokeSetPierceLevel(runtime.vanillaPierceLevel());
    }

    private static boolean ownerStillMatches(AbstractArrow arrow, ProjectileRuntimeState runtime) {
        return runtime.originalOwnerId() != null
                && arrow.getOwner() instanceof ServerPlayer player
                && runtime.originalOwnerId().equals(player.getUUID());
    }

    private static byte combinedPierce(ProjectileRuntimeState runtime) {
        int combined = Byte.toUnsignedInt(runtime.vanillaPierceLevel()) + runtime.piercingLevel();
        return (byte) Math.min(127, combined);
    }

    private static ProjectileRuntimeCarrier carrier(AbstractArrow arrow) {
        return (ProjectileRuntimeCarrier) arrow;
    }

    private static ProjectileRuntimeState runtime(AbstractArrow arrow) {
        return carrier(arrow).augmentationSurvival$getProjectileRuntime();
    }

    private static int boundedLevel(AugmentationPlayerState state, String augmentId) {
        int level = state.level(augmentId);
        if (level <= 0) {
            return 0;
        }
        return Math.min(level, AugmentCatalog.require(augmentId).maxLevel());
    }

    private static float safeScale(float amount, double multiplier) {
        double scaled = amount * multiplier;
        if (!Double.isFinite(scaled) || scaled <= 0.0D) {
            return 0.0F;
        }
        return (float) Math.min(Float.MAX_VALUE, scaled);
    }

    private static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }
}
