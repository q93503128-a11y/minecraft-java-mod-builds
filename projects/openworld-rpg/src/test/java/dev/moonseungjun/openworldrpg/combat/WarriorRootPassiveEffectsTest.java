package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.runtime.WarriorRootPassiveEffects;
import org.junit.jupiter.api.Test;

class WarriorRootPassiveEffectsTest {
    @Test
    void canonicalRootPassiveRanksResolveExactMagnitudes() {
        assertEquals(
                0.09,
                WarriorRootPassiveEffects
                        .maxHealthPercentBonusForRank(3),
                0.0001
        );
        assertEquals(
                12,
                WarriorRootPassiveEffects
                        .maxStaminaFlatBonusForRank(3)
        );
        assertEquals(
                0.03,
                WarriorRootPassiveEffects
                        .staminaRecoveryBonusForRank(3),
                0.0001
        );
        assertEquals(
                0.06,
                WarriorRootPassiveEffects
                        .weaponRhythmAttackSpeedBonusForRank(3),
                0.0001
        );
        assertEquals(
                1.12,
                WarriorRootPassiveEffects
                        .poiseOutputMultiplierForRank(3),
                0.0001
        );
        assertEquals(
                40L,
                WarriorRootPassiveEffects
                        .momentumExpiryBonusTicksForRank(2)
        );
        assertEquals(
                1.16,
                WarriorRootPassiveEffects
                        .counterforceOutputMultiplierForRank(2),
                0.0001
        );
        assertEquals(
                80L,
                WarriorRootPassiveEffects.BATTLE_TEMPER_ICD_TICKS
        );
        assertEquals(
                10.0,
                WarriorRootPassiveEffects
                        .BATTLE_TEMPER_STAMINA_RESTORE,
                0.0001
        );
        assertEquals(
                8.0,
                WarriorRootPassiveEffects
                        .BATTLE_TEMPER_MANA_RESTORE,
                0.0001
        );
        assertTrue(
                WarriorRootPassiveEffects
                        .counterforceOutputMultiplierForRank(2)
                        > 1.0
        );
    }
}
