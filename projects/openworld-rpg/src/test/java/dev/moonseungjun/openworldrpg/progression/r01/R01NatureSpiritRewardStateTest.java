package dev.moonseungjun.openworldrpg.progression.r01;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class R01NatureSpiritRewardStateTest {
    private static final String ENCOUNTER =
            "openworld_rpg:r01/nature_spirit/11111111-2222-3333-4444-555555555555";
    private static final String PLAYER =
            UUID.fromString("99999999-2222-3333-4444-555555555555").toString();

    @Test
    void firstQualifyingClassWinsAndCompletionPersistsExactPlan() {
        var state = R01NatureSpiritRewardState.initial()
                .recordParticipation(
                        ENCOUNTER,
                        PLAYER,
                        RootClass.MAGE
                )
                .recordParticipation(
                        ENCOUNTER,
                        PLAYER,
                        RootClass.WARRIOR
                );
        assertTrue(state.hasParticipant(ENCOUNTER, PLAYER));
        assertEquals(
                RootClass.MAGE,
                state.encounter(ENCOUNTER)
                        .orElseThrow()
                        .participants()
                        .get(PLAYER)
        );

        var plan = R01NatureSpiritRewardRules.createPlan(
                99, 0, 0, 0, 0, 10L,
                0, 1, 0
        );
        state = state.completeEncounter(
                ENCOUNTER,
                Map.of(PLAYER, plan)
        );

        assertTrue(state.encounter(ENCOUNTER).isEmpty());
        var pending = state.pendingForPlayer(PLAYER);
        assertEquals(1, pending.size());
        assertEquals(RootClass.MAGE, pending.getFirst().rewardClass());
        assertEquals(plan, pending.getFirst().plan());
        assertEquals(
                R01NatureSpiritRewardState.rewardId(
                        ENCOUNTER,
                        PLAYER
                ),
                pending.getFirst().rewardId()
        );
    }

    @Test
    void differentNatureSpiritInstancesCanQueueIndependentPersonalPlans() {
        String second =
                "openworld_rpg:r01/nature_spirit/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
        var planA = R01NatureSpiritRewardRules.createPlan(
                99, 0, 0, 0, 0, 1L,
                99, 0, 99
        );
        var planB = R01NatureSpiritRewardRules.createPlan(
                99, 0, 0, 0, 0, 2L,
                99, 0, 99
        );

        var state = R01NatureSpiritRewardState.initial()
                .recordParticipation(ENCOUNTER, PLAYER, RootClass.CLERIC)
                .completeEncounter(ENCOUNTER, Map.of(PLAYER, planA))
                .recordParticipation(second, PLAYER, RootClass.HUNTER)
                .completeEncounter(second, Map.of(PLAYER, planB));

        assertEquals(2, state.pendingForPlayer(PLAYER).size());
    }
}
