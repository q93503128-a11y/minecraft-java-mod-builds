package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class R01NatureSpiritBloomQuakeAreaTest {
    @Test
    void exactFourBlockHorizontalBoundaryIsInside() {
        assertTrue(R01NatureSpiritCombatRuntime.insideHorizontalRadius(
                10.0,
                -5.0,
                14.0,
                -5.0,
                4.0
        ));
        assertTrue(R01NatureSpiritCombatRuntime.insideHorizontalRadius(
                10.0,
                -5.0,
                10.0,
                -1.0,
                4.0
        ));
        assertTrue(R01NatureSpiritCombatRuntime.insideHorizontalRadius(
                0.0,
                0.0,
                2.4,
                3.2,
                4.0
        ));
    }

    @Test
    void targetOutsideFourBlockGroundRingIsRejected() {
        assertFalse(R01NatureSpiritCombatRuntime.insideHorizontalRadius(
                0.0,
                0.0,
                4.000001,
                0.0,
                4.0
        ));
        assertFalse(R01NatureSpiritCombatRuntime.insideHorizontalRadius(
                0.0,
                0.0,
                3.0,
                3.0,
                4.0
        ));
    }

    @Test
    void invalidAreaInputsFailClosed() {
        assertThrows(
                IllegalArgumentException.class,
                () -> R01NatureSpiritCombatRuntime.insideHorizontalRadius(
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        -1.0
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> R01NatureSpiritCombatRuntime.insideHorizontalRadius(
                        Double.NaN,
                        0.0,
                        0.0,
                        0.0,
                        4.0
                )
        );
    }
}
