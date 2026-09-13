package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.content.ContentId;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Provisional field-play geometry and movement calibration for the three authored Region 01 boss attack roles.
 *
 * <p>These values are calibration scaffolding, not final boss balance. The semantic content already locks the
 * relative jobs: committed strike is a broad close commitment, line displacement owns a longer narrow lane and is
 * authored as a {@code line_charge}, and arena pressure threatens a local area. Human Minecraft field play must
 * approve or revise the concrete dimensions, charge travel and radial displacement before production encounter
 * attachment.</p>
 */
public final class Region01BossFieldImpactProfile {
    public static final ContentId COMMITTED_STRIKE =
        ContentId.parse("riftfrontier:attack/boss/region_01_committed_strike");
    public static final ContentId LINE_DISPLACEMENT =
        ContentId.parse("riftfrontier:attack/boss/region_01_line_displacement");
    public static final ContentId ARENA_PRESSURE =
        ContentId.parse("riftfrontier:attack/boss/region_01_arena_pressure");

    private static final Map<ContentId, Profile> PROFILES = Map.of(
        COMMITTED_STRIKE, new Profile(Shape.FORWARD_ARC, 3.4D, 2.2D, 1.7D, 0.0D, 0.0D),
        // 0.5 blocks per ACTIVE tick yields a visible provisional charge while staying well inside the authored
        // six-block threat lane. This is a field-play calibration value, not final boss movement balance.
        LINE_DISPLACEMENT, new Profile(Shape.FORWARD_LANE, 6.0D, 1.15D, 1.7D, 0.5D, 0.0D),
        // The local-pressure role receives one horizontal radial push when ACTIVE begins. The 0.85 strength is
        // deliberately provisional: it exists to make displacement readable in human field play, not to lock final
        // knockback, arena size or encounter difficulty.
        ARENA_PRESSURE, new Profile(Shape.LOCAL_AREA, 4.5D, 4.5D, 1.7D, 0.0D, 0.85D)
    );

    private Region01BossFieldImpactProfile() {}

    public static Optional<Profile> find(ContentId patternId) {
        return Optional.ofNullable(PROFILES.get(Objects.requireNonNull(patternId, "patternId")));
    }

    public enum Shape {
        FORWARD_ARC,
        FORWARD_LANE,
        LOCAL_AREA
    }

    public record Profile(
        Shape shape,
        double reach,
        double halfWidth,
        double verticalRadius,
        double activeForwardStep,
        double activeEntryRadialImpulse
    ) {
        /** Geometry-only callers intentionally author neither charge travel nor radial displacement. */
        public Profile(Shape shape, double reach, double halfWidth, double verticalRadius) {
            this(shape, reach, halfWidth, verticalRadius, 0.0D, 0.0D);
        }

        /** Existing movement-only callers keep their exact meaning and opt out of radial displacement. */
        public Profile(
            Shape shape,
            double reach,
            double halfWidth,
            double verticalRadius,
            double activeForwardStep
        ) {
            this(shape, reach, halfWidth, verticalRadius, activeForwardStep, 0.0D);
        }

        public Profile {
            Objects.requireNonNull(shape, "shape");
            if (!Double.isFinite(reach) || reach <= 0.0D) {
                throw new IllegalArgumentException("reach must be finite and > 0");
            }
            if (!Double.isFinite(halfWidth) || halfWidth <= 0.0D) {
                throw new IllegalArgumentException("halfWidth must be finite and > 0");
            }
            if (!Double.isFinite(verticalRadius) || verticalRadius <= 0.0D) {
                throw new IllegalArgumentException("verticalRadius must be finite and > 0");
            }
            if (!Double.isFinite(activeForwardStep) || activeForwardStep < 0.0D) {
                throw new IllegalArgumentException("activeForwardStep must be finite and >= 0");
            }
            if (!Double.isFinite(activeEntryRadialImpulse) || activeEntryRadialImpulse < 0.0D) {
                throw new IllegalArgumentException("activeEntryRadialImpulse must be finite and >= 0");
            }
            if (activeForwardStep > 0.0D && shape != Shape.FORWARD_LANE) {
                throw new IllegalArgumentException("provisional forward travel is only authored for the line lane role");
            }
            if (activeForwardStep > reach) {
                throw new IllegalArgumentException("activeForwardStep cannot exceed the field threat reach");
            }
            if (activeEntryRadialImpulse > 0.0D && shape != Shape.LOCAL_AREA) {
                throw new IllegalArgumentException("provisional radial impulse is only authored for the local area role");
            }
        }
    }
}
