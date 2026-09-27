package io.github.q93503128.turnbound.combat;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SharedBattleOwnershipTest {
    @Test void duplicateHeroesAndOwnerChainsStayPlayerScoped() {
        UUID first=UUID.randomUUID(),second=UUID.randomUUID();
        CombatantState firstHero=new CombatantState("p1_ally_p01",PrototypeRoster.kyren(),CombatantSide.ALLY,0);
        CombatantState secondHero=new CombatantState("p2_ally_p01",PrototypeRoster.kyren(),CombatantSide.ALLY,1);
        CombatantState chained=new CombatantState("p1_temporary",PrototypeRoster.kyren(),CombatantSide.ALLY,2);
        CombatantState enemy=new CombatantState("enemy",PrototypeRoster.trainingEnemy("E_TEST","Enemy",10,1,1,1),CombatantSide.ENEMY,3);
        chained.setRef("ownerId",firstHero.instanceId());
        BattleState state=new BattleState(List.of(firstHero,secondHero,chained,enemy));
        Map<String,UUID> owners=Map.of(firstHero.instanceId(),first,secondHero.instanceId(),second);
        assertEquals(first,SharedBattleOwnership.ownerOf(state,owners,firstHero.instanceId()));
        assertEquals(second,SharedBattleOwnership.ownerOf(state,owners,secondHero.instanceId()));
        assertEquals(first,SharedBattleOwnership.ownerOf(state,owners,chained.instanceId()));
    }
}
