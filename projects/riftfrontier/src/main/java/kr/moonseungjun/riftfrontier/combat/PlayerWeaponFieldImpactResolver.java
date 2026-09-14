package kr.moonseungjun.riftfrontier.combat;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

        boolean damaged = target.hurtServer(level, level.damageSources().playerAttack(player), profile.diagnosticDamage());
        if (!damaged) return;

        // Minecraft-native impact readability only. These cues are emitted strictly after the
        // authoritative ACTIVE hit succeeds, so presentation can never create a second hit clock.
        // Final Riftfrontier weapon VFX/audio still require reference review and human field play.
        level.sendParticles(
            ParticleTypes.DAMAGE_INDICATOR,
            target.getX(),
            target.getY(0.5D),
            target.getZ(),
            2,
            0.12D,
            0.08D,
            0.12D,
            0.08D
        );
        level.playSound(
            null,
            target.getX(),
            target.getY(),
            target.getZ(),
            SoundEvents.PLAYER_ATTACK_STRONG,
            SoundSource.PLAYERS,
            0.7F,
            1.0F
        );
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
