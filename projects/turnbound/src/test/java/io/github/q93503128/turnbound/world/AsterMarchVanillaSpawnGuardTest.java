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
            assertTrue(AsterMarchVanillaSpawnGuard.insideAuthoredRegion(centerX, centerZ), region.id());
        }
    }

    @Test
    void unrelatedOverworldSpaceIsNotClaimedByTheGuard() {
        assertFalse(AsterMarchVanillaSpawnGuard.insideAuthoredRegion(1000.0, 1000.0));
        assertFalse(AsterMarchVanillaSpawnGuard.insideAuthoredRegion(-1000.0, -1000.0));
    }
}
