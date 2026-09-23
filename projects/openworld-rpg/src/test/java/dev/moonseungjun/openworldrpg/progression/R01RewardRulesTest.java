package dev.moonseungjun.openworldrpg.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.moonseungjun.openworldrpg.progression.r01.R01RewardRules;
import org.junit.jupiter.api.Test;

class R01RewardRulesTest {
    @Test
    void dustRewardMatchesClosedR01QuestValues() {
        var rule = R01RewardRules.DUST_ON_QUARRY_ROAD;

        assertEquals(0.70, rule.combatRequirementFraction(), 0.0001);
        assertEquals(0.50, rule.classRequirementFraction(), 0.0001);
        assertEquals(90L, rule.gold());
    }

    @Test
    void earthloongProgressionRewardCombinesBossAndDungeonLayers() {
        var rule = R01RewardRules.EARTHLOONG_FIRST_CLEAR_PROGRESSION;

        assertEquals(1.40, rule.combatRequirementFraction(), 0.0001);
        assertEquals(0.84, rule.classRequirementFraction(), 0.0001);
        assertEquals(180L, rule.gold());
    }
}
