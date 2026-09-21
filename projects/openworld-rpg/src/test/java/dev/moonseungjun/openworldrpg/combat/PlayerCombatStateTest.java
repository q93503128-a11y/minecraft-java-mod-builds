package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.state.PlayerCombatState;
import org.junit.jupiter.api.Test;

class PlayerCombatStateTest {
    @Test
    void canonicalManaFormulaMatchesDesignAnchors() {
        assertEquals(100, PlayerCombatState.maxManaForWill(5));
        assertEquals(163, PlayerCombatState.maxManaForWill(30));
        assertEquals(208, PlayerCombatState.maxManaForWill(60));
        assertEquals(223, PlayerCombatState.maxManaForWill(80));

        assertEquals(4.0, PlayerCombatState.baseManaRegenPerSecondForWill(5), 0.0001);
        assertEquals(5.0, PlayerCombatState.baseManaRegenPerSecondForWill(30), 0.0001);
        assertEquals(6.2, PlayerCombatState.baseManaRegenPerSecondForWill(60), 0.0001);
        assertEquals(7.0, PlayerCombatState.baseManaRegenPerSecondForWill(80), 0.0001);
    }

    @Test
    void manaSpendLocksRegenForTwentyTicksThenDoublesAfterFiveSecondsOutOfCombat() {
        PlayerCombatState state = new PlayerCombatState(5, 0);
        assertTrue(state.spendMana(12, 0));
        assertEquals(88.0, state.mana(19), 0.0001);
        assertEquals(88.0, state.mana(20), 0.0001);

        assertEquals(92.0, state.mana(40), 0.0001);
        assertEquals(100.0, state.mana(120), 0.0001);
    }

    @Test
    void laterCombatActivityDelaysOutOfCombatManaBonus() {
        PlayerCombatState state = new PlayerCombatState(5, 0);
        assertTrue(state.spendMana(50, 0));
        state.markCombatActivity(80);

        assertEquals(66.0, state.mana(100), 0.0001);
        assertEquals(82.0, state.mana(180), 0.0001);
        assertEquals(90.0, state.mana(200), 0.0001);
    }

    @Test
    void willSynchronizationPreservesManaPercentageInsteadOfRefilling() {
        PlayerCombatState state = new PlayerCombatState(5, 0);
        assertTrue(state.spendMana(50, 0));

        state.synchronizeWill(30, 0);
        assertEquals(163, state.maxMana());
        assertEquals(81.5, state.mana(0), 0.0001);

        state.synchronizeWill(5, 0);
        assertEquals(50.0, state.mana(0), 0.0001);
    }

    @Test
    void cooldownUsesServerTicks() {
        PlayerCombatState state = new PlayerCombatState(5, 10);
        state.startCooldown("openworld_rpg:arc_bolt", 60, 10);

        assertTrue(state.isCoolingDown("openworld_rpg:arc_bolt", 69));
        assertEquals(1, state.cooldownRemainingTicks("openworld_rpg:arc_bolt", 69));
        assertFalse(state.isCoolingDown("openworld_rpg:arc_bolt", 70));
    }
}
