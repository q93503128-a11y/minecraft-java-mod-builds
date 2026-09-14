package kr.moonseungjun.riftfrontier.combat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Region01BossFieldTelegraphEmitterTest {
    private static final double EPSILON = 1.0E-9D;

    @Test
    void localPressureBoundaryMatchesTheAuthoredThreatRadius() {
        var profile = Region01BossFieldImpactProfile.find(Region01BossFieldImpactProfile.ARENA_PRESSURE).orElseThrow();
        List<Region01BossFieldTelegraphGeometry.LocalPoint> points = Region01BossFieldTelegraphGeometry.sampleBoundary(profile);

        assertFalse(points.isEmpty());
        for (var point : points) {
            assertEquals(profile.reach(), Math.hypot(point.forward(), point.lateral()), EPSILON);
        }
    }

    @Test
    void lineBoundaryStaysInsideTheExactLaneAndShowsItsForwardCap() {
        var profile = Region01BossFieldImpactProfile.find(Region01BossFieldImpactProfile.LINE_DISPLACEMENT).orElseThrow();
        List<Region01BossFieldTelegraphGeometry.LocalPoint> points = Region01BossFieldTelegraphGeometry.sampleBoundary(profile);

        assertFalse(points.isEmpty());
        assertTrue(points.stream().anyMatch(point -> Math.abs(point.forward() - profile.reach()) <= EPSILON));
        for (var point : points) {
            assertTrue(point.forward() >= -EPSILON && point.forward() <= profile.reach() + EPSILON);
            assertTrue(Math.abs(point.lateral()) <= profile.halfWidth() + EPSILON);
        }
    }

    @Test
    void committedStrikeBoundaryNeverClaimsSpaceOutsideItsArcProfile() {
        var profile = Region01BossFieldImpactProfile.find(Region01BossFieldImpactProfile.COMMITTED_STRIKE).orElseThrow();
        List<Region01BossFieldTelegraphGeometry.LocalPoint> points = Region01BossFieldTelegraphGeometry.sampleBoundary(profile);

        assertFalse(points.isEmpty());
        for (var point : points) {
            assertTrue(point.forward() >= -EPSILON && point.forward() <= profile.reach() + EPSILON);
            assertTrue(Math.abs(point.lateral()) <= profile.halfWidth() + EPSILON);
            assertTrue(Math.hypot(point.forward(), point.lateral()) <= profile.reach() + EPSILON);
        }
    }
}
