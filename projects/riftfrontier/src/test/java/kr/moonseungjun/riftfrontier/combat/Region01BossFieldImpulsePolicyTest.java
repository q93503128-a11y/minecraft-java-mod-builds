package kr.moonseungjun.riftfrontier.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Region01BossFieldImpulsePolicyTest {
    private static final double EPSILON = 1.0E-9D;

    @Test
    void radialImpulsePreservesRequestedMagnitudeAndDirection() {
        var impulse = Region01BossFieldImpulsePolicy.radial(3.0D, 4.0D, 0.85D).orElseThrow();

        assertEquals(0.51D, impulse.x(), EPSILON);
        assertEquals(0.68D, impulse.z(), EPSILON);
        assertEquals(0.85D, Math.hypot(impulse.x(), impulse.z()), EPSILON);
    }

    @Test
    void zeroStrengthOrCoincidentTargetProducesNoInventedDirection() {
        assertTrue(Region01BossFieldImpulsePolicy.radial(1.0D, 0.0D, 0.0D).isEmpty());
        assertTrue(Region01BossFieldImpulsePolicy.radial(0.0D, 0.0D, 0.85D).isEmpty());
    }

    @Test
    void invalidInputsFailClosed() {
        assertThrows(IllegalArgumentException.class, () -> Region01BossFieldImpulsePolicy.radial(1.0D, 0.0D, -0.1D));
        assertThrows(IllegalArgumentException.class, () -> Region01BossFieldImpulsePolicy.radial(Double.NaN, 0.0D, 0.5D));
        assertThrows(IllegalArgumentException.class, () -> Region01BossFieldImpulsePolicy.radial(0.0D, Double.POSITIVE_INFINITY, 0.5D));
    }
}
