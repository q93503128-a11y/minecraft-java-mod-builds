package io.github.q93503128.turnbound.combat;

import io.github.q93503128.turnbound.content.CanonicalData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class V04BattleRuntimeTest {
    @Test
    void gaugeAndCooldownCommitBeforePrimaryEffects() {
        CombatantState hunter = new CombatantState("hunter", CanonicalData.definition("E012", 1, 0, false), CombatantSide.ENEMY, 0);
        CombatantState target = new CombatantState("target", PrototypeRoster.trainingEnemy("TARGET", "Target", 9999, 50, 50, 100), CombatantSide.ALLY, 1);
        hunter.setGauge(1000);
        target.setGauge(500);
        BattleEngine engine = new BattleEngine(new BattleState(List.of(target, hunter)));

        assertEquals("hunter", engine.nextReady().instanceId());
        int before = target.hp();
        int canonicalWithoutGaugeBonus = BattleEngine.calculateDamage(hunter.attack(), target.defense(), 1.00);
        engine.useSkill("hunter", "e012_basic", "target");

        assertEquals(0, hunter.gauge());
        assertEquals(canonicalWithoutGaugeBonus, before - target.hp());
    }

    @Test
    void morwenReturnsAfterExactlyTwoOtherRegularActions() {
        CombatantState morwen = new CombatantState("morwen", CanonicalData.definition("P06", 1, 5, false), CombatantSide.ALLY, 0);
        CombatantState survivor = new CombatantState("survivor", PrototypeRoster.kyren(), CombatantSide.ALLY, 1);
        CombatantState killer = new CombatantState("killer", PrototypeRoster.trainingEnemy("KILLER", "Killer", 99999, 5000, 0, 100), CombatantSide.ENEMY, 2);
        BattleEngine engine = new BattleEngine(new BattleState(List.of(morwen, survivor, killer)));

        killer.setGauge(1000);
        assertEquals("killer", engine.nextReady().instanceId());
        engine.useSkill("killer", "killer_basic", "morwen");
        assertTrue(morwen.downed());
        assertEquals(2, morwen.counter("p06_return_wait"));

        survivor.setGauge(1000);
        assertEquals("survivor", engine.nextReady().instanceId());
        engine.useSkill("survivor", "p01_chase_slash", "killer");
        assertTrue(morwen.downed());
        assertEquals(1, morwen.counter("p06_return_wait"));

        survivor.setGauge(1000);
        assertEquals("survivor", engine.nextReady().instanceId());
        engine.useSkill("survivor", "p01_chase_slash", "killer");
        assertFalse(morwen.downed());
        assertEquals((int)Math.floor(morwen.maxHp() * 0.35), morwen.hp());
        assertEquals(0, morwen.gauge());
    }

    @Test
    void marionSummonUsesSeparateFifthAllySlot() {
        CombatantState marion = new CombatantState("marion", CanonicalData.definition("P07", 1, 4, false), CombatantSide.ALLY, 0);
        CombatantState a2 = new CombatantState("a2", PrototypeRoster.kyren(), CombatantSide.ALLY, 1);
        CombatantState a3 = new CombatantState("a3", PrototypeRoster.bram(), CombatantSide.ALLY, 2);
        CombatantState a4 = new CombatantState("a4", PrototypeRoster.elysia(), CombatantSide.ALLY, 3);
        CombatantState enemy = new CombatantState("enemy", PrototypeRoster.trainingEnemy("ENEMY", "Enemy", 99999, 1, 0, 30), CombatantSide.ENEMY, 4);
        BattleState state = new BattleState(List.of(marion, a2, a3, a4, enemy));
        BattleEngine engine = new BattleEngine(state);

        marion.setGauge(1000);
        assertEquals("marion", engine.nextReady().instanceId());
        engine.useSkill("marion", "p07_summon_toto");

        assertEquals(5, state.living(CombatantSide.ALLY).size());
        CombatantState summon = state.living(CombatantSide.ALLY).stream().filter(unit -> unit.definition().summon()).findFirst().orElse(null);
        assertNotNull(summon);
        assertEquals("marion", summon.ref("ownerId"));
        assertEquals(85, summon.speed());
    }

    @Test
    void allLaterBossesEnterTheirFirstCanonicalPhase() {
        assertFirstPhase("B02", 0.40, "b02_phase2", "E008");
        assertFirstPhase("B03", 0.30, "b03_phase2", "E009");
        assertFirstPhase("B04", 0.35, "b04_phase2", "E014");
        assertFirstPhase("B05", 0.35, "b05_phase2", "E009");
    }

    private static void assertFirstPhase(String bossId, double preDamageRatio, String phaseFlag, String expectedAdd) {
        CombatantState striker = new CombatantState("striker", PrototypeRoster.trainingEnemy("STRIKER", "Striker", 99999, 1, 0, 100), CombatantSide.ALLY, 0);
        CombatantState boss = new CombatantState("boss", CanonicalData.definition(bossId, 1, 0, false), CombatantSide.ENEMY, 1);
        boss.takeDamage((int)Math.floor(boss.maxHp() * preDamageRatio));
        BattleState state = new BattleState(List.of(striker, boss));
        BattleEngine engine = new BattleEngine(state);
        striker.setGauge(1000);
        assertEquals("striker", engine.nextReady().instanceId());
        engine.useSkill("striker", "striker_basic", "boss");
        assertTrue(boss.flag(phaseFlag), bossId);
        assertTrue(state.living(CombatantSide.ENEMY).stream().anyMatch(unit -> unit.definition().id().equals(expectedAdd)), bossId);
    }
}
