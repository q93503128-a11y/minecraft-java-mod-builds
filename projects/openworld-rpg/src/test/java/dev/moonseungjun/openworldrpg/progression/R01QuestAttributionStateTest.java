package dev.moonseungjun.openworldrpg.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.mojang.serialization.JsonOps;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.progression.r01.R01PlayerState;
import dev.moonseungjun.openworldrpg.progression.r01.R01QuestAttributionState;
import org.junit.jupiter.api.Test;

class R01QuestAttributionStateTest {
    @Test
    void duplicateObjectiveNeverReassignsItsOriginalContributionClass() {
        var first = R01QuestAttributionState.initial().recordDustAction(
                R01PlayerState.QuarryRoadAction.LOST_CARGO,
                RootClass.HUNTER
        );
        var retriedAfterSwap = first.recordDustAction(
                R01PlayerState.QuarryRoadAction.LOST_CARGO,
                RootClass.MAGE
        );

        assertSame(first, retriedAfterSwap);
        assertEquals(
                RootClass.HUNTER,
                retriedAfterSwap.dustActionOwners().get("lost_cargo")
        );
    }

    @Test
    void strictMajorityOwnsQuestClassXpWhileThreeWaySplitFallsBackExternally() {
        var majority = R01QuestAttributionState.initial()
                .recordDustAction(
                        R01PlayerState.QuarryRoadAction.LOST_CARGO,
                        RootClass.CLERIC
                )
                .recordDustAction(
                        R01PlayerState.QuarryRoadAction.MEADOW_VIPER,
                        RootClass.CLERIC
                )
                .recordDustAction(
                        R01PlayerState.QuarryRoadAction.BROKEN_ROAD_MARKER,
                        RootClass.WARRIOR
                );

        assertEquals(RootClass.CLERIC, majority.dustMajorityClass().orElseThrow());

        var split = R01QuestAttributionState.initial()
                .recordDustAction(
                        R01PlayerState.QuarryRoadAction.LOST_CARGO,
                        RootClass.CLERIC
                )
                .recordDustAction(
                        R01PlayerState.QuarryRoadAction.MEADOW_VIPER,
                        RootClass.WARRIOR
                )
                .recordDustAction(
                        R01PlayerState.QuarryRoadAction.BROKEN_ROAD_MARKER,
                        RootClass.MAGE
                );

        assertFalse(split.dustMajorityClass().isPresent());
    }

    @Test
    void attributionSurvivesCodecRoundTrip() {
        var original = R01QuestAttributionState.initial()
                .recordDustAction(
                        R01PlayerState.QuarryRoadAction.R01_GATHERING_NODE,
                        RootClass.GUARDIAN
                );

        var encoded = R01QuestAttributionState.CODEC
                .encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow();
        var decoded = R01QuestAttributionState.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();

        assertEquals(original, decoded);
    }
}
