package io.github.q93503128.turnbound.combat;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SharedBattleRulesTest {
    @Test void enemyHpMultiplierTracksPlayerCountExactly() {
        assertEquals(1, SharedBattleRules.enemyHpMultiplier(1));
        assertEquals(2, SharedBattleRules.enemyHpMultiplier(2));
        assertEquals(3, SharedBattleRules.enemyHpMultiplier(3));
        assertEquals(4, SharedBattleRules.enemyHpMultiplier(4));
    }

    @Test void enemyScalingChangesOnlyHp() {
        SkillDefinition basic = new SkillDefinition("basic","basic",TargetRule.ENEMY_SINGLE,0,List.of(SkillEffect.damage(1.0)),"");
        CombatantDefinition base = new CombatantDefinition("E","Enemy",new BattleStats(100,20,30,40),
                "basic",List.of(basic),0,List.of(),Map.of());
        CombatantDefinition scaled = SharedBattleRules.scaleEnemyHp(base,3);
        assertEquals(300,scaled.stats().maxHp());
        assertEquals(20,scaled.stats().attack());
        assertEquals(30,scaled.stats().defense());
        assertEquals(40,scaled.stats().speed());
    }

    @Test void fourPlayersAllowSixteenHeroesAndFourPersonalSummons() {
        BattleState.Capacity c=SharedBattleRules.capacity(4);
        assertEquals(16,c.regularAllies());
        assertEquals(4,c.allySummons());
        assertEquals(5,c.enemies());
        assertEquals(25,c.totalCombatants());
    }
}
