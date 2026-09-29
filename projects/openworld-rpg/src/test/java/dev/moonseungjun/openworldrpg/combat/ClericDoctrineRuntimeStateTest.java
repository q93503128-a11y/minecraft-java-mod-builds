package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.moonseungjun.openworldrpg.combat.state.ClericDoctrineRuntimeState;
import org.junit.jupiter.api.Test;

class ClericDoctrineRuntimeStateTest {
    private static final double EPSILON = 0.0001;

    @Test
    void damagingThenSupportConsumesTenPercentAndPrimesBack() {
        var state = new ClericDoctrineRuntimeState();

        state.afterDamagingActive(100L);
        assertEquals(
                ClericDoctrineRuntimeState.Primed.HEALING_PROTECTION,
                state.primed(100L)
        );
        assertEquals(
                1.10,
                state.consumeHealingProtectionMultiplier(219L),
                EPSILON
        );
        state.afterHealingProtectionActive(219L);
        assertEquals(
                1.08,
                state.consumeDirectDamageMultiplier(220L),
                EPSILON
        );
    }

    @Test
    void onlyOneSideIsPrimedAndWindowExpiresAtSixSeconds() {
        var state = new ClericDoctrineRuntimeState();

        state.afterDamagingActive(10L);
        state.afterHealingProtectionActive(20L);
        assertEquals(
                1.0,
                state.consumeHealingProtectionMultiplier(21L),
                EPSILON
        );
        assertEquals(
                1.08,
                state.consumeDirectDamageMultiplier(139L),
                EPSILON
        );

        state.afterDamagingActive(200L);
        assertEquals(
                1.0,
                state.consumeHealingProtectionMultiplier(320L),
                EPSILON
        );
    }
}
