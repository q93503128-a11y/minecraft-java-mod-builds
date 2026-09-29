package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GachaPresentationTimelineTest {
    @Test
    void revealBeatOrderIsStable() {
        assertEquals(GachaPresentationTimeline.Phase.SIGNAL, GachaPresentationTimeline.phase(0));
        assertEquals(GachaPresentationTimeline.Phase.SIGNAL, GachaPresentationTimeline.phase(7));
        assertEquals(GachaPresentationTimeline.Phase.SILHOUETTE, GachaPresentationTimeline.phase(8));
        assertEquals(GachaPresentationTimeline.Phase.REVEAL, GachaPresentationTimeline.phase(16));
        assertEquals(GachaPresentationTimeline.Phase.NAME, GachaPresentationTimeline.phase(23));
        assertEquals(GachaPresentationTimeline.Phase.COMPLETE, GachaPresentationTimeline.phase(40));
    }

    @Test
    void rarityChangesIntensityNotRevealLength() {
        assertEquals(0, GachaPresentationTimeline.intensity(1));
        assertEquals(1, GachaPresentationTimeline.intensity(2));
        assertEquals(2, GachaPresentationTimeline.intensity(3));
        assertEquals(3, GachaPresentationTimeline.intensity(4));
        assertEquals(4, GachaPresentationTimeline.intensity(5));
        assertEquals(40, GachaPresentationTimeline.SLOT_TICKS);
    }

    @Test
    void tenPullSummaryGetsLongerReadingWindow() {
        assertEquals(78, GachaPresentationTimeline.totalTicks(1, 1));
        assertEquals(166, GachaPresentationTimeline.totalTicks(2, 10));
    }
}
