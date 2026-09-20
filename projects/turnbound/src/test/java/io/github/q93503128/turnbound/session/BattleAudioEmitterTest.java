package io.github.q93503128.turnbound.session;

import io.github.q93503128.turnbound.combat.BattleEvent;
import io.github.q93503128.turnbound.combat.BattleState;
import io.github.q93503128.turnbound.combat.CombatantSide;
import io.github.q93503128.turnbound.combat.CombatantState;
import io.github.q93503128.turnbound.content.CanonicalData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleAudioEmitterTest {
    @Test
    void coreHeroActionUsesHeroPaletteWhileEnemyKeepsGenericSkillCue() {
        CombatantState kyren = new CombatantState("kyren", CanonicalData.definition("P01"), CombatantSide.ALLY, 0);
        CombatantState enemy = new CombatantState("enemy", CanonicalData.definition("E001"), CombatantSide.ENEMY, 0);
        BattleState state = new BattleState(List.of(kyren, enemy));

        String heroCue = BattleAudioEmitter.cue(state,
                new BattleEvent("ACTION", kyren.instanceId(), enemy.instanceId(), 0, "p01_breaker_strike"));
        String enemyCue = BattleAudioEmitter.cue(state,
                new BattleEvent("ACTION", enemy.instanceId(), kyren.instanceId(), 0, enemy.definition().basicSkillId()));

        assertTrue(heroCue.startsWith("hero_kyren|SKILL|3|"));
        assertTrue(enemyCue.startsWith("skill|SKILL|2|"));
    }
}
