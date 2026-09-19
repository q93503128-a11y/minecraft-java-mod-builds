package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrehmalMetaPresentationRulesTest {
    @Test
    void hidesLegacyWorldSpecificChallengesButKeepsGenericCombatChallenges() {
        assertTrue(DrehmalMetaPresentationRules.challengeVisible("CH03_UNDER_12_ALLY_ACTIONS"));
        assertTrue(DrehmalMetaPresentationRules.challengeVisible("CH14_ELITE_NO_REVIVE"));
        assertFalse(DrehmalMetaPresentationRules.challengeVisible("CH12_KILL_E003_BEFORE_EXPLOSION"));
        assertFalse(DrehmalMetaPresentationRules.challengeVisible("CH15_HARD_B01"));
        assertFalse(DrehmalMetaPresentationRules.challengeVisible("CH20_RIFT_F30"));
    }

    @Test
    void firstDrabyelShopOnlyPublishesTheAuthoredBasicTier() {
        assertTrue(DrehmalMetaPresentationRules.firstHubShopVisible("T1"));
        assertFalse(DrehmalMetaPresentationRules.firstHubShopVisible("T2"));
        assertFalse(DrehmalMetaPresentationRules.firstHubShopVisible("T3"));
    }

    @Test
    void externalCodexDoesNotPreExposeUndiscoveredCombatants() {
        assertTrue(DrehmalMetaPresentationRules.combatantCodexVisible(true));
        assertFalse(DrehmalMetaPresentationRules.combatantCodexVisible(false));
    }
}
