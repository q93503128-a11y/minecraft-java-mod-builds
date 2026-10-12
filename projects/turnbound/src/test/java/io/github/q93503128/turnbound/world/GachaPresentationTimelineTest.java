package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GachaPresentationTimelineTest {
    @Test
    void fiveStarRevealBeatOrderHasRealAnticipationAndHold() {
        assertEquals(GachaPresentationTimeline.Phase.SIGNAL, GachaPresentationTimeline.phase(0,5,false));
        assertEquals(GachaPresentationTimeline.Phase.SIGNAL, GachaPresentationTimeline.phase(13,5,false));
        assertEquals(GachaPresentationTimeline.Phase.SILHOUETTE, GachaPresentationTimeline.phase(14,5,false));
        assertEquals(GachaPresentationTimeline.Phase.REVEAL, GachaPresentationTimeline.phase(28,5,false));
        assertEquals(GachaPresentationTimeline.Phase.NAME, GachaPresentationTimeline.phase(40,5,false));
        assertEquals(GachaPresentationTimeline.Phase.COMPLETE, GachaPresentationTimeline.phase(64,5,false));
    }

    @Test
    void rarityChangesBothIntensityAndRevealLengthWithoutMakingLowRaritySlow() {
        assertEquals(0, GachaPresentationTimeline.intensity(1));
        assertEquals(1, GachaPresentationTimeline.intensity(2));
        assertEquals(2, GachaPresentationTimeline.intensity(3));
        assertEquals(3, GachaPresentationTimeline.intensity(4));
        assertEquals(4, GachaPresentationTimeline.intensity(5));

        assertEquals(24, GachaPresentationTimeline.slotTicks(1,false));
        assertEquals(28, GachaPresentationTimeline.slotTicks(2,false));
        assertEquals(33, GachaPresentationTimeline.slotTicks(3,false));
        assertEquals(46, GachaPresentationTimeline.slotTicks(4,false));
        assertEquals(64, GachaPresentationTimeline.slotTicks(5,false));
        assertTrue(GachaPresentationTimeline.slotTicks(5,false) > GachaPresentationTimeline.slotTicks(4,false));
        assertTrue(GachaPresentationTimeline.slotTicks(4,false) > GachaPresentationTimeline.slotTicks(2,false));
    }

    @Test
    void newlyOwnedResultGetsAnExtraReadableNameHold() {
        assertEquals(4,
                GachaPresentationTimeline.slotTicks(5,true) - GachaPresentationTimeline.slotTicks(5,false));
        assertEquals(2,
                GachaPresentationTimeline.slotTicks(1,true) - GachaPresentationTimeline.slotTicks(1,false));
    }

    @Test
    void totalDurationSumsEveryRevealThenUsesBatchSummaryWindow() {
        var reveals = List.of(
                new GachaPresentationPlan.Reveal("P01",4,false),
                new GachaPresentationPlan.Reveal("P06",5,true));
        int revealTicks = GachaPresentationTimeline.slotTicks(4,false)
                + GachaPresentationTimeline.slotTicks(5,true);
        assertEquals(revealTicks, GachaPresentationTimeline.revealTicks(reveals));
        assertEquals(revealTicks + 86, GachaPresentationTimeline.totalTicks(reveals,10));
        assertEquals(38, GachaPresentationTimeline.summaryTicks(1));
        assertEquals(86, GachaPresentationTimeline.summaryTicks(10));
    }
}
