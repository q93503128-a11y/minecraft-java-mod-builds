package io.github.q93503128.turnbound.presentation;

import io.github.q93503128.turnbound.combat.BattleState;
import io.github.q93503128.turnbound.combat.CombatantSide;
import io.github.q93503128.turnbound.combat.CombatantState;
import io.github.q93503128.turnbound.content.CanonicalData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeroSignaturePresentationStateTest {
    @Test
    void v1CoreResourcesProjectTheActualRuntimeCountersIncludingZero() {
        Map<String, String> ids = Map.of(
                "P01", "focus",
                "P03", "guard",
                "P05", "shot",
                "P06", "records",
                "P07", "bond",
                "P08", "fury");
        Map<String, Integer> max = Map.of(
                "P01", 3,
                "P03", 100,
                "P05", 2,
                "P06", 5,
                "P07", 100,
                "P08", 100);

        for (String heroId : ids.keySet()) {
            CombatantState hero = new CombatantState(heroId.toLowerCase(), CanonicalData.definition(heroId),
                    CombatantSide.ALLY, 0);
            var resource = HeroSignaturePresentationState.resource(hero);
            assertEquals(ids.get(heroId), resource.id(), heroId);
            assertEquals(0, resource.value(), heroId + " zero state must remain visible");
            assertEquals(max.get(heroId), resource.max(), heroId);
            assertEquals("@r:" + ids.get(heroId) + ":0:" + max.get(heroId), resource.token(), heroId);
        }
    }

    @Test
    void duelAndSightlineMarkersProjectOntoTheActualEnemyTarget() {
        CombatantState kyren = new CombatantState("kyren", CanonicalData.definition("P01"), CombatantSide.ALLY, 0);
        CombatantState lynette = new CombatantState("lynette", CanonicalData.definition("P05"), CombatantSide.ALLY, 1);
        CombatantState enemy = new CombatantState("enemy", CanonicalData.definition("E001"), CombatantSide.ENEMY, 0);
        kyren.setRef("focusTarget", enemy.instanceId());
        lynette.setRef("sightline", enemy.instanceId());
        BattleState state = new BattleState(List.of(kyren, lynette, enemy));

        List<String> tokens = HeroSignaturePresentationState.tokens(state, enemy);
        assertTrue(tokens.contains("@m:duel"));
        assertTrue(tokens.contains("@m:sightline"));

        kyren.forceDown();
        lynette.forceDown();
        List<String> afterDown = HeroSignaturePresentationState.tokens(state, enemy);
        assertTrue(afterDown.stream().noneMatch(token -> token.startsWith("@m:")));
    }
}
