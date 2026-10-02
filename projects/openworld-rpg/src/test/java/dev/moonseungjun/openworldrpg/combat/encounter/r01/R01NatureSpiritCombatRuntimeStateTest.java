package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class R01NatureSpiritCombatRuntimeStateTest {
    private static final UUID ACTOR =
            UUID.fromString("87654321-1234-5678-9abc-def012345678");

    @Test
    void rollingDamageWindowTriggersLivingShellAtExactTwentyPercent() {
        var state = new R01NatureSpiritCombatRuntimeState(
                "nature-shell-damage",
                ACTOR
        );

        state.recordPostMitigationHostileDamage(79.0, 10);
        var below = state.selectAtDecision(
                true, true, true,
                3.0,
                78.0, 78.0,
                790.0,
                20
        );
        assertFalse(
                below.mode()
                        == R01NatureSpiritActionController.Mode.ENTER_LIVING_SHELL
        );

        state.recordPostMitigationHostileDamage(79.0, 20);
        var entered = state.selectAtDecision(
                true, true, true,
                3.0,
                78.0, 78.0,
                790.0,
                20
        );
        assertEquals(
                R01NatureSpiritActionController.Mode.ENTER_LIVING_SHELL,
                entered.mode()
        );
        assertEquals(
                0.65,
                state.directDamageTakenMultiplier(21),
                0.000001
        );
        assertEquals(
                1.25,
                state.poiseDamageTakenMultiplier(21),
                0.000001
        );
    }

    @Test
    void rollingDamageWindowExpiresAtFourSeconds() {
        var state = new R01NatureSpiritCombatRuntimeState(
                "nature-shell-window",
                ACTOR
        );
        state.recordPostMitigationHostileDamage(158.0, 10);

        assertEquals(
                0.20,
                state.recentHostileDamageFraction(790.0, 89),
                0.000001
        );
        assertEquals(
                0.0,
                state.recentHostileDamageFraction(790.0, 90),
                0.000001
        );
    }

    @Test
    void exactFortyPercentPoiseThresholdTriggersShellAndBreakEndsIt() {
        var state = new R01NatureSpiritCombatRuntimeState(
                "nature-shell-poise",
                ACTOR
        );

        var entered = state.selectAtDecision(
                true, true, true,
                3.0,
                31.2, 78.0,
                790.0,
                100
        );
        assertEquals(
                R01NatureSpiritActionController.Mode.ENTER_LIVING_SHELL,
                entered.mode()
        );
        assertTrue(state.onPoiseBroken(120));
        assertEquals(
                1.0,
                state.directDamageTakenMultiplier(120),
                0.000001
        );
        assertEquals(
                1.0,
                state.poiseDamageTakenMultiplier(120),
                0.000001
        );

        var next = state.selectAtDecision(
                true, false, true,
                3.0,
                78.0, 78.0,
                790.0,
                121
        );
        assertFalse(next.forcedAfterShell());
    }

    @Test
    void naturalShellEndKeepsForcedBloomQuakeContract() {
        var state = new R01NatureSpiritCombatRuntimeState(
                "nature-shell-natural-end",
                ACTOR
        );
        state.recordPostMitigationHostileDamage(158.0, 0);
        state.selectAtDecision(
                true, true, true,
                3.0,
                78.0, 78.0,
                790.0,
                0
        );

        var forced = state.selectAtDecision(
                true, true, true,
                4.0,
                78.0, 78.0,
                790.0,
                50
        );
        assertEquals(
                R01SecondaryCreatureEncounterData.ActionId.BLOOM_QUAKE,
                forced.action().orElseThrow()
        );
        assertTrue(forced.forcedAfterShell());
    }
}
