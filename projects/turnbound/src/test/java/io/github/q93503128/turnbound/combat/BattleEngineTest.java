package io.github.q93503128.turnbound.combat;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

final class BattleEngineTest {
    private static CombatantDefinition unit(String id,int hp,int atk,int def,int spd){return PrototypeRoster.trainingEnemy(id,id,hp,atk,def,spd);}
    @Test void overflowGaugeCreatesNaturalConsecutiveTurns(){var fast=new CombatantState("fast",unit("FAST",9999,10,10,220),CombatantSide.ALLY,0);var slow=new CombatantState("slow",unit("SLOW",9999,10,10,100),CombatantSide.ENEMY,1);var e=new BattleEngine(new BattleState(List.of(fast,slow)));assertEquals("fast",e.nextReady().instanceId());e.useSkill("fast","fast_basic","slow");assertEquals(100L,fast.gauge());assertEquals("fast",e.nextReady().instanceId());e.useSkill("fast","fast_basic","slow");assertEquals("slow",e.nextReady().instanceId());}
    @Test void cooldownTicksOnlyOnFutureOwnActions(){var k=new CombatantState("k",PrototypeRoster.kyren(),CombatantSide.ALLY,0);var d=new CombatantState("d",unit("D",9999,1,1,1),CombatantSide.ENEMY,1);k.setGauge(1000);var e=new BattleEngine(new BattleState(List.of(k,d)));e.nextReady();e.useSkill("k","p01_shatter","d");assertEquals(2,k.cooldown("p01_shatter"));k.setGauge(1000);e.nextReady();e.useSkill("k","p01_basic","d");assertEquals(1,k.cooldown("p01_shatter"));k.setGauge(1000);e.nextReady();e.useSkill("k","p01_basic","d");assertEquals(0,k.cooldown("p01_shatter"));}
    @Test void basicActionCanHeal(){var h=new CombatantState("h",PrototypeRoster.elysia(),CombatantSide.ALLY,0);var a=new CombatantState("a",PrototypeRoster.kyren(),CombatantSide.ALLY,1);var en=new CombatantState("e",unit("E",9999,1,1,1),CombatantSide.ENEMY,2);a.takeDamage(500);h.setGauge(1000);var e=new BattleEngine(new BattleState(List.of(h,a,en)));int before=a.hp();e.nextReady();e.useSkill("h","p04_basic","a");assertTrue(a.hp()>before);assertEquals(0,h.cooldown("p04_basic"));}
    @Test void reviveReturnsAtThirtyPercent(){var h=new CombatantState("h",PrototypeRoster.elysia(),CombatantSide.ALLY,0);var a=new CombatantState("a",PrototypeRoster.kyren(),CombatantSide.ALLY,1);var en=new CombatantState("e",unit("E",9999,1,1,1),CombatantSide.ENEMY,2);a.takeDamage(99999);h.setGauge(1000);var e=new BattleEngine(new BattleState(List.of(h,a,en)));e.nextReady();e.useSkill("h","p04_revive","a");assertFalse(a.downed());assertEquals((int)Math.floor(a.maxHp()*0.30),a.hp());assertEquals(0,a.gauge());}
    @Test void timelinePreviewDoesNotMutate(){BattleState s=P0Scenario.create();long[] before=s.combatants().stream().mapToLong(CombatantState::gauge).toArray();assertEquals(8,s.timelinePreview(8).size());assertArrayEquals(before,s.combatants().stream().mapToLong(CombatantState::gauge).toArray());}
    @Test void p0DiagnosticTerminates(){String r=P0Scenario.runAutoDiagnostic(120);assertFalse(r.contains("outcome=RUNNING"),r);}
}
