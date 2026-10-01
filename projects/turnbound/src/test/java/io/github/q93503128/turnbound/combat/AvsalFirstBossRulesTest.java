package io.github.q93503128.turnbound.combat;

import io.github.q93503128.turnbound.content.CanonicalData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvsalFirstBossRulesTest {
    @Test
    void barrierDirectHitQueuesARealCounter() {
        CombatantState ally = new CombatantState("ally", PrototypeRoster.kyren(), CombatantSide.ALLY, 0);
        CombatantState boss = new CombatantState("boss", CanonicalData.definition("AV_B01"), CombatantSide.ENEMY, 1);
        BattleEngine engine = new BattleEngine(new BattleState(List.of(ally, boss)));

        boss.addBarrier(900);
        int allyBefore = ally.hp();
        ally.setGauge(1000);
        engine.nextReady();
        engine.useSkill("ally", "p01_chase_slash", "boss");

        assertTrue(ally.hp() < allyBefore, "Karnon barrier must counter a direct hit in the authoritative engine");
    }

    @Test
    void halfHealthTransitionAddsBarrierAndPermanentTempoPressure() {
        CombatantState ally = new CombatantState("ally", PrototypeRoster.kyren(), CombatantSide.ALLY, 0);
        CombatantState boss = new CombatantState("boss", CanonicalData.definition("AV_B01"), CombatantSide.ENEMY, 1);
        BattleEngine engine = new BattleEngine(new BattleState(List.of(ally, boss)));

        boss.takeDamage((int)Math.ceil(boss.maxHp() * 0.47));
        ally.setGauge(1000);
        engine.nextReady();
        engine.useSkill("ally", "p01_chase_slash", "boss");

        assertTrue(boss.flag("av_b01_phase2"));
        assertTrue(boss.barrier() > 0);
        assertNotNull(boss.status("speed_multiplier", boss.instanceId()));
    }
}
