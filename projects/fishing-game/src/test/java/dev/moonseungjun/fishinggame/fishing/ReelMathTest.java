package dev.moonseungjun.fishinggame.fishing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ReelMathTest {
    @Test
    void safeTensionAdvancesProgress() {
        float next = ReelMath.updateProgress(0.25f, 0.50f, 1.0f);
        assertTrue(next > 0.25f);
    }

    @Test
    void slackLineLosesProgress() {
        float next = ReelMath.updateProgress(0.25f, 0.10f, 1.0f);
        assertTrue(next < 0.25f);
    }

    @Test
    void repeatedReelPressureCanBreakLine() {
        float tension = 0.70f;
        for (int i = 0; i < 8; i++) {
            tension = ReelMath.updateTension(tension, true, 1.25f);
        }
        assertTrue(ReelMath.isLineBroken(tension));
    }

    @Test
    void progressIsClamped() {
        assertEquals(1.0f, ReelMath.updateProgress(0.999f, 0.50f, 0.65f));
        assertEquals(0.0f, ReelMath.updateProgress(0.001f, 0.05f, 2.0f));
    }
}
