package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.encounter.r01.R01EarthloongFurrowGeometry;
import org.junit.jupiter.api.Test;

class R01EarthloongFurrowGeometryTest {
    @Test
    void committedAxisProducesCanonicalLongitudinalAndLateralCoordinates() {
        var p = R01EarthloongFurrowGeometry.coordinates(
                10.0, 20.0,
                0.0, 1.0,
                12.5, 26.0
        );
        assertEquals(6.0, p.longitudinal(), 0.0001);
        assertEquals(-2.5, p.lateral(), 0.0001);
        assertTrue(R01EarthloongFurrowGeometry.insideLane(p, -2.5, 0.7, 12.0));
        assertFalse(R01EarthloongFurrowGeometry.insideLane(p, 0.0, 0.7, 12.0));
    }

    @Test
    void pointsBehindOrBeyondCommittedLengthAreNotHit() {
        var behind = R01EarthloongFurrowGeometry.coordinates(0, 0, 1, 0, -1, 0);
        var beyond = R01EarthloongFurrowGeometry.coordinates(0, 0, 1, 0, 12.1, 0);
        assertFalse(R01EarthloongFurrowGeometry.insideLane(behind, 0, 0.7, 12));
        assertFalse(R01EarthloongFurrowGeometry.insideLane(beyond, 0, 0.7, 12));
    }
}
