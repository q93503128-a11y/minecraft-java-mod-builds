package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.moonseungjun.openworldrpg.combat.runtime.ProjectUltimateChargeRuntime;
import org.junit.jupiter.api.Test;

class ProjectUltimateChargeRuntimeTest {
    @Test
    void supportStepsUseFivePercentAndCapAtSixChargeWorth() {
        assertEquals(
                0,
                ProjectUltimateChargeRuntime.supportSteps(
                        4.99,
                        100.0
                )
        );
        assertEquals(
                1,
                ProjectUltimateChargeRuntime.supportSteps(
                        5.0,
                        100.0
                )
        );
        assertEquals(
                2,
                ProjectUltimateChargeRuntime.supportSteps(
                        10.0,
                        100.0
                )
        );
        assertEquals(
                3,
                ProjectUltimateChargeRuntime.supportSteps(
                        100.0,
                        100.0
                )
        );
    }
}
