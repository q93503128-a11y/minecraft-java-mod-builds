package io.github.q93503128.turnbound.session;

import io.github.q93503128.turnbound.combat.CombatantSide;
import io.github.q93503128.turnbound.combat.CombatantState;
import io.github.q93503128.turnbound.combat.PrototypeRoster;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BattleFormationLayoutTest {
    @Test void soloStaysOnOriginalSingleRow() {
        CombatantState unit=new CombatantState("a",PrototypeRoster.kyren(),CombatantSide.ALLY,0);
        var first=BattleFormationLayout.ally(unit,0,4,0);
        var last=BattleFormationLayout.ally(unit,3,4,0);
        assertEquals(-3.0,first.x(),0.001);
        assertEquals(3.0,last.x(),0.001);
        assertEquals(4.0,first.z(),0.001);
        assertEquals(4.0,last.z(),0.001);
    }

    @Test void eachMultiplayerOwnerGetsOwnTwoByTwoBlock() {
        CombatantState p1a=new CombatantState("p1a",PrototypeRoster.kyren(),CombatantSide.ALLY,0);
        CombatantState p1d=new CombatantState("p1d",PrototypeRoster.kyren(),CombatantSide.ALLY,1);
        CombatantState p2a=new CombatantState("p2a",PrototypeRoster.kyren(),CombatantSide.ALLY,2);
        p1a.setPresentationSlot(0,0);
        p1d.setPresentationSlot(0,3);
        p2a.setPresentationSlot(1,0);

        var a=BattleFormationLayout.ally(p1a,0,8,2);
        var d=BattleFormationLayout.ally(p1d,3,8,2);
        var b=BattleFormationLayout.ally(p2a,4,8,2);
        assertEquals(1.8,d.x()-a.x(),0.001);
        assertEquals(1.8,d.z()-a.z(),0.001);
        assertEquals(4.8,b.x()-a.x(),0.001);
    }
}
