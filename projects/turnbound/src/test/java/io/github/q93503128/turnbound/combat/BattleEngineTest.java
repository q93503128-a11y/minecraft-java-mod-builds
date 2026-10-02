package io.github.q93503128.turnbound.combat;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

final class BattleEngineTest {
    private static CombatantDefinition unit(String id,int hp,int atk,int def,int spd){return PrototypeRoster.trainingEnemy(id,id,hp,atk,def,spd);}
    @Test void overflowGaugeCreatesNaturalConsecutiveTurns(){var fast=new CombatantState("fast",unit("FAST",9999,10,10,220),CombatantSide.ALLY,0);var slow=new CombatantState("slow",unit("SLOW",9999,10,10,100),CombatantSide.ENEMY,1);var e=new BattleEngine(new BattleState(List.of(fast,slow)));assertEquals("fast",e.nextReady().instanceId());e.useSkill("fast","fast_basic","slow");assertEquals(0L,fast.gauge(),"fixed-point scheduling keeps only the true sub-Gauge overflow");assertEquals("fast",e.nextReady().instanceId());e.useSkill("fast","fast_basic","slow");assertEquals("slow",e.nextReady().instanceId());}
    @Test void cooldownTicksOnlyOnFutureOwnActions(){var k=new CombatantState("k",PrototypeRoster.kyren(),CombatantSide.ALLY,0);var d=new CombatantState("d",unit("D",9999,1,1,1),CombatantSide.ENEMY,1);k.setGauge(1000);var e=new BattleEngine(new BattleState(List.of(k,d)));e.nextReady();e.useSkill("k","p01_breaker_strike","d");assertEquals(2,k.cooldown("p01_breaker_strike"));k.setGauge(1000);e.nextReady();e.useSkill("k","p01_chase_slash","d");assertEquals(1,k.cooldown("p01_breaker_strike"));k.setGauge(1000);e.nextReady();e.useSkill("k","p01_chase_slash","d");assertEquals(0,k.cooldown("p01_breaker_strike"));}
    @Test void basicActionCanHeal(){var h=new CombatantState("h",PrototypeRoster.elysia(),CombatantSide.ALLY,0);var a=new CombatantState("a",PrototypeRoster.kyren(),CombatantSide.ALLY,1);var en=new CombatantState("e",unit("E",9999,1,1,1),CombatantSide.ENEMY,2);a.takeDamage(500);h.setGauge(1000);var e=new BattleEngine(new BattleState(List.of(h,a,en)));int before=a.hp();e.nextReady();e.useSkill("h","p04_heal","a");assertTrue(a.hp()>before);assertEquals(0,h.cooldown("p04_heal"));}
    @Test void reviveReturnsAtThirtyPercent(){var h=new CombatantState("h",PrototypeRoster.elysia(),CombatantSide.ALLY,0);var a=new CombatantState("a",PrototypeRoster.kyren(),CombatantSide.ALLY,1);var en=new CombatantState("e",unit("E",9999,1,1,1),CombatantSide.ENEMY,2);a.takeDamage(99999);h.setGauge(1000);var e=new BattleEngine(new BattleState(List.of(h,a,en)));e.nextReady();e.useSkill("h","p04_returned_breath","a");assertFalse(a.downed());assertEquals((int)Math.floor(a.maxHp()*0.30),a.hp());assertEquals(150,a.gauge());}

    @Test void authoredGaugeSkillsApplyAfterTurnCommitAndPreserveTheirExactGaugeRules(){
        var kyren=new CombatantState("k",PrototypeRoster.kyren(),CombatantSide.ALLY,0);var dummy=new CombatantState("d",unit("D",99999,1,999,1),CombatantSide.ENEMY,1);kyren.setGauge(1000);var duel=new BattleEngine(new BattleState(List.of(kyren,dummy)));duel.nextReady();duel.useSkill("k","p01_duel_lock","d");assertEquals(100L,kyren.gauge(),"P01 tempo refund occurs after the 1000 turn commit");

        var lumea=new CombatantState("l",PrototypeRoster.lumea(),CombatantSide.ALLY,0);var ally=new CombatantState("a",PrototypeRoster.kyren(),CombatantSide.ALLY,1);var enemy=new CombatantState("e",unit("E",99999,1,999,1),CombatantSide.ENEMY,2);lumea.setGauge(1000);ally.setGauge(250);var accelerate=new BattleEngine(new BattleState(List.of(lumea,ally,enemy)));accelerate.nextReady();accelerate.useSkill("l","p02_accelerate","a");assertEquals(490L,ally.gauge());

        lumea=new CombatantState("l",PrototypeRoster.lumea(),CombatantSide.ALLY,0);ally=new CombatantState("a",PrototypeRoster.kyren(),CombatantSide.ALLY,1);enemy=new CombatantState("e",unit("E",99999,1,999,1),CombatantSide.ENEMY,2);lumea.setGauge(1000);ally.setGauge(250);var leap=new BattleEngine(new BattleState(List.of(lumea,ally,enemy)));leap.nextReady();leap.useSkill("l","p02_time_leap","a");assertEquals(700L,ally.gauge(),"Time Leap preserves the stronger slower-ally bonus without forcing Ready");

        lumea=new CombatantState("l",PrototypeRoster.lumea(),CombatantSide.ALLY,0);enemy=new CombatantState("e",unit("E",99999,1,999,1),CombatantSide.ENEMY,1);lumea.setGauge(1000);enemy.setGauge(400);var delay=new BattleEngine(new BattleState(List.of(lumea,enemy)));delay.nextReady();delay.useSkill("l","p02_delay_field","e");assertEquals(260L,enemy.gauge());

        var bram=new CombatantState("b",PrototypeRoster.bram(),CombatantSide.ALLY,0);enemy=new CombatantState("e",unit("E",99999,1,999,1),CombatantSide.ENEMY,1);bram.setGauge(1000);enemy.setGauge(400);var pressure=new BattleEngine(new BattleState(List.of(bram,enemy)));pressure.nextReady();pressure.useSkill("b","p03_shield_pressure","e");assertEquals(320L,enemy.gauge(),"Base vibration shield applies -80 until Guard 50 empowers it");
    }

    @Test
    void equipmentTempoRulesActuallyReachTheAuthoritativeGaugeRuntime() {
        SkillDefinition strike = new SkillDefinition(
                "strike", "Strike", TargetRule.ENEMY_SINGLE, 0,
                List.of(SkillEffect.damage(1.0)));
        SkillDefinition grant = new SkillDefinition(
                "grant", "Grant", TargetRule.ALLY_SINGLE, 0,
                List.of(SkillEffect.gaugeAdd(120)));

        CombatantDefinition giverDef = new CombatantDefinition(
                "GIVER", "Giver", new BattleStats(1000, 100, 50, 100),
                "grant", List.of(grant), 4,
                List.of("START_GAUGE_30", "START_GAUGE_50", "ALLY_GAUGE_GRANT_PLUS_20"),
                java.util.Map.of());
        CombatantDefinition allyDef = new CombatantDefinition(
                "ALLY", "Ally", new BattleStats(1000, 100, 50, 90),
                "strike", List.of(strike), 4, List.of(), java.util.Map.of());
        CombatantDefinition targetDef = new CombatantDefinition(
                "TARGET", "Target", new BattleStats(1000, 100, 50, 80),
                "strike", List.of(strike), 0,
                List.of("DIRECT_HIT_GAUGE_20"), java.util.Map.of());

        CombatantState giver = new CombatantState("giver", giverDef, CombatantSide.ALLY, 0);
        CombatantState ally = new CombatantState("ally", allyDef, CombatantSide.ALLY, 1);
        CombatantState target = new CombatantState("target", targetDef, CombatantSide.ENEMY, 2);
        BattleEngine engine = new BattleEngine(new BattleState(List.of(giver, ally, target)));

        assertEquals(80, giver.gauge(), "equipped start-Gauge traits stack once at battle creation");

        giver.setGauge(1000);
        engine.nextReady();
        engine.useSkill("giver", "grant", "ally");
        assertEquals(140, ally.gauge(), "ally Gauge grants include the equipped +20 trait");

        target.setGauge(1000);
        engine.nextReady();
        engine.useSkill("target", "strike", "giver");
        // Giver has no direct-hit armor trait; target's own trait should be checked when target is hit below.
        ally.setGauge(1000);
        engine.nextReady();
        engine.useSkill("ally", "strike", "target");
        assertEquals(20, target.gauge(), "direct enemy hit grants the equipped +20 Gauge once");
    }


    @Test
    void defensiveEquipmentTraitsAffectAuthoritativeState() {
        SkillDefinition strike = new SkillDefinition("strike","Strike",TargetRule.ENEMY_SINGLE,0,List.of(SkillEffect.damage(1.0)));
        CombatantDefinition def = new CombatantDefinition("GEAR","Gear",new BattleStats(1000,100,100,100),
                "strike",List.of(strike),4,List.of("HEAL_RECEIVED_15","BARRIER_RECEIVED_25","HIGH_HP_DR_12","REVIVE_HP_PLUS_20"),java.util.Map.of());
        CombatantState unit = new CombatantState("gear",def,CombatantSide.ALLY,0);
        assertEquals(125, unit.addBarrier(100));
        unit.takeDamage(300);
        int before=unit.hp();
        assertEquals(115, unit.heal(100));
        assertEquals(before+115,unit.hp());
        assertEquals(0.12,unit.damageReduction(),0.000001);
        unit.forceDown();
        assertEquals(500,unit.revive(0.30));
    }

    @Test
    void signatureRulesChangeTempoAndLowHpBehavior() {
        SkillDefinition grant = new SkillDefinition("p02_accelerate","Grant",TargetRule.ALLY_SINGLE,0,List.of(SkillEffect.gaugeAdd(100)));
        CombatantDefinition lumea = new CombatantDefinition("P02","Lumea",new BattleStats(800,90,70,114),
                "p02_accelerate",List.of(grant),5,List.of("SIG_P02_BASIC_SELF_GAUGE_60"),java.util.Map.of());
        CombatantState l=new CombatantState("l",lumea,CombatantSide.ALLY,0);
        CombatantState a=new CombatantState("a",unit("A",9999,10,10,90),CombatantSide.ALLY,1);
        CombatantState e=new CombatantState("e",unit("E",9999,10,10,80),CombatantSide.ENEMY,2);
        l.setGauge(1000);
        BattleEngine engine=new BattleEngine(new BattleState(List.of(l,a,e)));
        engine.nextReady();engine.useSkill("l","p02_accelerate","a");
        assertEquals(60,l.gauge());

        CombatantDefinition raze = new CombatantDefinition("P08","Raze",new BattleStats(1000,100,50,100),
                "strike",List.of(new SkillDefinition("strike","Strike",TargetRule.ENEMY_SINGLE,0,List.of(SkillEffect.damage(1.0)))),3,
                List.of("SIG_P08_LOW_HP_SPEED_15"),java.util.Map.of());
        CombatantState r=new CombatantState("r",raze,CombatantSide.ALLY,0);
        r.takeDamage(600);
        assertEquals(115,r.speed());
    }

    @Test void timelinePreviewDoesNotMutate(){BattleState s=TrainingBattleFactory.create();long[] before=s.combatants().stream().mapToLong(CombatantState::gauge).toArray();assertEquals(8,s.timelinePreview(8).size());assertArrayEquals(before,s.combatants().stream().mapToLong(CombatantState::gauge).toArray());}
    @Test void deterministicAutoControllerTerminatesTrainingBattle(){
        BattleState state=TrainingBattleFactory.create();
        BattleEngine engine=new BattleEngine(state);
        int actions=0;
        while(state.outcome()==BattleOutcome.RUNNING && actions++<200){
            BattleAutoController.chooseAutoAction(engine,state,engine.nextReady());
        }
        assertNotEquals(BattleOutcome.RUNNING,state.outcome());
    }
}
