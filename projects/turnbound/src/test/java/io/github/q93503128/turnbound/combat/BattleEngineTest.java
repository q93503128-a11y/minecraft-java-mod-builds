package io.github.q93503128.turnbound.combat;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

final class BattleEngineTest {
    private static CombatantDefinition unit(String id,int hp,int atk,int def,int spd){return PrototypeRoster.trainingEnemy(id,id,hp,atk,def,spd);}
    @Test void overflowGaugeCreatesNaturalConsecutiveTurns(){var fast=new CombatantState("fast",unit("FAST",9999,10,10,220),CombatantSide.ALLY,0);var slow=new CombatantState("slow",unit("SLOW",9999,10,10,100),CombatantSide.ENEMY,1);var e=new BattleEngine(new BattleState(List.of(fast,slow)));assertEquals("fast",e.nextReady().instanceId());e.useSkill("fast","fast_basic","slow");assertEquals(100L,fast.gauge());assertEquals("fast",e.nextReady().instanceId());e.useSkill("fast","fast_basic","slow");assertEquals("slow",e.nextReady().instanceId());}
    @Test void cooldownTicksOnlyOnFutureOwnActions(){var k=new CombatantState("k",PrototypeRoster.kyren(),CombatantSide.ALLY,0);var d=new CombatantState("d",unit("D",9999,1,1,1),CombatantSide.ENEMY,1);k.setGauge(1000);var e=new BattleEngine(new BattleState(List.of(k,d)));e.nextReady();e.useSkill("k","p01_breaker_strike","d");assertEquals(2,k.cooldown("p01_breaker_strike"));k.setGauge(1000);e.nextReady();e.useSkill("k","p01_chase_slash","d");assertEquals(1,k.cooldown("p01_breaker_strike"));k.setGauge(1000);e.nextReady();e.useSkill("k","p01_chase_slash","d");assertEquals(0,k.cooldown("p01_breaker_strike"));}
    @Test void basicActionCanHeal(){var h=new CombatantState("h",PrototypeRoster.elysia(),CombatantSide.ALLY,0);var a=new CombatantState("a",PrototypeRoster.kyren(),CombatantSide.ALLY,1);var en=new CombatantState("e",unit("E",9999,1,1,1),CombatantSide.ENEMY,2);a.takeDamage(500);h.setGauge(1000);var e=new BattleEngine(new BattleState(List.of(h,a,en)));int before=a.hp();e.nextReady();e.useSkill("h","p04_heal","a");assertTrue(a.hp()>before);assertEquals(0,h.cooldown("p04_heal"));}
    @Test void reviveReturnsAtThirtyPercent(){var h=new CombatantState("h",PrototypeRoster.elysia(),CombatantSide.ALLY,0);var a=new CombatantState("a",PrototypeRoster.kyren(),CombatantSide.ALLY,1);var en=new CombatantState("e",unit("E",9999,1,1,1),CombatantSide.ENEMY,2);a.takeDamage(99999);h.setGauge(1000);var e=new BattleEngine(new BattleState(List.of(h,a,en)));e.nextReady();e.useSkill("h","p04_returned_breath","a");assertFalse(a.downed());assertEquals((int)Math.floor(a.maxHp()*0.30),a.hp());assertEquals(0,a.gauge());}
    @Test void timelinePreviewDoesNotMutate(){BattleState s=P0Scenario.create();long[] before=s.combatants().stream().mapToLong(CombatantState::gauge).toArray();assertEquals(8,s.timelinePreview(8).size());assertArrayEquals(before,s.combatants().stream().mapToLong(CombatantState::gauge).toArray());}
    @Test void p0DiagnosticTerminates(){
        // The original P0 guard used 120 actions for a three-enemy fixture. The current
        // stress fixture contains five specialized enemies; keep a finite runaway guard
        // without changing canonical combat stats merely to satisfy the old fixture size.
        String r=P0Scenario.runAutoDiagnostic(200);
        assertFalse(r.contains("outcome=RUNNING"),r);
    }
}
