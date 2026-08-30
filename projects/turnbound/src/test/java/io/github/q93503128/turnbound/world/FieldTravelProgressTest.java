package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.SouthgateEncounterCatalog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FieldTravelProgressTest {
    @Test
    void a01RelayCanActivateImmediatelyButA02RequiresChapterClear() {
        SouthgateChapterProgress progress = new SouthgateChapterProgress();
        assertTrue(progress.activateRelay(FieldTravelCatalog.RELAY_A01));
        assertTrue(progress.relayActivated(FieldTravelCatalog.RELAY_A01));
        assertFalse(progress.activateRelay(FieldTravelCatalog.RELAY_A02));
        assertFalse(progress.relayActivated(FieldTravelCatalog.RELAY_A02));

        for (String id : SouthgateEncounterCatalog.normalEncounterIds()) progress.recordVictory(id);
        progress.recordVictory(SouthgateEncounterCatalog.B01_GRAUL);

        assertTrue(progress.chapterCleared());
        assertTrue(progress.activateRelay(FieldTravelCatalog.RELAY_A02));
        assertTrue(progress.relayActivated(FieldTravelCatalog.RELAY_A02));
        assertFalse(progress.activateRelay(FieldTravelCatalog.RELAY_A02));
    }

    @Test
    void a02CellIsContiguousWithSouthgateA01() {
        assertEquals(FieldCellA01.ORIGIN_X, FieldCellA02.ORIGIN_X);
        assertEquals(FieldCellA01.ORIGIN_Z + FieldCellA01.SIZE, FieldCellA02.ORIGIN_Z);
        assertEquals(FieldCellA01.BASE_Y, FieldCellA02.BASE_Y);
        assertEquals(FieldCellA01.SIZE, FieldCellA02.SIZE);
    }
}
