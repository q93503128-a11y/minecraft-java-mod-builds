package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.moonseungjun.openworldrpg.combat.runtime.ProjectUltimateChargeRuntime;
import org.junit.jupiter.api.Test;

class ProjectUltimateChargeRuntimeTest {
    @Test
    void warriorChargeEventsMatchRootCanon() {
        assertEquals(
                2.0,
                ProjectUltimateChargeRuntime
                        .WARRIOR_BASIC_CYCLE_CHARGE,
                0.0001
        );
        assertEquals(
                3.0,
                ProjectUltimateChargeRuntime
                        .WARRIOR_ACTIVE_HIT_CHARGE,
                0.0001
        );
        assertEquals(
                5.0,
                ProjectUltimateChargeRuntime
                        .WARRIOR_PERFECT_GUARD_CHARGE,
                0.0001
        );
        assertEquals(
                10.0,
                ProjectUltimateChargeRuntime
                        .WARRIOR_POISE_BREAK_CHARGE,
                0.0001
        );
    }

    @Test
    void hunterChargeEventsMatchRootCanon() {
        assertEquals(
                2.0,
                ProjectUltimateChargeRuntime
                        .HUNTER_RANGED_QUARRY_HIT_CHARGE,
                0.0001
        );
        assertEquals(
                1.0,
                ProjectUltimateChargeRuntime
                        .HUNTER_LONG_RANGE_BONUS_CHARGE,
                0.0001
        );
        assertEquals(
                3.0,
                ProjectUltimateChargeRuntime
                        .HUNTER_WEAK_POINT_HIT_CHARGE,
                0.0001
        );
        assertEquals(
                6.0,
                ProjectUltimateChargeRuntime
                        .HUNTER_RANGED_POISE_BREAK_CHARGE,
                0.0001
        );
        assertEquals(
                3.0,
                ProjectUltimateChargeRuntime
                        .HUNTER_ACTIVE_QUARRY_HIT_CHARGE,
                0.0001
        );
    }

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
