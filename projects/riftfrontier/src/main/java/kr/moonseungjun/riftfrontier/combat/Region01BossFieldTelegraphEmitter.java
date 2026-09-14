package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.content.ContentId;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Objects;

/**
 * Field-review-only threat-shape readability for the Region 01 boss.
 *
 * <p>This class does not own an attack clock, target admission, damage, movement or hit geometry. It reads the exact
 * server-authored {@link Region01BossFieldImpactProfile} already used by the authoritative field hit resolver and
 * emits a sparse vanilla-particle outline during TELEGRAPH. The particles are deliberately a temporary Minecraft-
 * native readability baseline, not Riftfrontier's final VFX language.</p>
 */
public final class Region01BossFieldTelegraphEmitter {
    /**
     * Emits at half tick-rate so the outline remains readable without turning the field harness into a particle wall.
     * The cadence is presentation-only and never feeds back into gameplay timing.
     *
     * @return number of boundary particles emitted this call
     */
    public int emit(ServerLevel level, LivingEntity boss, BossPresentationSemanticState state) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(boss, "boss");
        Objects.requireNonNull(state, "state");

        if (!state.active()
            || !AttackTimeline.Phase.TELEGRAPH.name().equals(state.attackPhase())
            || (state.serverGameTick() & 1L) != 0L) {
            return 0;
        }

        Region01BossFieldImpactProfile.Profile profile = Region01BossFieldImpactProfile.find(
            ContentId.parse(state.patternId())
        ).orElse(null);
        if (profile == null) {
            return 0;
        }

        Vec3 look = boss.getLookAngle();
        double horizontalLength = Math.hypot(look.x, look.z);
        if (profile.shape() != Region01BossFieldImpactProfile.Shape.LOCAL_AREA && horizontalLength < 1.0E-6D) {
            return 0;
        }

        double forwardX = horizontalLength < 1.0E-6D ? 0.0D : look.x / horizontalLength;
        double forwardZ = horizontalLength < 1.0E-6D ? 1.0D : look.z / horizontalLength;
        double rightX = -forwardZ;
        double rightZ = forwardX;
        Vec3 origin = boss.getBoundingBox().getCenter();
        double y = boss.getBoundingBox().minY + 0.08D;

        List<Region01BossFieldTelegraphGeometry.LocalPoint> boundary =
            Region01BossFieldTelegraphGeometry.sampleBoundary(profile);
        for (Region01BossFieldTelegraphGeometry.LocalPoint point : boundary) {
            double x = origin.x + forwardX * point.forward() + rightX * point.lateral();
            double z = origin.z + forwardZ * point.forward() + rightZ * point.lateral();
            level.sendParticles(
                ParticleTypes.CRIT,
                x,
                y,
                z,
                1,
                0.0D,
                0.0D,
                0.0D,
                0.0D
            );
        }
        return boundary.size();
    }
}
