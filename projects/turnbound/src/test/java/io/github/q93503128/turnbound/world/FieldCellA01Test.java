package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldCellA01Test {
    @Test
    void firstVerticalSliceCellLivesInsideCanonicalSouthgateMeadowCoordinates() {
        assertEquals(64, FieldCellA01.SIZE);
        assertEquals(-32, FieldCellA01.ORIGIN_X);
        assertEquals(128, FieldCellA01.ORIGIN_Z);
        assertEquals(64, FieldCellA01.BASE_Y);
        assertTrue(FieldCellA01.ORIGIN_X >= -80);
        assertTrue(FieldCellA01.ORIGIN_X + FieldCellA01.SIZE - 1 <= 430);
        assertTrue(FieldCellA01.ORIGIN_Z >= 120);
        assertTrue(FieldCellA01.ORIGIN_Z + FieldCellA01.SIZE - 1 <= 360);
    }

    @Test
    void authoredRoadConnectsRadiaEdgeToFutureSouthCellWithoutAProceduralExit() {
        for (int z = 0; z < FieldCellA01.SIZE; z++) {
            int x = FieldCellA01.roadCenterX(z);
            assertTrue(x >= 28 && x <= 36);
        }
        assertTrue(FieldCellA01.isRadiaGate(32, 0));
        assertFalse(FieldCellA01.isRadiaGate(20, 0));
        assertTrue(FieldCellA01.isFutureSouthGate(32, 63));
    }
}
