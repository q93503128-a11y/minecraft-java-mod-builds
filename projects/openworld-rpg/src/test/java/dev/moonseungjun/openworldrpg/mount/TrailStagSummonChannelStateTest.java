package dev.moonseungjun.openworldrpg.mount;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TrailStagSummonChannelStateTest {
    @Test
    void summonChannelCompletesAtExactlyTwentyTicks() {
        var channel = new TrailStagSummonChannelState(7L, 100L, 10L);

        assertEquals(20L, channel.remainingTicks(100L));
        assertEquals(1L, channel.remainingTicks(119L));
        assertEquals(0L, channel.remainingTicks(120L));
        assertFalse(channel.complete(119L));
        assertTrue(channel.complete(120L));
    }

    @Test
    void onlyNewHostileHpActivityCancelsTheChannel() {
        var channel = new TrailStagSummonChannelState(3L, 200L, 120L);

        assertFalse(channel.canceledByHostileActivity(120L));
        assertTrue(channel.canceledByHostileActivity(200L));
        assertTrue(channel.canceledByHostileActivity(201L));
    }

    @Test
    void hostileQuietAgePreservesTheExactFourSecondGate() {
        assertEquals(
                Long.MAX_VALUE,
                TrailStagMountIntentRuntime.ticksSinceHostileActivity(
                        500L,
                        Long.MIN_VALUE / 4L
                )
        );
        assertEquals(
                TrailStagMountRules.HOSTILE_COMBAT_QUIET_TICKS,
                TrailStagMountIntentRuntime.ticksSinceHostileActivity(500L, 420L)
        );
        assertEquals(
                TrailStagMountRules.HOSTILE_COMBAT_QUIET_TICKS - 1L,
                TrailStagMountIntentRuntime.ticksSinceHostileActivity(500L, 421L)
        );
    }
}
