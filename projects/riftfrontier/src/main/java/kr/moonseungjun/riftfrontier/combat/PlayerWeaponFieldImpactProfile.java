package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.content.ContentId;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Provisional, field-play-only player weapon impact calibration.
 *
 * <p>This is deliberately not final balance. The locked weapon dossier approves only the relative
 * role contrast: mobile pressure must stay shorter-ranged than reach commitment, while committed
 * attacks must expose a clearer lane. Damage is intentionally held at a diagnostic 1.0 so this
 * checkpoint can prove authoritative hit delivery without pretending a balance pass happened.
 * Replace/tune these values only from captured Minecraft field-play evidence.</p>
 */
public final class PlayerWeaponFieldImpactProfile {
    private static final Map<ContentId, Profile> PROFILES = Map.of(
        ContentId.parse("riftfrontier:attack/player/mobile_pressure_entry"),
        new Profile(Shape.FORWARD_ARC, 2.4D, 1.45D, 1.35D, 1.0F),
        ContentId.parse("riftfrontier:attack/player/mobile_pressure_finisher"),
        new Profile(Shape.FORWARD_LANE, 2.9D, 1.10D, 1.40D, 1.0F),
        ContentId.parse("riftfrontier:attack/player/reach_commitment_strike"),
        new Profile(Shape.FORWARD_LANE, 4.0D, 0.85D, 1.45D, 1.0F)
    );

    private PlayerWeaponFieldImpactProfile() {}

    public static Optional<Profile> find(ContentId patternId) {
        return Optional.ofNullable(PROFILES.get(Objects.requireNonNull(patternId, "patternId")));
    }

    public enum Shape {
        FORWARD_ARC,
        FORWARD_LANE
    }

    public record Profile(
        Shape shape,
        double reach,
        double halfWidth,
        double verticalRadius,
        float diagnosticDamage
    ) {
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
            if (!Float.isFinite(diagnosticDamage) || diagnosticDamage <= 0.0F) {
                throw new IllegalArgumentException("diagnosticDamage must be finite and > 0");
            }
        }
    }
}
