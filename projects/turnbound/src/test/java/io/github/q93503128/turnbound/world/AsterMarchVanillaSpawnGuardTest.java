package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsterMarchVanillaSpawnGuardTest {
    @Test
    void everyAuthoredRegionIsCoveredByTheSpawnGuard() {
        for (AsterMarchRegionCatalog.Region region : AsterMarchRegionCatalog.regions()) {
            double centerX = (region.minX() + region.maxX()) / 2.0;
            double centerZ = (region.minZ() + region.maxZ()) / 2.0;
            assertTrue(AsterMarchVanillaSpawnGuard.insideAuthoredSpace(centerX, centerZ), region.id());
            assertTrue(AsterMarchVanillaSpawnGuard.insideAuthoredRegion(centerX, centerZ), region.id());
        }
    }

    @Test
    void everyAuthoredTransitCorridorIsCoveredIncludingSegmentMidpoints() {
        for (AsterMarchRegionCatalog.TransitCorridor corridor : AsterMarchRegionCatalog.transitCorridors()) {
            for (AsterMarchRegionCatalog.TransitPoint point : corridor.points()) {
                assertTrue(AsterMarchVanillaSpawnGuard.insideAuthoredSpace(point.x(), point.z()), corridor.id());
            }
            for (int i = 0; i < corridor.points().size() - 1; i++) {
                AsterMarchRegionCatalog.TransitPoint a = corridor.points().get(i);
                AsterMarchRegionCatalog.TransitPoint b = corridor.points().get(i + 1);
                assertTrue(AsterMarchVanillaSpawnGuard.insideAuthoredSpace(
                        (a.x() + b.x()) / 2.0,
                        (a.z() + b.z()) / 2.0), corridor.id() + " segment " + i);
            }
        }
    }

    @Test
    void relayRoadGapIsClaimedEvenOutsideChapterRectangles() {
        // This point is on the long authored Radia -> Old Relay road but outside both region rectangles.
        assertFalse(AsterMarchVanillaSpawnGuard.insideAuthoredRegion(202.0, -132.0));
        assertTrue(AsterMarchVanillaSpawnGuard.insideAuthoredSpace(202.0, -132.0));
    }

    @Test
    void smallGateSeamsAreClaimedEvenWhereRegionRectanglesDoNotOverlap() {
        assertFalse(AsterMarchVanillaSpawnGuard.insideAuthoredRegion(0.0, -116.0));
        assertTrue(AsterMarchVanillaSpawnGuard.insideAuthoredSpace(0.0, -116.0));

        assertFalse(AsterMarchVanillaSpawnGuard.insideAuthoredRegion(-129.0, 20.0));
        assertTrue(AsterMarchVanillaSpawnGuard.insideAuthoredSpace(-129.0, 20.0));
    }

    @Test
    void unrelatedOverworldSpaceIsNotClaimedByTheGuard() {
        assertFalse(AsterMarchVanillaSpawnGuard.insideAuthoredSpace(1000.0, 1000.0));
        assertFalse(AsterMarchVanillaSpawnGuard.insideAuthoredSpace(-1000.0, -1000.0));
    }
}
