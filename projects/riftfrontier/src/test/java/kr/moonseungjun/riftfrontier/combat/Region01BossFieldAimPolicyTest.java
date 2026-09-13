package kr.moonseungjun.riftfrontier.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class Region01BossFieldAimPolicyTest {
    private static final double EPSILON = 1.0E-6D;

    @Test
    void mapsCardinalTargetsToMinecraftYaw() {
        assertEquals(0.0D, Region01BossFieldAimPolicy.committedYawDegrees(0, 0, 0, 5), EPSILON);
        assertEquals(-90.0D, Region01BossFieldAimPolicy.committedYawDegrees(0, 0, 5, 0), EPSILON);
        assertEquals(90.0D, Region01BossFieldAimPolicy.committedYawDegrees(0, 0, -5, 0), EPSILON);
        assertEquals(-180.0D, Region01BossFieldAimPolicy.committedYawDegrees(0, 0, 0, -5), EPSILON);
    }

    @Test
    void acquisitionRadiusIsExplicitlyBounded() {
        double radius = Region01BossFieldAimPolicy.TARGET_ACQUISITION_RADIUS;
        assertTrue(Region01BossFieldAimPolicy.withinAcquisitionRadius(radius * radius));
        assertFalse(Region01BossFieldAimPolicy.withinAcquisitionRadius(radius * radius + 0.001D));
        assertFalse(Region01BossFieldAimPolicy.withinAcquisitionRadius(-1.0D));
        assertFalse(Region01BossFieldAimPolicy.withinAcquisitionRadius(Double.NaN));
    }

    @Test
    void rejectsDegenerateOrNonFiniteAim() {
        assertThrows(IllegalArgumentException.class,
            () -> Region01BossFieldAimPolicy.committedYawDegrees(1, 2, 1, 2));
        assertThrows(IllegalArgumentException.class,
            () -> Region01BossFieldAimPolicy.committedYawDegrees(Double.NaN, 0, 1, 1));
    }
}
