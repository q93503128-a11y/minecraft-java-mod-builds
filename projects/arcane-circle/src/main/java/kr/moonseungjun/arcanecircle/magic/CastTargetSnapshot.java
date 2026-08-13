package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * Immutable authoritative targeting state captured exactly once when a spell leaves ready-hold.
 * Delayed gameplay and client presentation consume the same absolute target instead of sampling
 * the caster's later look direction. Homing is deliberately opt-in.
 */
public record CastTargetSnapshot(
        String spellId,
        UUID casterId,
        ResourceKey<Level> dimension,
        Vec3 launchOrigin,
        Vec3 target,
        Vec3 launchDirection,
        UUID targetEntityId,
        BlockPos impactSurface,
        boolean homing,
        long barrageSeed
) {
    private static final ThreadLocal<CastTargetSnapshot> ACTIVE = new ThreadLocal<>();

    public CastTargetSnapshot {
        launchOrigin = launchOrigin == null ? Vec3.ZERO : launchOrigin;
        target = target == null ? launchOrigin : target;
        launchDirection = safeDirection(launchDirection);
    }

    public boolean validFor(LivingEntity caster) {
        return caster != null
                && caster.isAlive()
                && caster.getUUID().equals(casterId)
                && caster.level().dimension().equals(dimension);
    }

    public Optional<LivingEntity> targetEntity(ServerPlayer player) {
        if (targetEntityId == null || !validFor(player)) return Optional.empty();
        Entity entity = ((ServerLevel) player.level()).getEntity(targetEntityId);
        return entity instanceof LivingEntity living && living.isAlive() && !living.isRemoved()
                ? Optional.of(living) : Optional.empty();
    }

    public Vec3 resolvedTarget(ServerPlayer player) {
        if (!homing) return target;
        return targetEntity(player).map(LivingEntity::getEyePosition).orElse(target);
    }

    public boolean executeLocked(ServerPlayer player, BooleanSupplier action) {
        if (!validFor(player) || player.isSpectator()) return false;
        CastTargetSnapshot previous = ACTIVE.get();
        float oldYaw = player.getYRot();
        float oldPitch = player.getXRot();
        ACTIVE.set(this);
        try {
            Vec3 aim = resolvedTarget(player);
            Vec3 delta = aim.subtract(player.getEyePosition());
            if (target.distanceToSqr(launchOrigin) < 2.25 || delta.lengthSqr() < 1.0E-8) {
                delta = launchDirection;
            }
            double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
            player.setYRot((float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0));
            player.setXRot((float) (-Math.toDegrees(Math.atan2(delta.y, Math.max(1.0E-8, horizontal)))));
            return action.getAsBoolean();
        } finally {
            player.setYRot(oldYaw);
            player.setXRot(oldPitch);
            if (previous == null) ACTIVE.remove();
            else ACTIVE.set(previous);
        }
    }

    public static Optional<CastTargetSnapshot> active(ServerPlayer player) {
        CastTargetSnapshot snapshot = ACTIVE.get();
        return snapshot != null && snapshot.validFor(player) ? Optional.of(snapshot) : Optional.empty();
    }

    public static Vec3 targetOr(ServerPlayer player, Vec3 fallback) {
        return active(player).map(value -> value.resolvedTarget(player)).orElse(fallback);
    }

    public static Vec3 launchOriginOr(ServerPlayer player, Vec3 fallback) {
        return active(player).map(CastTargetSnapshot::launchOrigin).orElse(fallback);
    }

    public static Vec3 launchDirectionOr(ServerPlayer player, Vec3 fallback) {
        return active(player).map(CastTargetSnapshot::launchDirection).orElseGet(() -> safeDirection(fallback));
    }

    private static Vec3 safeDirection(Vec3 value) {
        return value == null || value.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : value.normalize();
    }
}
