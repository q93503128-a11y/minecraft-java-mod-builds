package kr.moonseungjun.riftfrontier.combat;

import java.util.Optional;

/** Pure horizontal projection used by the Region 01 field harness when a local-pressure attack displaces targets. */
public final class Region01BossFieldImpulsePolicy {
    private static final double MIN_DIRECTION_LENGTH = 1.0E-9D;

    private Region01BossFieldImpulsePolicy() {}

    public static Optional<HorizontalImpulse> radial(double offsetX, double offsetZ, double strength) {
        if (!Double.isFinite(offsetX) || !Double.isFinite(offsetZ)) {
            throw new IllegalArgumentException("radial impulse offsets must be finite");
        }
        if (!Double.isFinite(strength) || strength < 0.0D) {
            throw new IllegalArgumentException("radial impulse strength must be finite and >= 0");
        }
        if (strength == 0.0D) {
            return Optional.empty();
        }

        double length = Math.hypot(offsetX, offsetZ);
        if (length < MIN_DIRECTION_LENGTH) {
            // Do not invent an arbitrary launch direction for a target exactly on the pressure origin.
            return Optional.empty();
        }
        return Optional.of(new HorizontalImpulse(
            offsetX / length * strength,
            offsetZ / length * strength
        ));
    }

    public record HorizontalImpulse(double x, double z) {
        public HorizontalImpulse {
            if (!Double.isFinite(x) || !Double.isFinite(z)) {
                throw new IllegalArgumentException("horizontal impulse components must be finite");
            }
        }
    }
}
