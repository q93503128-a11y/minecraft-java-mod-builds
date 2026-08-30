package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AsterMarchRegionCatalogTest {
    @Test
    void v04MajorAnchorsRemainExact() {
        var radia = AsterMarchRegionCatalog.fastTravelPoint(AsterMarchRegionCatalog.FT_RADIA);
        assertEquals(0.0, radia.x());
        assertEquals(66.0, radia.y());
        assertEquals(20.0, radia.z());
        assertEquals(180.0F, radia.yaw());

        var meadow = AsterMarchRegionCatalog.fastTravelPoint(AsterMarchRegionCatalog.FT_MEADOW);
        assertEquals(190.0, meadow.x());
        assertEquals(67.0, meadow.y());
        assertEquals(230.0, meadow.z());

        var graul = AsterMarchRegionCatalog.bossPoint(AsterMarchRegionCatalog.B01);
        assertEquals(355.0, graul.x());
        assertEquals(68.0, graul.y());
        assertEquals(245.0, graul.z());
        assertEquals(90.0F, graul.yaw());
    }

    @Test
    void canonicalRegionBoundsMatchV04() {
        assertTrue(AsterMarchRegionCatalog.RADIA.contains(0, 20));
        assertTrue(AsterMarchRegionCatalog.SOUTHGATE.contains(355, 245));
        assertTrue(AsterMarchRegionCatalog.GLOAMWOOD.contains(-40, -300));
        assertFalse(AsterMarchRegionCatalog.SOUTHGATE.contains(-40, -300));
        assertEquals(-80, AsterMarchRegionCatalog.SOUTHGATE.minX());
        assertEquals(430, AsterMarchRegionCatalog.SOUTHGATE.maxX());
        assertEquals(120, AsterMarchRegionCatalog.SOUTHGATE.minZ());
        assertEquals(360, AsterMarchRegionCatalog.SOUTHGATE.maxZ());
    }

    @Test
    void futureRegionAnchorsAreAlreadyPinnedWithoutClaimingTheirRuntimeImplementation() {
        var gloam = AsterMarchRegionCatalog.fastTravelPoint(AsterMarchRegionCatalog.FT_GLOAM);
        assertEquals(-40.0, gloam.x());
        assertEquals(70.0, gloam.y());
        assertEquals(-300.0, gloam.z());
        var b02 = AsterMarchRegionCatalog.bossPoint(AsterMarchRegionCatalog.B02);
        assertEquals(-35.0, b02.x());
        assertEquals(72.0, b02.y());
        assertEquals(-440.0, b02.z());
        assertEquals(180.0F, b02.yaw());
    }
}
