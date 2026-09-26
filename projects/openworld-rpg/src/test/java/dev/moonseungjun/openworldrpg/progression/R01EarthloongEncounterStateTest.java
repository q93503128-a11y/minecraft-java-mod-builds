package dev.moonseungjun.openworldrpg.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.progression.r01.R01EarthloongEncounterState;
import org.junit.jupiter.api.Test;

class R01EarthloongEncounterStateTest {
    private static final String ENCOUNTER =
            "openworld_rpg:r01/earthloong/00000000-0000-0000-0000-000000000001";
    private static final String PLAYER_A =
            "00000000-0000-0000-0000-00000000000a";
    private static final String PLAYER_B =
            "00000000-0000-0000-0000-00000000000b";

    @Test
    void firstValidContributionFixesRewardClassAndCompletionBecomesPending() {
        var state = R01EarthloongEncounterState.initial()
                .recordParticipation(ENCOUNTER, PLAYER_A, RootClass.MAGE)
                .recordParticipation(ENCOUNTER, PLAYER_A, RootClass.WARRIOR)
                .recordParticipation(ENCOUNTER, PLAYER_B, RootClass.GUARDIAN);

        assertTrue(state.hasParticipant(ENCOUNTER, PLAYER_A));
        assertEquals(
                RootClass.MAGE,
                state.activeEncounters().get(ENCOUNTER)
                        .participants().get(PLAYER_A)
        );

        var completed = state.completeEncounter(ENCOUNTER);
        assertFalse(completed.activeEncounters().containsKey(ENCOUNTER));
        assertEquals(
                RootClass.MAGE,
                completed.pendingFinalization(PLAYER_A)
                        .orElseThrow()
                        .rewardClass()
        );
        assertEquals(
                RootClass.GUARDIAN,
                completed.pendingFinalization(PLAYER_B)
                        .orElseThrow()
                        .rewardClass()
        );
    }

    @Test
    void pendingFinalizationClearsOnlyMatchingEncounter() {
        var completed = R01EarthloongEncounterState.initial()
                .recordParticipation(ENCOUNTER, PLAYER_A, RootClass.HUNTER)
                .completeEncounter(ENCOUNTER);

        var cleared = completed.clearPendingFinalization(PLAYER_A, ENCOUNTER);
        assertTrue(cleared.pendingFinalization(PLAYER_A).isEmpty());
    }

    @Test
    void encounterStateSurvivesCodecRoundTrip() {
        var original = R01EarthloongEncounterState.initial()
                .recordParticipation(ENCOUNTER, PLAYER_A, RootClass.CLERIC)
                .completeEncounter(ENCOUNTER);

        var encoded = R01EarthloongEncounterState.CODEC
                .encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow();
        var decoded = R01EarthloongEncounterState.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();

        assertEquals(original, decoded);
    }
}
