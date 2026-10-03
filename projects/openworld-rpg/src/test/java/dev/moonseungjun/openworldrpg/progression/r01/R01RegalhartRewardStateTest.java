package dev.moonseungjun.openworldrpg.progression.r01;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class R01RegalhartRewardStateTest {
    private static final String ENCOUNTER =
            "openworld_rpg:r01/regalhart/00000000-0000-0000-0000-000000000099";
    private static final String PLAYER =
            "00000000-0000-0000-0000-000000000001";

    @Test
    void firstContributionFreezesRewardClassForEncounter() {
        var state = R01RegalhartRewardState.initial()
                .recordParticipation(
                        ENCOUNTER,
                        0L,
                        PLAYER,
                        RootClass.WARRIOR
                )
                .recordParticipation(
                        ENCOUNTER,
                        0L,
                        PLAYER,
                        RootClass.MAGE
                );

        assertEquals(
                RootClass.WARRIOR,
                state.encounter(ENCOUNTER)
                        .orElseThrow()
                        .participants()
                        .get(PLAYER)
        );
    }

    @Test
    void firstPlanCommitsPersonalFirstDefeatBeforeDelivery() {
        var state = R01RegalhartRewardState.initial()
                .recordParticipation(
                        ENCOUNTER,
                        0L,
                        PLAYER,
                        RootClass.HUNTER
                );
        var firstPlan = firstPlan();

        state = state.completeEncounter(
                ENCOUNTER,
                Map.of(PLAYER, firstPlan)
        );

        assertTrue(state.firstDefeatCommitted(PLAYER));
        assertTrue(state.encounter(ENCOUNTER).isEmpty());
        assertEquals(1, state.pendingForPlayer(PLAYER).size());
        assertTrue(
                state.pendingForPlayer(PLAYER)
                        .getFirst()
                        .plan()
                        .firstEligibleDefeat()
        );
    }

    @Test
    void laterEncounterForSamePlayerRequiresRepeatPlan() {
        var state = R01RegalhartRewardState.initial()
                .recordParticipation(
                        ENCOUNTER,
                        0L,
                        PLAYER,
                        RootClass.GUARDIAN
                )
                .completeEncounter(
                        ENCOUNTER,
                        Map.of(PLAYER, firstPlan())
                );

        String later =
                "openworld_rpg:r01/regalhart/00000000-0000-0000-0000-000000000100";
        state = state.recordParticipation(
                later,
                1L,
                PLAYER,
                RootClass.GUARDIAN
        );
        var repeat = repeatPlan();
        state = state.completeEncounter(
                later,
                Map.of(PLAYER, repeat)
        );

        assertEquals(2, state.pendingForPlayer(PLAYER).size());
        assertFalse(
                state.pendingForPlayer(PLAYER)
                        .stream()
                        .filter(pending -> pending.encounterId().equals(later))
                        .findFirst()
                        .orElseThrow()
                        .plan()
                        .firstEligibleDefeat()
        );
    }

    @Test
    void stateCodecPreservesPendingAndFirstDefeatHistory() {
        var original = R01RegalhartRewardState.initial()
                .recordParticipation(
                        ENCOUNTER,
                        3L,
                        PLAYER,
                        RootClass.CLERIC
                )
                .completeEncounter(
                        ENCOUNTER,
                        Map.of(PLAYER, firstPlan())
                );

        var encoded = R01RegalhartRewardState.CODEC
                .encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow();
        var decoded = R01RegalhartRewardState.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();

        assertEquals(original, decoded);
        assertTrue(decoded.firstDefeatCommitted(PLAYER));
    }

    @Test
    void rewardIdIncludesEncounterCycleAndPlayer() {
        assertEquals(
                ENCOUNTER + "/cycle/7/reward/" + PLAYER,
                R01RegalhartRewardState.rewardId(
                        ENCOUNTER,
                        7L,
                        PLAYER
                )
        );
        UUID.fromString(PLAYER);
    }

    private static R01RegalhartRewardRules.RewardPlan firstPlan() {
        return R01RegalhartRewardRules.createPlan(
                true, 0, 0, 30, 0, 1L,
                99, 1, 0, 30, 0, 2L, 99
        );
    }

    private static R01RegalhartRewardRules.RewardPlan repeatPlan() {
        return R01RegalhartRewardRules.createPlan(
                false, 0, 0, 30, 0, 1L,
                99, 1, 0, 30, 0, 2L, 99
        );
    }
}
