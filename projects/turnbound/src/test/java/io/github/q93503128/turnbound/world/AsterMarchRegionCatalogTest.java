package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AsterMarchRegionCatalogTest {
    @Test
    void v04MajorAnchorsRemainExact() {
        var radia = AsterMarchRegionCatalog.fastTravel(AsterMarchRegionCatalog.FT_RADIA);
        assertEquals(0.0, radia.position().x);
        assertEquals(66.0, radia.position().y);
        assertEquals(20.0, radia.position().z);

        var meadow = AsterMarchRegionCatalog.fastTravel(AsterMarchRegionCatalog.FT_MEADOW);
        assertEquals(190.0, meadow.position().x);
        assertEquals(67.0, meadow.position().y);
        assertEquals(230.0, meadow.position().z);

        var graul = AsterMarchRegionCatalog.boss(AsterMarchRegionCatalog.B01);
        assertEquals(355.0, graul.position().x);
        assertEquals(68.0, graul.position().y);
        assertEquals(245.0, graul.position().z);
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
    void authoredRibbonRespectsQuestGates() {
        assertTrue(SouthgateMeadowExpansion.allowedPosition(
                AsterMarchRegionCatalog.fastTravel(AsterMarchRegionCatalog.FT_RADIA).position(), false, false));
        assertFalse(SouthgateMeadowExpansion.allowedPosition(SouthgateMeadowExpansion.M04_CLEARING, false, false));
        assertTrue(SouthgateMeadowExpansion.allowedPosition(SouthgateMeadowExpansion.M04_CLEARING, true, false));
        assertFalse(SouthgateMeadowExpansion.allowedPosition(
                AsterMarchRegionCatalog.boss(AsterMarchRegionCatalog.B01).position(), true, false));
        assertTrue(SouthgateMeadowExpansion.allowedPosition(
                AsterMarchRegionCatalog.boss(AsterMarchRegionCatalog.B01).position(), true, true));
    }
}
