package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.content.ContentId;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
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
    private static final int LOCAL_RING_SAMPLES = 24;
    private static final int DIRECTIONAL_SIDE_SAMPLES = 8;
    private static final int ARC_FRONT_SAMPLES = 10;

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

        List<LocalPoint> boundary = sampleBoundary(profile);
        for (LocalPoint point : boundary) {
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

    /** Returns local-space outline samples that remain inside the exact provisional hit-profile boundary. */
    static List<LocalPoint> sampleBoundary(Region01BossFieldImpactProfile.Profile profile) {
        Objects.requireNonNull(profile, "profile");
        return switch (profile.shape()) {
            case LOCAL_AREA -> sampleLocalArea(profile);
            case FORWARD_LANE -> sampleForwardLane(profile);
            case FORWARD_ARC -> sampleForwardArc(profile);
        };
    }

    private static List<LocalPoint> sampleLocalArea(Region01BossFieldImpactProfile.Profile profile) {
        List<LocalPoint> points = new ArrayList<>(LOCAL_RING_SAMPLES);
        for (int i = 0; i < LOCAL_RING_SAMPLES; i++) {
            double angle = Math.PI * 2.0D * i / LOCAL_RING_SAMPLES;
            points.add(new LocalPoint(
                Math.cos(angle) * profile.reach(),
                Math.sin(angle) * profile.reach()
            ));
        }
        return List.copyOf(points);
    }

    private static List<LocalPoint> sampleForwardLane(Region01BossFieldImpactProfile.Profile profile) {
        List<LocalPoint> points = new ArrayList<>((DIRECTIONAL_SIDE_SAMPLES + 1) * 2 + 5);
        for (int i = 0; i <= DIRECTIONAL_SIDE_SAMPLES; i++) {
            double forward = profile.reach() * i / DIRECTIONAL_SIDE_SAMPLES;
            points.add(new LocalPoint(forward, -profile.halfWidth()));
            points.add(new LocalPoint(forward, profile.halfWidth()));
        }
        for (int i = -2; i <= 2; i++) {
            points.add(new LocalPoint(profile.reach(), profile.halfWidth() * i / 2.0D));
        }
        return List.copyOf(points);
    }

    private static List<LocalPoint> sampleForwardArc(Region01BossFieldImpactProfile.Profile profile) {
        double sideLateral = Math.min(profile.halfWidth(), profile.reach());
        double sideForward = Math.sqrt(Math.max(0.0D, profile.reach() * profile.reach() - sideLateral * sideLateral));
        List<LocalPoint> points = new ArrayList<>((DIRECTIONAL_SIDE_SAMPLES + 1) * 2 + ARC_FRONT_SAMPLES + 1);

        for (int i = 0; i <= DIRECTIONAL_SIDE_SAMPLES; i++) {
            double forward = sideForward * i / DIRECTIONAL_SIDE_SAMPLES;
            points.add(new LocalPoint(forward, -sideLateral));
            points.add(new LocalPoint(forward, sideLateral));
        }
        for (int i = 0; i <= ARC_FRONT_SAMPLES; i++) {
            double lateral = -sideLateral + (sideLateral * 2.0D * i / ARC_FRONT_SAMPLES);
            double forward = Math.sqrt(Math.max(0.0D, profile.reach() * profile.reach() - lateral * lateral));
            points.add(new LocalPoint(forward, lateral));
        }
        return List.copyOf(points);
    }

    record LocalPoint(double forward, double lateral) {
        LocalPoint {
            if (!Double.isFinite(forward) || !Double.isFinite(lateral)) {
                throw new IllegalArgumentException("telegraph boundary point must be finite");
            }
        }
    }
}
