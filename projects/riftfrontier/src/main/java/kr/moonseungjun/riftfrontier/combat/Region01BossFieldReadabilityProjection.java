package kr.moonseungjun.riftfrontier.combat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Minecraft-client-free projection of the provisional Region 01 boss field geometry for diagnostics.
 *
 * <p>The projection deliberately derives every sample from {@link Region01BossFieldImpactProfile.Profile}; it owns no
 * independent reach, width, damage, timing or art constants. Client presentation may draw these samples, but the
 * server resolver remains authoritative for actual hit admission.</p>
 */
public final class Region01BossFieldReadabilityProjection {
    private Region01BossFieldReadabilityProjection() {}

    public static List<LocalSample> samples(Region01BossFieldImpactProfile.Profile profile) {
        Objects.requireNonNull(profile, "profile");
        return switch (profile.shape()) {
            case LOCAL_AREA -> localAreaSamples(profile);
            case FORWARD_LANE -> forwardLaneSamples(profile);
            case FORWARD_ARC -> forwardArcSamples(profile);
        };
    }

    private static List<LocalSample> localAreaSamples(Region01BossFieldImpactProfile.Profile profile) {
        List<LocalSample> samples = new ArrayList<>();
        int points = 20;
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0D * i / points;
            samples.add(new LocalSample(Math.cos(angle) * profile.reach(), Math.sin(angle) * profile.reach()));
        }
        return List.copyOf(samples);
    }

    private static List<LocalSample> forwardLaneSamples(Region01BossFieldImpactProfile.Profile profile) {
        List<LocalSample> samples = new ArrayList<>();
        int longitudinalSteps = 8;
        for (int i = 0; i <= longitudinalSteps; i++) {
            double forward = profile.reach() * i / longitudinalSteps;
            samples.add(new LocalSample(forward, -profile.halfWidth()));
            samples.add(new LocalSample(forward, profile.halfWidth()));
        }
        samples.add(new LocalSample(profile.reach(), 0.0D));
        return List.copyOf(samples);
    }

    private static List<LocalSample> forwardArcSamples(Region01BossFieldImpactProfile.Profile profile) {
        List<LocalSample> samples = new ArrayList<>();
        int forwardSteps = 8;
        for (int i = 1; i <= forwardSteps; i++) {
            double forward = profile.reach() * i / forwardSteps;
            double circleHalfWidth = Math.sqrt(Math.max(0.0D,
                profile.reach() * profile.reach() - forward * forward));
            double lateral = Math.min(profile.halfWidth(), circleHalfWidth);
            samples.add(new LocalSample(forward, -lateral));
            samples.add(new LocalSample(forward, lateral));
        }
        samples.add(new LocalSample(profile.reach(), 0.0D));
        return List.copyOf(samples);
    }

    public record LocalSample(double forward, double lateral) {
        public LocalSample {
            if (!Double.isFinite(forward) || !Double.isFinite(lateral)) {
                throw new IllegalArgumentException("readability sample coordinates must be finite");
            }
        }
    }
}
