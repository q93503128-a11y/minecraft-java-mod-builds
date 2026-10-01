package dev.moonseungjun.openworldrpg.combat.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MagePhaseStepRuntimeTest {
    @Test
    void pathLimitStopsBeforeSolidCollisionAndNeverExceedsFiveBlocks() {
        assertEquals(
                4.55,
                MagePhaseStepRuntime.pathLimitBeforeWall(
                        5.0,
                        0.40
                ),
                0.0001
        );
        assertEquals(
                5.0,
                MagePhaseStepRuntime.pathLimitBeforeWall(
                        20.0,
                        0.30
                ),
                0.0001
        );
        assertEquals(
                0.0,
                MagePhaseStepRuntime.pathLimitBeforeWall(
                        0.20,
                        0.30
                ),
                0.0001
        );
    }

    @Test
    void invalidPathInputsFailClosed() {
        assertThrows(
                IllegalArgumentException.class,
                () -> MagePhaseStepRuntime.pathLimitBeforeWall(
                        Double.NaN,
                        0.3
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> MagePhaseStepRuntime.pathLimitBeforeWall(
                        3.0,
                        -0.1
                )
        );
    }
}
