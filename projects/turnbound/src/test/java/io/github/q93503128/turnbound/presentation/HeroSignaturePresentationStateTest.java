package io.github.q93503128.turnbound.presentation;

import io.github.q93503128.turnbound.combat.BattleState;
import io.github.q93503128.turnbound.combat.CombatantSide;
import io.github.q93503128.turnbound.combat.CombatantState;
import io.github.q93503128.turnbound.combat.StatusInstance;
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
    void signatureResourcesMapToReadableBodyLanguageStages() {
        assertStages("P01", "focus", new int[]{0,1,2,3}, new int[]{0,1,2,3});
        assertStages("P03", "guard", new int[]{0,1,49,50,99,100}, new int[]{0,1,1,2,2,3});
        assertStages("P05", "shot", new int[]{0,1,2}, new int[]{0,1,2});
        assertStages("P06", "records", new int[]{0,1,2,3,4,5}, new int[]{0,1,1,2,2,3});
        assertStages("P07", "bond", new int[]{0,1,49,50,99,100}, new int[]{0,1,1,2,2,3});
        assertStages("P08", "fury", new int[]{0,1,59,60,79,80,100}, new int[]{0,1,1,2,2,3,3});
    }

    @Test
    void razeOverheatOverridesFuryPoseWhileItsThreeSelfModifiersAreActive() {
        CombatantState raze = new CombatantState("raze", CanonicalData.definition("P08"), CombatantSide.ALLY, 0);
        raze.setCounter("fury", 20);
        assertEquals("state_1", HeroSignaturePresentationState.visualState(raze).animationKey());

        raze.putStatus(new StatusInstance("attack_multiplier", raze.instanceId(), 3, 0.20));
        raze.putStatus(new StatusInstance("speed_multiplier", raze.instanceId(), 3, 0.10));
        raze.putStatus(new StatusInstance("defense_multiplier", raze.instanceId(), 3, -0.15));
        assertEquals("overheat", HeroSignaturePresentationState.visualState(raze).animationKey());

        raze.forceDown();
        assertEquals("state_0", HeroSignaturePresentationState.visualState(raze).animationKey());
    }

    private static void assertStages(String heroId, String counter, int[] values, int[] expected) {
        CombatantState hero = new CombatantState(heroId.toLowerCase(), CanonicalData.definition(heroId),
                CombatantSide.ALLY, 0);
        for (int i = 0; i < values.length; i++) {
            hero.setCounter(counter, values[i]);
            assertEquals(expected[i], HeroSignaturePresentationState.visualState(hero).stage(),
                    heroId + " value=" + values[i]);
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
