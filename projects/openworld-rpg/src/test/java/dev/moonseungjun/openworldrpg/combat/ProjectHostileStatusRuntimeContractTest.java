package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.moonseungjun.openworldrpg.combat.runtime.ProjectHostileStatusRuntime;
import org.junit.jupiter.api.Test;

class ProjectHostileStatusRuntimeContractTest {
    @Test
    void rebukedNumbersMatchClericCanon() {
        assertEquals(
                0.85,
                ProjectHostileStatusRuntime.REBUKED_STANDARD_MULTIPLIER,
                0.0001
        );
        assertEquals(
                0.92,
                ProjectHostileStatusRuntime.REBUKED_BOSS_MULTIPLIER,
                0.0001
        );
        assertEquals(
                80L,
                ProjectHostileStatusRuntime.REBUKED_STANDARD_TICKS
        );
        assertEquals(
                60L,
                ProjectHostileStatusRuntime.REBUKED_BOSS_TICKS
        );
        assertEquals(
                20L,
                ProjectHostileStatusRuntime.REBUKED_EMPOWERED_BONUS_TICKS
        );
    }

    @Test
    void snaredNumbersMatchHunterCanon() {
        assertEquals(
                0.65,
                ProjectHostileStatusRuntime
                        .SNARED_STANDARD_MOVEMENT_MULTIPLIER,
                0.0001
        );
        assertEquals(
                0.80,
                ProjectHostileStatusRuntime
                        .SNARED_MINIBOSS_MOVEMENT_MULTIPLIER,
                0.0001
        );
        assertEquals(
                0.88,
                ProjectHostileStatusRuntime
                        .SNARED_BOSS_MOVEMENT_MULTIPLIER,
                0.0001
        );
        assertEquals(
                60L,
                ProjectHostileStatusRuntime.SNARED_STANDARD_TICKS
        );
        assertEquals(
                50L,
                ProjectHostileStatusRuntime.SNARED_MINIBOSS_TICKS
        );
        assertEquals(
                40L,
                ProjectHostileStatusRuntime.SNARED_BOSS_TICKS
        );
        assertEquals(
                30L,
                ProjectHostileStatusRuntime
                        .SNARED_EMPOWERED_NON_BOSS_BONUS_TICKS
        );
        assertEquals(
                10L,
                ProjectHostileStatusRuntime
                        .SNARED_EMPOWERED_BOSS_BONUS_TICKS
        );
    }
}
