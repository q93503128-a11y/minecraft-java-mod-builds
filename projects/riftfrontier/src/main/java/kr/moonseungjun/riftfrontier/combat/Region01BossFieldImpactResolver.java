package kr.moonseungjun.riftfrontier.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Server-authoritative field-play hit resolver for the three authored Region 01 boss attack roles. */
public final class Region01BossFieldImpactResolver implements MinecraftAttackAdapter.HitVolume {
    @Override
    public Iterable<? extends LivingEntity> resolve(
        ServerLevel level,
        LivingEntity attacker,
        AttackExecution.Snapshot snapshot
    ) {
        Region01BossFieldImpactProfile.Profile profile = Region01BossFieldImpactProfile.find(snapshot.patternId())
            .orElse(null);
        if (profile == null || !snapshot.mayApplyHit()) return List.of();

        Vec3 look = attacker.getLookAngle();
        double horizontalLength = Math.hypot(look.x, look.z);
        if (horizontalLength < 1.0E-6D && profile.shape() != Region01BossFieldImpactProfile.Shape.LOCAL_AREA) {
            return List.of();
        }
        double forwardX = horizontalLength < 1.0E-6D ? 0.0D : look.x / horizontalLength;
        double forwardZ = horizontalLength < 1.0E-6D ? 0.0D : look.z / horizontalLength;

        AABB search = attacker.getBoundingBox().inflate(profile.reach(), profile.verticalRadius(), profile.reach());
        return level.getEntitiesOfClass(
            LivingEntity.class,
            search,
            target -> MinecraftCombatAuthority.isEligibleTarget(level, attacker, target)
                && insideProfile(attacker, target, profile, forwardX, forwardZ)
        );
    }

    static boolean insideProfile(
        LivingEntity attacker,
        LivingEntity target,
        Region01BossFieldImpactProfile.Profile profile,
        double forwardX,
        double forwardZ
    ) {
        Vec3 origin = attacker.getBoundingBox().getCenter();
        Vec3 point = target.getBoundingBox().getCenter();
        double dx = point.x - origin.x;
        double dy = point.y - origin.y;
        double dz = point.z - origin.z;
        if (Math.abs(dy) > profile.verticalRadius() + target.getBbHeight() * 0.5D) return false;

        double horizontalDistance = Math.hypot(dx, dz);
        if (profile.shape() == Region01BossFieldImpactProfile.Shape.LOCAL_AREA) {
            return horizontalDistance <= profile.reach();
        }

        double forward = dx * forwardX + dz * forwardZ;
        if (forward < 0.0D || forward > profile.reach()) return false;
        double lateral = Math.abs(dx * -forwardZ + dz * forwardX);

        return switch (profile.shape()) {
            case FORWARD_ARC -> horizontalDistance <= profile.reach() && lateral <= profile.halfWidth();
            case FORWARD_LANE -> lateral <= profile.halfWidth();
            case LOCAL_AREA -> false; // handled above
        };
    }
}
