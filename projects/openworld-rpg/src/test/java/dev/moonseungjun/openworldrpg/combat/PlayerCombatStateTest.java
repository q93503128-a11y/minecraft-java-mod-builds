package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.state.EquipmentResourceModifiers;
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
    void canonicalStaminaFormulaMatchesDesignAnchors() {
        assertEquals(100, PlayerCombatState.maxStaminaForEndurance(5));
        assertEquals(130, PlayerCombatState.maxStaminaForEndurance(30));
        assertEquals(154, PlayerCombatState.maxStaminaForEndurance(60));
        assertEquals(162, PlayerCombatState.maxStaminaForEndurance(80));

        assertEquals(24.0, PlayerCombatState.baseStaminaRegenPerSecondForEndurance(5), 0.0001);
        assertEquals(27.0, PlayerCombatState.baseStaminaRegenPerSecondForEndurance(30), 0.0001);
        assertEquals(30.6, PlayerCombatState.baseStaminaRegenPerSecondForEndurance(60), 0.0001);
        assertEquals(31.6, PlayerCombatState.baseStaminaRegenPerSecondForEndurance(80), 0.0001);
    }

    @Test
    void sprintDrainsFiveStaminaPerSecondAndWaitsSevenTicksBeforeRegen() {
        PlayerCombatState state = new PlayerCombatState(5, 0);
        for (int tick = 0; tick < 20; tick++) {
            assertTrue(state.updateSprinting(true, tick));
        }
        assertEquals(95.0, state.stamina(19), 0.0001);

        assertTrue(state.updateSprinting(false, 20));
        assertEquals(95.0, state.stamina(27), 0.0001);
        assertEquals(96.2, state.stamina(28), 0.0001);
    }

    @Test
    void sprintExhaustionUsesTheSameSevenTickStopDelayWithoutAnExtraTick() {
        PlayerCombatState state = new PlayerCombatState(5, 0);
        assertTrue(state.spendStamina(99.9, 0, 0));

        assertFalse(state.updateSprinting(true, 0));
        assertTrue(state.updateSprinting(false, 1));
        assertEquals(0.1, state.stamina(7), 0.0001);
        assertEquals(1.3, state.stamina(8), 0.0001);
    }

    @Test
    void enduranceSynchronizationPreservesStaminaPercentage() {
        PlayerCombatState state = new PlayerCombatState(5, 0);
        assertTrue(state.spendStamina(50.0, 12, 0));

        state.synchronizeEndurance(30, 0);
        assertEquals(130, state.maxStamina());
        assertEquals(65.0, state.stamina(0), 0.0001);

        state.synchronizeEndurance(5, 0);
        assertEquals(50.0, state.stamina(0), 0.0001);
    }

    @Test
    void maxResourceAffixesPreserveCurrentPercentInsteadOfRefilling() {
        PlayerCombatState state = new PlayerCombatState(5, 0);
        assertTrue(state.spendMana(50.0, 0));
        assertTrue(state.spendStamina(50.0, 12, 0));

        state.synchronizeResourceModifiers(
                new EquipmentResourceModifiers(
                        0.07,
                        0.09,
                        0.09,
                        0.0,
                        0.0,
                        0.0,
                        0.0
                ),
                0
        );

        assertEquals(109, state.maxMana());
        assertEquals(109, state.maxStamina());
        assertEquals(54.5, state.mana(0), 0.0001);
        assertEquals(54.5, state.stamina(0), 0.0001);

        state.synchronizeResourceModifiers(
                EquipmentResourceModifiers.none(),
                0
        );
        assertEquals(100, state.maxMana());
        assertEquals(100, state.maxStamina());
        assertEquals(50.0, state.mana(0), 0.0001);
        assertEquals(50.0, state.stamina(0), 0.0001);
    }

    @Test
    void resourceRecoveryAffixesMultiplyCanonicalBaseRegeneration() {
        PlayerCombatState state = new PlayerCombatState(5, 0);
        state.synchronizeResourceModifiers(
                new EquipmentResourceModifiers(
                        0.0,
                        0.0,
                        0.0,
                        0.12,
                        0.12,
                        0.0,
                        0.0
                ),
                0
        );

        assertTrue(state.spendMana(50.0, 0));
        assertTrue(state.spendStamina(50.0, 0, 0));

        assertEquals(54.48, state.mana(40), 0.0001);
        assertEquals(100.0, state.stamina(40), 0.0001);
    }

    @Test
    void dodgeSprintCostReductionAffectsOnlyThoseMovementCosts() {
        PlayerCombatState state = new PlayerCombatState(5, 0);
        state.synchronizeResourceModifiers(
                new EquipmentResourceModifiers(
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        0.25
                ),
                0
        );

        assertEquals(
                22.5,
                state.effectiveDodgeSprintStaminaCost(30.0),
                0.0001
        );
        for (int tick = 0; tick < 20; tick++) {
            assertTrue(state.updateSprinting(true, tick));
        }
        assertEquals(96.25, state.stamina(19), 0.0001);
    }

    @Test
    void manaCostReductionAppliesToChecksAndSpendWithCanonicalTwentyPercentCap() {
        PlayerCombatState state = new PlayerCombatState(5, 0);
        state.synchronizeResourceModifiers(
                new EquipmentResourceModifiers(
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        0.20,
                        0.0
                ),
                0
        );

        assertEquals(40.0, state.effectiveManaCost(50.0), 0.0001);
        assertTrue(state.spendMana(50.0, 0));
        assertEquals(60.0, state.mana(0), 0.0001);
        assertTrue(state.canSpendMana(75.0, 0));
        assertTrue(state.spendMana(75.0, 0));
        assertEquals(0.0, state.mana(0), 0.0001);
        assertFalse(state.canSpendMana(1.0, 0));
    }


    @Test
    void hostileHpActivityBlocksNaturalRecoveryForExactlyEightSeconds() {
        PlayerCombatState state = new PlayerCombatState(5, 0);
        state.markHostileHpActivity(40);

        assertFalse(state.canNaturalHpRecover(199));
        assertTrue(state.canNaturalHpRecover(200));
    }

    @Test
    void laterCombatActivityAlsoPreventsNaturalHpRecoveryUntilCombatHasBeenQuiet() {
        PlayerCombatState state = new PlayerCombatState(5, 0);
        state.markHostileHpActivity(0);
        state.markCombatActivity(100);

        assertFalse(state.canNaturalHpRecover(259));
        assertTrue(state.canNaturalHpRecover(260));
    }

    @Test
    void persistentSnapshotPreservesSpentResourcesAndCooldownAcrossReconnect() {
        PlayerCombatState original = new PlayerCombatState(5, 0);
        assertTrue(original.spendMana(50.0, 10));
        assertTrue(original.spendStamina(40.0, 12, 10));
        original.startCooldown("openworld_rpg:arc_bolt", 60, 10);

        var snapshot = original.persistentSnapshot(10);
        PlayerCombatState restored = new PlayerCombatState(5, 20);
        restored.restorePersistent(snapshot, 20);

        assertEquals(50.0, restored.mana(20), 0.0001);
        assertEquals(60.0, restored.stamina(20), 0.0001);
        assertTrue(restored.isCoolingDown("openworld_rpg:arc_bolt", 20));
        assertEquals(50L, restored.cooldownRemainingTicks("openworld_rpg:arc_bolt", 20));
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
