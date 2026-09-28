package dev.moonseungjun.openworldrpg.multiplayer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MultiplayerCombatRulesTest {
    @Test
    void formalPartySupportsTwoThroughFourPlayers() {
        assertFalse(MultiplayerCombatRules.validFormalPartySize(1));
        assertTrue(MultiplayerCombatRules.validFormalPartySize(2));
        assertTrue(MultiplayerCombatRules.validFormalPartySize(3));
        assertTrue(MultiplayerCombatRules.validFormalPartySize(4));
        assertFalse(MultiplayerCombatRules.validFormalPartySize(5));
        assertEquals(4, MultiplayerCombatRules.MAX_FORMAL_PARTY_SIZE);
    }

    @Test
    void bossHpScalingMatchesCanonForAndBeyondPartyCap() {
        assertEquals(1.00, MultiplayerCombatRules.bossHpScale(1), 0.0001);
        assertEquals(1.65, MultiplayerCombatRules.bossHpScale(2), 0.0001);
        assertEquals(2.30, MultiplayerCombatRules.bossHpScale(3), 0.0001);
        assertEquals(2.95, MultiplayerCombatRules.bossHpScale(4), 0.0001);
        assertEquals(3.40, MultiplayerCombatRules.bossHpScale(5), 0.0001);
        assertEquals(3.85, MultiplayerCombatRules.bossHpScale(6), 0.0001);
    }

    @Test
    void bossPoiseScalingMatchesCanonForAndBeyondPartyCap() {
        assertEquals(1.00, MultiplayerCombatRules.bossPoiseScale(1), 0.0001);
        assertEquals(1.40, MultiplayerCombatRules.bossPoiseScale(2), 0.0001);
        assertEquals(1.80, MultiplayerCombatRules.bossPoiseScale(3), 0.0001);
        assertEquals(2.20, MultiplayerCombatRules.bossPoiseScale(4), 0.0001);
        assertEquals(2.50, MultiplayerCombatRules.bossPoiseScale(5), 0.0001);
        assertEquals(2.80, MultiplayerCombatRules.bossPoiseScale(6), 0.0001);
    }

    @Test
    void ordinaryEnemiesNeverGainGenericCoopHpScaling() {
        assertEquals(1.0, MultiplayerCombatRules.ordinaryEnemyHpScale(1));
        assertEquals(1.0, MultiplayerCombatRules.ordinaryEnemyHpScale(4));
        assertEquals(1.0, MultiplayerCombatRules.ordinaryEnemyHpScale(20));
    }

    @Test
    void invalidEncounterCountsFailClosedAndLaunchPvpIsDisabled() {
        assertThrows(
                IllegalArgumentException.class,
                () -> MultiplayerCombatRules.bossHpScale(0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> MultiplayerCombatRules.bossPoiseScale(0)
        );
        assertFalse(MultiplayerCombatRules.DIRECT_PLAYER_PVP_ENABLED);
    }
}
