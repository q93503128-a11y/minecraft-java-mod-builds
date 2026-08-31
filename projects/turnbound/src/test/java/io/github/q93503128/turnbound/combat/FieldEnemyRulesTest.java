package io.github.q93503128.turnbound.combat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldEnemyRulesTest {
    @Test
    void e001TenacityTriggersOnceWhenHpCrossesThirtyPercent() {
        CombatantState ally = new CombatantState("ally", PrototypeRoster.kyren(), CombatantSide.ALLY, 0);
        CombatantState walker = new CombatantState("walker", PrototypeRoster.corruptedWalker(), CombatantSide.ENEMY, 1);
        BattleState state = new BattleState(List.of(ally, walker));
        BattleEngine engine = new BattleEngine(state);
        walker.takeDamage(500);
        ally.setGauge(1000);
        engine.nextReady();
        engine.useSkill("ally", "p01_chase_slash", "walker");
        assertTrue(walker.hp() > 0 && walker.hp() * 100 <= walker.maxHp() * 30);
        assertEquals(72, walker.barrier());
        assertTrue(walker.flag("e001_tenacity"));
    }

    @Test
    void e005ReformAppliesFifteenPercentDefenseForTwoOwnerActions() {
        CombatantState ally = new CombatantState("ally", PrototypeRoster.kyren(), CombatantSide.ALLY, 0);
        CombatantState walker = new CombatantState("walker", PrototypeRoster.corruptedWalker(), CombatantSide.ENEMY, 1);
        CombatantState medic = new CombatantState("medic", PrototypeRoster.fieldMedic(), CombatantSide.ENEMY, 2);
        BattleState state = new BattleState(List.of(ally, walker, medic));
        BattleEngine engine = new BattleEngine(state);
        medic.setGauge(1000);
        engine.nextReady();
        engine.useSkill("medic", "e005_reform");
        assertEquals(78, walker.defense());
        assertEquals(69, medic.defense());
        assertEquals(2, walker.status("defense_multiplier").remainingOwnerTurns());
        assertEquals(2, medic.status("defense_multiplier").remainingOwnerTurns());
    }
}
