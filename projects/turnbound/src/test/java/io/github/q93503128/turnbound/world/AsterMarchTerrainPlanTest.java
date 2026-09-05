package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AsterMarchTerrainPlanTest {
    @Test
    void rebuiltRadiaKeepsCanonicalTraversalSeamsOnLand() {
        assertTrue(AsterMarchTerrainPlan.radiaLand(0, -108));
        assertEquals(65, AsterMarchTerrainPlan.radiaSurfaceY(0, -108));

        assertTrue(AsterMarchTerrainPlan.radiaLand(-124, 20));
        assertEquals(65, AsterMarchTerrainPlan.radiaSurfaceY(-124, 20));

        assertTrue(AsterMarchTerrainPlan.radiaLand(124, -80));
        assertEquals(65, AsterMarchTerrainPlan.radiaSurfaceY(124, -80));

        assertTrue(AsterMarchTerrainPlan.radiaLand(0, 104));
        assertTrue(AsterMarchTerrainPlan.radiaSurfaceY(0, 104) >= 64);
    }

    @Test
    void harborIsWaterWhileCentralTownRemainsElevatedLand() {
        // x=0 is the authored north causeway through the inlet, so sample open water beside it.
        assertFalse(AsterMarchTerrainPlan.radiaLand(20, -82));
        assertEquals(AsterMarchTerrainPlan.Kind.RADIA_WATER, AsterMarchTerrainPlan.column(20, -82).kind());
        assertEquals(AsterMarchTerrainPlan.RADIA_SEA_Y, AsterMarchTerrainPlan.column(20, -82).surfaceY());

        assertTrue(AsterMarchTerrainPlan.radiaLand(0, 12));
        int centerY = AsterMarchTerrainPlan.radiaSurfaceY(0, 12);
        assertTrue(centerY >= 74 && centerY <= 76);
    }

    @Test
    void radiaAuthoredHeightsStayInsideRuntimeContainmentEnvelope() {
        for (int x = AsterMarchRegionCatalog.RADIA.minX(); x <= AsterMarchRegionCatalog.RADIA.maxX(); x += 8) {
            for (int z = AsterMarchRegionCatalog.RADIA.minZ(); z <= AsterMarchRegionCatalog.RADIA.maxZ(); z += 8) {
                if (!AsterMarchTerrainPlan.radiaLand(x, z)) continue;
                int y = AsterMarchTerrainPlan.radiaSurfaceY(x, z);
                assertTrue(y >= 64 && y <= 76, "out-of-range Radia height at " + x + "," + z + ": " + y);
            }
        }
    }
}
