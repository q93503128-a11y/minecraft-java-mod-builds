package kr.moonseungjun.riftfrontier.combat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Minecraft-API-free local-space boundary sampler for Region 01 field-review threat telegraphs.
 *
 * <p>The source of truth remains {@link Region01BossFieldImpactProfile}; this helper only converts an existing
 * provisional gameplay profile into sparse outline samples for presentation. It intentionally owns no timing,
 * target admission, damage, movement, knockback or final VFX language.</p>
 */
public final class Region01BossFieldTelegraphGeometry {
    private static final int LOCAL_RING_SAMPLES = 24;
    private static final int DIRECTIONAL_SIDE_SAMPLES = 8;
    private static final int ARC_FRONT_SAMPLES = 10;

    private Region01BossFieldTelegraphGeometry() {}

    public static List<LocalPoint> sampleBoundary(Region01BossFieldImpactProfile.Profile profile) {
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

    public record LocalPoint(double forward, double lateral) {
        public LocalPoint {
            if (!Double.isFinite(forward) || !Double.isFinite(lateral)) {
                throw new IllegalArgumentException("telegraph boundary point must be finite");
            }
        }
    }
}
