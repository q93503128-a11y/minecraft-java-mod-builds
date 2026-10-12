package io.github.q93503128.turnbound.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JourneyMapLabelScaleTest {
    @Test
    void labelsGrowWithFullscreenZoomInsteadOfShrinkingToTinyText() {
        float zoom1024 = TurnboundJourneyMapPlugin.labelScaleForZoom(1024);
        float zoom2048 = TurnboundJourneyMapPlugin.labelScaleForZoom(2048);
        float zoom4096 = TurnboundJourneyMapPlugin.labelScaleForZoom(4096);

        assertEquals(0.90F, zoom1024, 0.001F);
        assertEquals(1.15F, zoom2048, 0.001F);
        assertEquals(2.30F, zoom4096, 0.001F);
        assertTrue(zoom4096 > zoom2048);
        assertTrue(zoom2048 > zoom1024);
    }

    @Test
    void extremeZoomIsCappedBeforeLabelsBecomeMapSized() {
        assertEquals(3.00F, TurnboundJourneyMapPlugin.labelScaleForZoom(16384), 0.001F);
    }
}
