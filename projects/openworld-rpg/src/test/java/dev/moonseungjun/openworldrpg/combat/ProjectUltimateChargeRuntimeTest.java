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
    void mageChargeEventsMatchRootCanon() {
        assertEquals(
                3.0,
                ProjectUltimateChargeRuntime.MAGE_PRIMARY_ACTIVE_HIT_CHARGE,
                0.0001
        );
        assertEquals(
                0.5,
                ProjectUltimateChargeRuntime.MAGE_ADDITIONAL_ACTIVE_HIT_CHARGE,
                0.0001
        );
        assertEquals(
                6.0,
                ProjectUltimateChargeRuntime.MAGE_WEAVE_COMPLETION_CHARGE,
                0.0001
        );
        assertEquals(
                2.0,
                ProjectUltimateChargeRuntime.MAGE_MEANINGFUL_CONTROL_CHARGE,
                0.0001
        );
        assertEquals(
                80L,
                ProjectUltimateChargeRuntime.MAGE_CONTROL_ICD_TICKS
        );
    }

    @Test
    void guardianChargeEventsMatchRootCanon() {
        assertEquals(
                2.0,
                ProjectUltimateChargeRuntime.GUARDIAN_GUARDED_HIT_CHARGE,
                0.0001
        );
        assertEquals(
                6.0,
                ProjectUltimateChargeRuntime.GUARDIAN_PERFECT_GUARD_CHARGE,
                0.0001
        );
        assertEquals(
                2.0,
                ProjectUltimateChargeRuntime.GUARDIAN_BARRIER_STEP_CHARGE,
                0.0001
        );
        assertEquals(
                2.0,
                ProjectUltimateChargeRuntime.GUARDIAN_PROVOKED_HIT_CHARGE,
                0.0001
        );
        assertEquals(
                12.0,
                ProjectUltimateChargeRuntime
                        .GUARDIAN_GUARDED_HIT_STAMINA_THRESHOLD,
                0.0001
        );
        assertEquals(
                40L,
                ProjectUltimateChargeRuntime
                        .GUARDIAN_PROVOKED_HIT_ICD_TICKS
        );
        assertEquals(
                3,
                ProjectUltimateChargeRuntime
                        .MAX_GUARDIAN_BARRIER_STEPS_PER_SOURCE_RECIPIENT
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
