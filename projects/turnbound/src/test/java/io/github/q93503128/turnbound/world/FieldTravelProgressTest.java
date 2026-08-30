package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.SouthgateEncounterCatalog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FieldTravelProgressTest {
    @Test
    void radiaIsImmediateAndMqC0101UnlocksCanonicalMeadowFastTravel() {
        SouthgateChapterProgress progress = new SouthgateChapterProgress();
        assertTrue(progress.activateRelay(FieldTravelCatalog.FT_RADIA));
        assertTrue(progress.relayActivated(FieldTravelCatalog.FT_RADIA));
        assertFalse(progress.activateRelay(FieldTravelCatalog.FT_MEADOW));
        assertFalse(progress.relayActivated(FieldTravelCatalog.FT_MEADOW));

        progress.recordVictory(SouthgateEncounterCatalog.ENC_M01);
        assertFalse(progress.relayActivated(FieldTravelCatalog.FT_MEADOW));
        progress.recordVictory(SouthgateEncounterCatalog.ENC_M02);

        assertTrue(progress.meadowRouteUnlocked());
        assertTrue(progress.relayActivated(FieldTravelCatalog.FT_MEADOW));
        assertFalse(progress.activateRelay(FieldTravelCatalog.FT_MEADOW));
        assertEquals(190.0, FieldTravelCatalog.destination(FieldTravelCatalog.FT_MEADOW).position().x);
        assertEquals(230.0, FieldTravelCatalog.destination(FieldTravelCatalog.FT_MEADOW).position().z);
    }

    @Test
    void a02CellIsContiguousWithSouthgateA01() {
        assertEquals(FieldCellA01.ORIGIN_X, FieldCellA02.ORIGIN_X);
        assertEquals(FieldCellA01.ORIGIN_Z + FieldCellA01.SIZE, FieldCellA02.ORIGIN_Z);
        assertEquals(FieldCellA01.BASE_Y, FieldCellA02.BASE_Y);
        assertEquals(FieldCellA01.SIZE, FieldCellA02.SIZE);
    }
}
