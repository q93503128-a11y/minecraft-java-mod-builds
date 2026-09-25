package dev.moonseungjun.openworldrpg.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

class PlayerActiveWorldTimeStateTest {
    @Test
    void activeTimePersistsAcrossEpochsAndEpochDoesNotReroll() {
        var state = PlayerActiveWorldTimeState.initial()
                .advance(2_400)
                .markEpochOnce(PlayerActiveWorldTimeService.ALDERFORD_SHRINE_EPOCH)
                .advance(4_800);

        assertEquals(7_200L, state.activeTicks());
        assertEquals(
                4_800L,
                state.elapsedSinceEpoch(PlayerActiveWorldTimeService.ALDERFORD_SHRINE_EPOCH)
        );

        var retried = state.markEpochOnce(
                PlayerActiveWorldTimeService.ALDERFORD_SHRINE_EPOCH
        );
        assertSame(state, retried);
    }

    @Test
    void stateSurvivesCodecRoundTrip() {
        var original = PlayerActiveWorldTimeState.initial()
                .advance(1_200)
                .markEpochOnce("openworld_rpg:test_epoch")
                .advance(600);

        var encoded = PlayerActiveWorldTimeState.CODEC
                .encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow();
        var decoded = PlayerActiveWorldTimeState.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();

        assertEquals(original, decoded);
    }

    @Test
    void epochCannotPointBeyondActiveTime() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PlayerActiveWorldTimeState(
                        1,
                        20,
                        java.util.Map.of("openworld_rpg:test_epoch", 21L)
                )
        );
    }
}
