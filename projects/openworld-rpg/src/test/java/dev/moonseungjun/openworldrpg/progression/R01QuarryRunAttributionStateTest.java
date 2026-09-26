package dev.moonseungjun.openworldrpg.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.progression.r01.R01QuarryRunAttributionState;
import dev.moonseungjun.openworldrpg.progression.r01.R01QuarryRunContribution;
import org.junit.jupiter.api.Test;

class R01QuarryRunAttributionStateTest {
    @Test
    void duplicateContributionCannotInflateClassOwnership() {
        var state = R01QuarryRunAttributionState.initial()
                .beginRun(7L)
                .record(
                        7L,
                        R01QuarryRunContribution.UPPER_GALLERY_COMBAT,
                        RootClass.WARRIOR
                )
                .record(
                        7L,
                        R01QuarryRunContribution.UPPER_GALLERY_COMBAT,
                        RootClass.MAGE
                );

        assertEquals(1, state.contributionOwners().size());
        assertEquals(
                RootClass.WARRIOR,
                state.contributionOwners().get(
                        R01QuarryRunContribution.UPPER_GALLERY_COMBAT
                )
        );
    }

    @Test
    void mostUsedClassOwnsDungeonCompletion() {
        var state = R01QuarryRunAttributionState.initial()
                .beginRun(8L)
                .record(
                        8L,
                        R01QuarryRunContribution.UPPER_GALLERY_COMBAT,
                        RootClass.HUNTER
                )
                .record(
                        8L,
                        R01QuarryRunContribution.COLLAPSED_HOIST_COMBAT,
                        RootClass.HUNTER
                )
                .record(
                        8L,
                        R01QuarryRunContribution.LIFT_SHORTCUT,
                        RootClass.MAGE
                );

        assertEquals(RootClass.HUNTER, state.completionClass().orElseThrow());
    }

    @Test
    void tieUsesEarlierCanonicalContributionNotCompletionTimeClass() {
        var state = R01QuarryRunAttributionState.initial()
                .beginRun(9L)
                .record(
                        9L,
                        R01QuarryRunContribution.UPPER_GALLERY_COMBAT,
                        RootClass.CLERIC
                )
                .record(
                        9L,
                        R01QuarryRunContribution.COLLAPSED_HOIST_COMBAT,
                        RootClass.GUARDIAN
                )
                .record(
                        9L,
                        R01QuarryRunContribution.RELAY_EVIDENCE,
                        RootClass.GUARDIAN
                )
                .record(
                        9L,
                        R01QuarryRunContribution.EARTHLOONG_COMBAT,
                        RootClass.CLERIC
                );

        assertEquals(RootClass.CLERIC, state.completionClass().orElseThrow());
    }

    @Test
    void newRunClearsPreviousBoundedEvidence() {
        var state = R01QuarryRunAttributionState.initial()
                .beginRun(10L)
                .record(
                        10L,
                        R01QuarryRunContribution.ROOT_BREACHED_COMBAT,
                        RootClass.MAGE
                )
                .beginRun(11L);

        assertEquals(11L, state.runId());
        assertTrue(state.contributionOwners().isEmpty());
    }

    @Test
    void attributionSurvivesCodecRoundTrip() {
        var original = R01QuarryRunAttributionState.initial()
                .beginRun(12L)
                .record(
                        12L,
                        R01QuarryRunContribution.RELAY_EVIDENCE,
                        RootClass.GUARDIAN
                );

        var encoded = R01QuarryRunAttributionState.CODEC
                .encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow();
        var decoded = R01QuarryRunAttributionState.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();

        assertEquals(original, decoded);
    }
}
