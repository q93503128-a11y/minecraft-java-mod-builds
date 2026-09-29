package dev.moonseungjun.openworldrpg.death;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ProjectDeathPenaltyRulesTest {
    @Test
    void currentLevelXpPenaltyMatchesFourPercentRuleWithoutLevelLoss() {
        assertEquals(
                6L,
                ProjectDeathPenaltyRules.currentLevelXpLoss(1, 100)
        );
        assertEquals(
                30L,
                ProjectDeathPenaltyRules.currentLevelXpLoss(8, 500)
        );
        assertEquals(
                10L,
                ProjectDeathPenaltyRules.currentLevelXpLoss(8, 10)
        );
        assertEquals(
                0L,
                ProjectDeathPenaltyRules.currentLevelXpLoss(8, 0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ProjectDeathPenaltyRules.currentLevelXpLoss(80, 1)
        );
    }

    @Test
    void goldFallbackMatchesCanonAnchorsAndCap() {
        assertEquals(40L, ProjectDeathPenaltyRules.goldFallbackCost(1));
        assertEquals(100L, ProjectDeathPenaltyRules.goldFallbackCost(10));
        assertEquals(200L, ProjectDeathPenaltyRules.goldFallbackCost(20));
        assertEquals(460L, ProjectDeathPenaltyRules.goldFallbackCost(40));
        assertEquals(820L, ProjectDeathPenaltyRules.goldFallbackCost(60));
        assertEquals(1200L, ProjectDeathPenaltyRules.goldFallbackCost(80));
    }
}
