package io.github.q93503128.turnbound.session;

import io.github.q93503128.turnbound.combat.CombatantSide;
import io.github.q93503128.turnbound.combat.CombatantState;
import io.github.q93503128.turnbound.combat.PrototypeRoster;
import io.github.q93503128.turnbound.combat.StatusInstance;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class BattleSnapshotPresentationTest {
    @Test void characterResourcesAndStatusStacksReachTheClientWithoutReplacingRawStatusIds(){
        CombatantState kyren=new CombatantState("k",PrototypeRoster.kyren(), CombatantSide.ALLY,0);
        kyren.setCounter("focus",2);
        assertTrue(BattleSnapshotCodec.presentationStates(kyren).contains("@r:focus:2:3"));

        CombatantState enemy=new CombatantState("e",PrototypeRoster.corruptedWalker(),CombatantSide.ENEMY,1);
        enemy.putStatus(new StatusInstance("exposed","lynette",999,0.0,2));
        var states=BattleSnapshotCodec.presentationStates(enemy);
        assertTrue(states.contains("exposed"));
        assertTrue(states.contains("@s:exposed:2:999:0.000"));
    }
}
