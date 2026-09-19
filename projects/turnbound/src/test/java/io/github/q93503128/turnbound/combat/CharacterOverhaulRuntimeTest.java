package io.github.q93503128.turnbound.combat;

import io.github.q93503128.turnbound.content.CanonicalData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class CharacterOverhaulRuntimeTest {
    private static CombatantState enemy(String id, int hp, int atk, int def, int speed, int seed) {
        return new CombatantState(id,
                PrototypeRoster.trainingEnemy(id.toUpperCase(), id, hp, atk, def, speed),
                CombatantSide.ENEMY, seed);
    }

    @Test
    void bramBuildsGuardAndSpendsItForEmpoweredPressure() {
        CombatantState bram = new CombatantState("bram", CanonicalData.definition("P03", 1, 4, false), CombatantSide.ALLY, 0);
        CombatantState foe = enemy("foe", 9999, 50, 50, 100, 1);
        BattleEngine engine = new BattleEngine(new BattleState(List.of(bram, foe)));

        bram.setGauge(1000);
        engine.nextReady();
        engine.useSkill("bram", "p03_guard_stance", "foe");
        assertEquals(15, bram.counter("guard"));
        assertTrue(bram.barrier() > 0);

        bram.setCounter("guard", 50);
        foe.setGauge(500);
        bram.setGauge(1000);
        engine.nextReady();
        int before = foe.hp();
        engine.useSkill("bram", "p03_shield_pressure", "foe");
        assertEquals(0, bram.counter("guard"));
        assertEquals(320L, foe.gauge());
        assertTrue(before - foe.hp() >= BattleEngine.calculateDamage(bram.attack(), foe.defense(), 1.30));
    }

    @Test
    void sanctuaryIsPreparedThenConsumedOnlyWhenAllyFallsIntoDanger() {
        CombatantState elysia = new CombatantState("elysia", CanonicalData.definition("P04", 1, 4, false), CombatantSide.ALLY, 0);
        CombatantState ally = new CombatantState("ally", PrototypeRoster.kyren(), CombatantSide.ALLY, 1);
        CombatantState foe = enemy("foe", 9999, 500, 0, 100, 2);
        BattleEngine engine = new BattleEngine(new BattleState(List.of(elysia, ally, foe)));

        ally.takeDamage(400);
        elysia.setGauge(1000);
        engine.nextReady();
        engine.useSkill("elysia", "p04_heal", "ally");
        assertNotNull(ally.status("sanctuary", "elysia"));

        int before = ally.hp();
        foe.setGauge(1000);
        engine.nextReady();
        engine.useSkill("foe", "foe_basic", "ally");
        assertNull(ally.status("sanctuary", "elysia"));
        assertTrue(ally.hp() > Math.max(0, before - BattleEngine.calculateDamage(foe.attack(), ally.defense(), 1.0)));
    }

    @Test
    void lynetteUsesSightlineShotsAndCrossShotWithoutIndependentTurn() {
        CombatantState lynette = new CombatantState("lynette", CanonicalData.definition("P05", 1, 4, false), CombatantSide.ALLY, 0);
        CombatantState ally = new CombatantState("ally", PrototypeRoster.kyren(), CombatantSide.ALLY, 1);
        CombatantState foe = enemy("foe", 99999, 1, 0, 70, 2);
        BattleState state = new BattleState(List.of(lynette, ally, foe));
        BattleEngine engine = new BattleEngine(state);

        lynette.setGauge(1000);
        engine.nextReady();
        engine.useSkill("lynette", "p05_hunt_signal", "foe");
        assertEquals("foe", lynette.ref("sightline"));
        assertEquals(1, lynette.counter("shot"));

        ally.setGauge(1000);
        engine.nextReady();
        engine.useSkill("ally", "p01_chase_slash", "foe");
        assertEquals(0, lynette.counter("shot"));
        assertEquals(1, lynette.counter("p05_followups"));
        assertTrue(state.events().stream().anyMatch(e -> "P05_CROSS_SHOT".equals(e.detail())));
    }

    @Test
    void morwenRecordsDangerAndSpendsAtMostThreeRecords() {
        CombatantState morwen = new CombatantState("morwen", CanonicalData.definition("P06", 1, 5, false), CombatantSide.ALLY, 0);
        CombatantState ally = new CombatantState("ally", PrototypeRoster.kyren(), CombatantSide.ALLY, 1);
        CombatantState foe = enemy("foe", 99999, 1000, 0, 80, 2);
        BattleEngine engine = new BattleEngine(new BattleState(List.of(morwen, ally, foe)));

        ally.takeDamage((int)Math.floor(ally.maxHp() * 0.72));
        foe.setGauge(1000);
        engine.nextReady();
        engine.useSkill("foe", "foe_basic", "ally");
        assertEquals(1, morwen.counter("records"));

        morwen.setCounter("records", 5);
        morwen.setGauge(1000);
        engine.nextReady();
        engine.useSkill("morwen", "p06_condolence", "foe");
        assertEquals(2, morwen.counter("records"));
    }

    @Test
    void marionPartnerGuardRedirectsOnceAndBuildsBond() {
        CombatantState marion = new CombatantState("marion", CanonicalData.definition("P07", 1, 4, false), CombatantSide.ALLY, 0);
        CombatantState ally = new CombatantState("ally", PrototypeRoster.kyren(), CombatantSide.ALLY, 1);
        CombatantState foe = enemy("foe", 99999, 180, 0, 90, 2);
        BattleState state = new BattleState(List.of(marion, ally, foe));
        BattleEngine engine = new BattleEngine(state);
        CombatantState partner = state.living(CombatantSide.ALLY).stream().filter(x -> x.definition().summon()).findFirst().orElseThrow();

        marion.setGauge(1000);
        engine.nextReady();
        engine.useSkill("marion", "p07_summon_toto", "ally");
        assertNotNull(ally.status("partner_guard", "marion"));
        int partnerBefore = partner.hp();

        foe.setGauge(1000);
        engine.nextReady();
        engine.useSkill("foe", "foe_basic", "ally");
        assertNull(ally.status("partner_guard", "marion"));
        assertTrue(partner.hp() < partnerBefore);
        assertEquals(20, marion.counter("bond"));
    }

    @Test
    void razeFuryControlsOverheatAndAwakeningSurvival() {
        CombatantState raze = new CombatantState("raze", CanonicalData.definition("P08", 1, 3, true), CombatantSide.ALLY, 0);
        CombatantState foe = enemy("foe", 99999, 1, 0, 80, 1);
        BattleEngine engine = new BattleEngine(new BattleState(List.of(raze, foe)));

        raze.setGauge(1000);
        engine.nextReady();
        assertThrows(IllegalStateException.class, () -> engine.useSkill("raze", "p08_battle_mania"));

        raze.setCounter("fury", 60);
        engine.useSkill("raze", "p08_battle_mania");
        assertEquals(0, raze.counter("fury"));
        assertTrue(raze.speed() > raze.definition().stats().speed());

        raze.takeDamage(999999);
        assertFalse(raze.downed());
        assertEquals(1, raze.hp());
        assertEquals(100, raze.counter("fury"));
        assertEquals(250L, raze.gauge());
        assertTrue(raze.healingReceivedModifier() < 0);
    }
}
