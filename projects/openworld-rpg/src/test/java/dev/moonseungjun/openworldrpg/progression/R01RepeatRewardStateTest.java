package dev.moonseungjun.openworldrpg.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.progression.r01.R01RepeatRewardState;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class R01RepeatRewardStateTest {
    @Test
    void onePendingRoadsidePlanCommitsToBoundedLastCycleReceipt() {
        var plan = new R01RepeatRewardState.RoadsideRewardPlan(
                7L,
                15L,
                Optional.of(RootClass.CLERIC),
                5L,
                20L
        );
        var pending = R01RepeatRewardState.initial().beginRoadside(plan);
        var completed = pending.completeRoadside(7L);

        assertEquals(plan, pending.pendingRoadsideReward().orElseThrow());
        assertEquals(7L, completed.lastRoadsideRewardedCycle());
        assertTrue(completed.pendingRoadsideReward().isEmpty());
        assertSame(completed, completed.completeRoadside(7L));
    }

    @Test
    void pendingPlanCannotBeRerolledOrSwapContributionClass() {
        var hunter = new R01RepeatRewardState.RoadsideRewardPlan(
                3L,
                10L,
                Optional.of(RootClass.HUNTER),
                4L,
                20L
        );
        var mage = new R01RepeatRewardState.RoadsideRewardPlan(
                3L,
                10L,
                Optional.of(RootClass.MAGE),
                4L,
                20L
        );
        var pending = R01RepeatRewardState.initial().beginRoadside(hunter);

        assertThrows(
                IllegalStateException.class,
                () -> pending.beginRoadside(mage)
        );
    }

    @Test
    void noClassContributionMayCarryCombatXpAndGoldButNeverClassXp() {
        var plan = new R01RepeatRewardState.RoadsideRewardPlan(
                1L,
                8L,
                Optional.empty(),
                0L,
                20L
        );
        assertFalse(plan.rewardClass().isPresent());

        assertThrows(
                IllegalArgumentException.class,
                () -> new R01RepeatRewardState.RoadsideRewardPlan(
                        1L,
                        8L,
                        Optional.empty(),
                        1L,
                        20L
                )
        );
    }

    @Test
    void repeatRewardStateSurvivesCodecRoundTrip() {
        var original = R01RepeatRewardState.initial().beginRoadside(
                new R01RepeatRewardState.RoadsideRewardPlan(
                        4L,
                        12L,
                        Optional.of(RootClass.WARRIOR),
                        4L,
                        20L
                )
        );

        var encoded = R01RepeatRewardState.CODEC
                .encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow();
        var decoded = R01RepeatRewardState.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();

        assertEquals(original, decoded);
    }
}
