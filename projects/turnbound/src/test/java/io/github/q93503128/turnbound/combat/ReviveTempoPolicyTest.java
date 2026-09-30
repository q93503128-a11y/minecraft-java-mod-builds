package io.github.q93503128.turnbound.combat;

import io.github.q93503128.turnbound.content.CanonicalData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReviveTempoPolicyTest {
    @Test
    void playableCharactersUseAuthoredReturnTempo() {
        CombatantState bram = new CombatantState("bram", CanonicalData.definition("P03", 1, 4, false), CombatantSide.ALLY, 0);
        CombatantState lumea = new CombatantState("lumea", CanonicalData.definition("P02", 1, 5, false), CombatantSide.ALLY, 1);
        assertEquals(100, ReviveTempoPolicy.manualStartGauge(bram));
        assertEquals(340, ReviveTempoPolicy.manualStartGauge(lumea));
    }

    @Test
    void immediateSelfReviveStartsReady() {
        SkillDefinition basic = new SkillDefinition(
                "basic", "기본", TargetRule.ENEMY_SINGLE, 0,
                List.of(new SkillEffect(EffectType.DAMAGE, 1.0, 0, 0, "")));
        CombatantDefinition definition = new CombatantDefinition(
                "TEST_REVIVER", "복귀자", new BattleStats(100, 20, 10, 100),
                "basic", List.of(basic), 4,
                List.of("AUTO_REVIVE_ONCE", "REVIVE_IMMEDIATE_TURN"),
                Map.of("autoReviveHp", 0.35));
        CombatantState target = new CombatantState("reviver", definition, CombatantSide.ALLY, 0);
        assertEquals(1000, ReviveTempoPolicy.selfReviveStartGauge(target));
    }
}
