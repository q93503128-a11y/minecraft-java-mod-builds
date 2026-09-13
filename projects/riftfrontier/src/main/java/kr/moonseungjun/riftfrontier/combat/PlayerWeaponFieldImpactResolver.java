package kr.moonseungjun.riftfrontier.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Server-authoritative field-play resolver for the first two production weapon roles.
 *
 * <p>The geometry is intentionally simple and visible enough to calibrate in Minecraft. It is not
 * final weapon art or a final hitbox contract. The resolver consumes the same ACTIVE-only snapshot
 * used by the attack clock, then applies impact only after the adapter's once-per-execution target
 * deduplication has accepted the candidate.</p>
 */
public final class PlayerWeaponFieldImpactResolver
    implements MinecraftPlayerWeaponCombatAdapter.HitVolume, MinecraftPlayerWeaponCombatAdapter.ImpactPolicy {

    @Override
    public Iterable<? extends LivingEntity> resolve(
        ServerLevel level,
        LivingEntity actor,
        AttackExecution.Snapshot snapshot
    ) {
        PlayerWeaponFieldImpactProfile.Profile profile = PlayerWeaponFieldImpactProfile.find(snapshot.patternId())
            .orElse(null);
        if (profile == null || !snapshot.mayApplyHit()) return List.of();

        Vec3 look = actor.getLookAngle();
        double horizontalLength = Math.hypot(look.x, look.z);
        if (horizontalLength < 1.0E-6D) return List.of();
        double forwardX = look.x / horizontalLength;
        double forwardZ = look.z / horizontalLength;

        AABB search = actor.getBoundingBox().inflate(profile.reach(), profile.verticalRadius(), profile.reach());
        return level.getEntitiesOfClass(
            LivingEntity.class,
            search,
            target -> MinecraftCombatAuthority.isEligibleTarget(level, actor, target)
                && insideProfile(actor, target, profile, forwardX, forwardZ)
        );
    }

    @Override
    public void apply(
        ServerLevel level,
        LivingEntity actor,
        LivingEntity target,
        AttackExecution.Snapshot snapshot
    ) {
        PlayerWeaponFieldImpactProfile.Profile profile = PlayerWeaponFieldImpactProfile.find(snapshot.patternId())
            .orElse(null);
        if (profile == null || !snapshot.mayApplyHit() || !(actor instanceof ServerPlayer player)) return;
        target.hurtServer(level, level.damageSources().playerAttack(player), profile.diagnosticDamage());
    }

    static boolean insideProfile(
        LivingEntity actor,
        LivingEntity target,
        PlayerWeaponFieldImpactProfile.Profile profile,
        double forwardX,
        double forwardZ
    ) {
        Vec3 origin = actor.getBoundingBox().getCenter();
        Vec3 point = target.getBoundingBox().getCenter();
        double dx = point.x - origin.x;
        double dy = point.y - origin.y;
        double dz = point.z - origin.z;
        if (Math.abs(dy) > profile.verticalRadius() + target.getBbHeight() * 0.5D) return false;

        double forward = dx * forwardX + dz * forwardZ;
        if (forward < 0.0D || forward > profile.reach()) return false;
        double lateral = Math.abs(dx * -forwardZ + dz * forwardX);

        return switch (profile.shape()) {
            case FORWARD_LANE -> lateral <= profile.halfWidth();
            case FORWARD_ARC -> Math.hypot(dx, dz) <= profile.reach() && lateral <= profile.halfWidth();
        };
    }
}
