package dev.moonseungjun.openworldrpg.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.progression.r01.R01PlayerState;
import org.junit.jupiter.api.Test;

class R01RoadsidePersonalStateTest {
    @Test
    void participationTracksOnlyLatestCycleWithoutGrowingUnboundedHistory() {
        var state = R01PlayerState.initial()
                .markRoadsideEventParticipation(1L, 10L)
                .markRoadsideEventEnded(1L, 5_000L, 11L)
                .markRoadsideEventParticipation(2L, 12L);

        var repeat = state.worldLoops().repeatAndCamp();

        assertEquals(2L, repeat.roadsideEventCycle());
        assertTrue(repeat.participatedInRoadsideCycle(2L));
        assertEquals(1, repeat.roadsideEventParticipation().size());
        assertEquals(5_000L, repeat.roadsideEventLastEndActiveTime());
    }
}
