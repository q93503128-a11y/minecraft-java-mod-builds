package dev.moonseungjun.openworldrpg.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.progression.r01.R01SharedWorldState;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class R01SharedWorldStateTest {
    @Test
    void roadsideCompletionRequiresAllThreeSharedRequirementsAndQueuesParticipants() {
        String player = UUID.randomUUID().toString();
        var state = R01SharedWorldState.initial()
                .advanceActiveWorld(5_000)
                .startRoadside()
                .recordRoadsideParticipation(player, true, Optional.of(RootClass.HUNTER))
                .markRoadsideRequirement(
                        R01SharedWorldState.RoadsideRequirement.THREAT_PACK_DEFEATED
                )
                .markRoadsideRequirement(
                        R01SharedWorldState.RoadsideRequirement.WHEEL_BRACED
                );

        assertFalse(state.roadsideRequirementsComplete());

        state = state.markRoadsideRequirement(
                R01SharedWorldState.RoadsideRequirement.CARGO_RETURNED
        );
        assertTrue(state.roadsideRequirementsComplete());

        var completed = state.completeRoadside();
        assertFalse(completed.roadsideActive());
        assertEquals(5_000L, completed.roadsideLastEndActiveTicks());
        assertEquals(
                1L,
                completed.pendingRoadsideCycle(player).orElseThrow()
        );
        assertTrue(
                completed.pendingRoadsideFinalization(player).dustQuestEligible()
        );
    }

    @Test
    void emptyActiveEventAbandonsAfterExactly120SecondsWithoutRewards() {
        var state = R01SharedWorldState.initial()
                .startRoadside()
                .updateRoadsidePresence(false)
                .advanceActiveWorld(2_399);

        assertFalse(state.roadsideShouldAbandon());

        state = state.advanceActiveWorld(1);
        assertTrue(state.roadsideShouldAbandon());

        var abandoned = state.abandonRoadside();
        assertFalse(abandoned.roadsideActive());
        assertEquals(2_400L, abandoned.roadsideLastEndActiveTicks());
        assertTrue(abandoned.pendingRoadsideFinalizations().isEmpty());
    }

    @Test
    void pendingFinalizationClearsOnlyMatchingCycle() {
        String player = UUID.randomUUID().toString();
        var completed = R01SharedWorldState.initial()
                .startRoadside()
                .recordRoadsideParticipation(player, false, Optional.empty())
                .markRoadsideRequirement(
                        R01SharedWorldState.RoadsideRequirement.THREAT_PACK_DEFEATED
                )
                .markRoadsideRequirement(
                        R01SharedWorldState.RoadsideRequirement.WHEEL_BRACED
                )
                .markRoadsideRequirement(
                        R01SharedWorldState.RoadsideRequirement.CARGO_RETURNED
                )
                .completeRoadside();

        var cleared = completed.clearPendingRoadsideFinalization(player, 1L);

        assertNull(cleared.pendingRoadsideFinalization(player));
    }

    @Test
    void sharedWorldStateSurvivesCodecRoundTrip() {
        String player = UUID.randomUUID().toString();
        var original = R01SharedWorldState.initial()
                .advanceActiveWorld(8_000)
                .startRoadside()
                .recordRoadsideParticipation(player, true, Optional.of(RootClass.HUNTER))
                .markRoadsideRequirement(
                        R01SharedWorldState.RoadsideRequirement.WHEEL_BRACED
                );

        var encoded = R01SharedWorldState.CODEC
                .encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow();
        var decoded = R01SharedWorldState.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();

        assertEquals(original, decoded);
    }
}
