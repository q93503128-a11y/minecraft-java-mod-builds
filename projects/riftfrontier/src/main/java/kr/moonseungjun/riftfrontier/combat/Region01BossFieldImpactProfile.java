package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.content.ContentId;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Provisional field-play geometry for the three authored Region 01 boss attack roles.
 *
 * <p>These values are calibration scaffolding, not final boss balance. The semantic content already locks the
 * relative jobs: committed strike is a broad close commitment, line displacement owns a longer narrow lane, and
 * arena pressure threatens a local area. Human Minecraft field play must approve or revise the concrete dimensions
 * before production encounter attachment.</p>
 */
public final class Region01BossFieldImpactProfile {
    public static final ContentId COMMITTED_STRIKE =
        ContentId.parse("riftfrontier:attack/boss/region_01_committed_strike");
    public static final ContentId LINE_DISPLACEMENT =
        ContentId.parse("riftfrontier:attack/boss/region_01_line_displacement");
    public static final ContentId ARENA_PRESSURE =
        ContentId.parse("riftfrontier:attack/boss/region_01_arena_pressure");

    private static final Map<ContentId, Profile> PROFILES = Map.of(
        COMMITTED_STRIKE, new Profile(Shape.FORWARD_ARC, 3.4D, 2.2D, 1.7D),
        LINE_DISPLACEMENT, new Profile(Shape.FORWARD_LANE, 6.0D, 1.15D, 1.7D),
        ARENA_PRESSURE, new Profile(Shape.LOCAL_AREA, 4.5D, 4.5D, 1.7D)
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

    public record Profile(Shape shape, double reach, double halfWidth, double verticalRadius) {
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
        }
    }
}
