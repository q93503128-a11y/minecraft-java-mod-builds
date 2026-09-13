package kr.moonseungjun.riftfrontier.combat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Region01BossFieldReadabilityProjectionTest {
    @Test
    void localAreaSamplesStayOnAuthoredRadius() {
        var profile = new Region01BossFieldImpactProfile.Profile(
            Region01BossFieldImpactProfile.Shape.LOCAL_AREA,
            4.5D,
            4.5D,
            1.7D
        );

        List<Region01BossFieldReadabilityProjection.LocalSample> samples =
            Region01BossFieldReadabilityProjection.samples(profile);

        assertEquals(20, samples.size());
        for (var sample : samples) {
            assertEquals(profile.reach(), Math.hypot(sample.forward(), sample.lateral()), 1.0E-9D);
        }
    }

    @Test
    void forwardLaneShowsBothExactSideBoundariesThroughFullReach() {
        var profile = new Region01BossFieldImpactProfile.Profile(
            Region01BossFieldImpactProfile.Shape.FORWARD_LANE,
            6.0D,
            1.15D,
            1.7D
        );

        List<Region01BossFieldReadabilityProjection.LocalSample> samples =
            Region01BossFieldReadabilityProjection.samples(profile);

        assertTrue(samples.stream().anyMatch(sample ->
            Math.abs(sample.forward() - profile.reach()) < 1.0E-9D
                && Math.abs(sample.lateral() - profile.halfWidth()) < 1.0E-9D));
        assertTrue(samples.stream().anyMatch(sample ->
            Math.abs(sample.forward() - profile.reach()) < 1.0E-9D
                && Math.abs(sample.lateral() + profile.halfWidth()) < 1.0E-9D));
        assertTrue(samples.stream().allMatch(sample ->
            sample.forward() >= 0.0D && sample.forward() <= profile.reach() + 1.0E-9D));
    }

    @Test
    void forwardArcSamplesNeverClaimSpaceOutsideServerEnvelope() {
        var profile = new Region01BossFieldImpactProfile.Profile(
            Region01BossFieldImpactProfile.Shape.FORWARD_ARC,
            3.4D,
            2.2D,
            1.7D
        );

        List<Region01BossFieldReadabilityProjection.LocalSample> samples =
            Region01BossFieldReadabilityProjection.samples(profile);

        assertTrue(samples.stream().allMatch(sample -> sample.forward() >= 0.0D));
        assertTrue(samples.stream().allMatch(sample -> Math.abs(sample.lateral()) <= profile.halfWidth() + 1.0E-9D));
        assertTrue(samples.stream().allMatch(sample ->
            Math.hypot(sample.forward(), sample.lateral()) <= profile.reach() + 1.0E-9D));
        assertTrue(samples.stream().anyMatch(sample ->
            Math.abs(sample.forward() - profile.reach()) < 1.0E-9D && Math.abs(sample.lateral()) < 1.0E-9D));
    }
}
