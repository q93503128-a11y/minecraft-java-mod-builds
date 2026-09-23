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
    void earthloongFirstClearPreservesSeparateBossAndDungeonLayers() {
        var boss = R01RewardRules.EARTHLOONG_FIRST_BOSS_LAYER;
        var completion = R01RewardRules.EARTHLOONG_FIRST_DUNGEON_COMPLETION;

        assertEquals(0.40, boss.combatRequirementFraction(), 0.0001);
        assertEquals(0.20, boss.classRequirementFraction(), 0.0001);
        assertEquals(0L, boss.gold());

        assertEquals(1.00, completion.combatRequirementFraction(), 0.0001);
        assertEquals(0.64, completion.classRequirementFraction(), 0.0001);
        assertEquals(180L, completion.gold());
    }
}
