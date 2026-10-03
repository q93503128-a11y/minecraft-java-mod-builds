package dev.moonseungjun.openworldrpg.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import dev.moonseungjun.openworldrpg.progression.r01.R01RegalhartTerritoryRules;
import dev.moonseungjun.openworldrpg.progression.r01.R01RegalhartTerritoryState;
import org.junit.jupiter.api.Test;

class R01RegalhartTerritoryStateTest {
    @Test
    void repeatNeedsFullTwentyMinutesThenASeparateSixtySecondEmptyWindow() {
        var state = R01RegalhartTerritoryState.initial()
                .recordValidDefeat(1_000L);

        state = state.updatePostEligibilityArenaPresence(
                24_999L,
                false
        );
        assertEquals(-1L, state.arenaEmptySinceActiveTicks());
        assertFalse(state.repeatEligible(24_999L, false));

        state = state.updatePostEligibilityArenaPresence(
                25_000L,
                false
        );
        assertEquals(25_000L, state.arenaEmptySinceActiveTicks());
        assertFalse(state.repeatEligible(26_199L, false));
        assertTrue(state.repeatEligible(26_200L, false));
        assertFalse(state.repeatEligible(26_200L, true));
    }

    @Test
    void returningToArenaRestartsOnlyThePostEligibilityEmptyWindow() {
        var state = R01RegalhartTerritoryState.initial()
                .recordValidDefeat(0L)
                .updatePostEligibilityArenaPresence(
                        R01RegalhartTerritoryRules
                                .REPEAT_DELAY_ACTIVE_TICKS,
                        false
                );

        state = state.updatePostEligibilityArenaPresence(
                24_700L,
                true
        );
        assertEquals(-1L, state.arenaEmptySinceActiveTicks());

        state = state.updatePostEligibilityArenaPresence(
                24_800L,
                false
        );
        assertFalse(state.repeatEligible(25_999L, false));
        assertTrue(state.repeatEligible(26_000L, false));
    }

    @Test
    void disengageFiresAtExactlyTwentyFiveSecondsAndPresenceCancelsIt() {
        var state = R01RegalhartTerritoryState.initial()
                .updateEngagementPresence(
                        10_000L,
                        true,
                        false
                );

        assertFalse(state.shouldDisengage(10_499L, true));
        assertTrue(state.shouldDisengage(10_500L, true));

        state = state.updateEngagementPresence(
                10_500L,
                true,
                true
        );
        assertEquals(-1L, state.engagementEmptySinceActiveTicks());
        assertFalse(state.shouldDisengage(20_000L, true));
    }

    @Test
    void newValidDefeatResetsBothPendingEmptyWindows() {
        var state = R01RegalhartTerritoryState.initial()
                .recordValidDefeat(0L)
                .updatePostEligibilityArenaPresence(
                        24_000L,
                        false
                )
                .updateEngagementPresence(
                        24_000L,
                        true,
                        false
                )
                .recordValidDefeat(30_000L);

        assertEquals(30_000L, state.lastDefeatActiveTicks());
        assertEquals(-1L, state.arenaEmptySinceActiveTicks());
        assertEquals(-1L, state.engagementEmptySinceActiveTicks());
    }

    @Test
    void territoryTimingStateSurvivesCodecRoundTrip() {
        var original = R01RegalhartTerritoryState.initial()
                .recordValidDefeat(2_000L)
                .updatePostEligibilityArenaPresence(
                        26_000L,
                        false
                )
                .updateEngagementPresence(
                        26_000L,
                        true,
                        false
                );

        var encoded = R01RegalhartTerritoryState.CODEC
                .encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow();
        var decoded = R01RegalhartTerritoryState.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();

        assertEquals(original, decoded);
    }
}
