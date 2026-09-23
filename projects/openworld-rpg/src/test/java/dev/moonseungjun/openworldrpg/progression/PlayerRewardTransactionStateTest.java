package dev.moonseungjun.openworldrpg.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.progression.reward.PlayerRewardTransactionState;
import org.junit.jupiter.api.Test;

class PlayerRewardTransactionStateTest {
    @Test
    void persistedRewardPlanCannotBeRerolledUnderSameTransactionId() {
        var plan = new PlayerRewardTransactionState.RewardPlan(
                105,
                RootClass.HUNTER,
                60,
                90
        );
        var state = PlayerRewardTransactionState.initial()
                .begin("openworld_rpg:r01/dust_on_quarry_road", plan);

        assertEquals(
                plan,
                state.pendingPlan("openworld_rpg:r01/dust_on_quarry_road").orElseThrow()
        );
        assertSame(
                state,
                state.begin("openworld_rpg:r01/dust_on_quarry_road", plan)
        );
        assertThrows(
                IllegalStateException.class,
                () -> state.begin(
                        "openworld_rpg:r01/dust_on_quarry_road",
                        new PlayerRewardTransactionState.RewardPlan(
                                200,
                                RootClass.HUNTER,
                                60,
                                90
                        )
                )
        );
    }

    @Test
    void completionMovesPlanOutOfPendingAndBecomesIdempotent() {
        var plan = new PlayerRewardTransactionState.RewardPlan(
                105,
                RootClass.HUNTER,
                60,
                90
        );
        var pending = PlayerRewardTransactionState.initial()
                .begin("openworld_rpg:r01/dust_on_quarry_road", plan);
        var completed = pending.complete("openworld_rpg:r01/dust_on_quarry_road");

        assertFalse(completed.pendingPlan(
                "openworld_rpg:r01/dust_on_quarry_road"
        ).isPresent());
        assertTrue(completed.isCompleted(
                "openworld_rpg:r01/dust_on_quarry_road"
        ));
        assertSame(
                completed,
                completed.complete("openworld_rpg:r01/dust_on_quarry_road")
        );
    }

    @Test
    void rewardTransactionStateSurvivesCodecRoundTrip() {
        var original = PlayerRewardTransactionState.initial().begin(
                "openworld_rpg:r01/earthloong_first_clear/progression",
                new PlayerRewardTransactionState.RewardPlan(
                        1064,
                        RootClass.MAGE,
                        101,
                        180
                )
        );

        var encoded = PlayerRewardTransactionState.CODEC
                .encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow();
        var decoded = PlayerRewardTransactionState.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();

        assertEquals(original, decoded);
    }
}
