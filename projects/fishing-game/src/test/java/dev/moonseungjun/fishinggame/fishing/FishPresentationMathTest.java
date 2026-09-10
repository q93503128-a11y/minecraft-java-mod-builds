package dev.moonseungjun.fishinggame.fishing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FishPresentationMathTest {
    @Test
    void largerFishRenderLargerButStayBounded() {
        float small = FishPresentationMath.scaleForLength(20.0);
        float large = FishPresentationMath.scaleForLength(95.0);
        float huge = FishPresentationMath.scaleForLength(500.0);

        assertTrue(small < large);
        assertTrue(large < huge);
        assertEquals(1.85f, huge);
    }

    @Test
    void approachConvergesOnHook() {
        double start = FishPresentationMath.approachRadius(0.0, 5.4, 0.55);
        double middle = FishPresentationMath.approachRadius(0.5, 5.4, 0.55);
        double end = FishPresentationMath.approachRadius(1.0, 5.4, 0.55);

        assertEquals(5.4, start, 0.0001);
        assertTrue(middle < start);
        assertEquals(0.55, end, 0.0001);
    }

    @Test
    void burstExtendsFightRadiusAndPullsLine() {
        double calm = FishPresentationMath.fightRadius(0.25f, 1.1f, 0.0f, 0.55f);
        double burst = FishPresentationMath.fightRadius(0.25f, 1.1f, 1.0f, 0.55f);
        float pulled = FishPresentationMath.burstPull(0.55f, 1.2f, 1.0f, 1.0f);

        assertTrue(burst > calm);
        assertTrue(pulled > 0.55f);
        assertTrue(pulled < ReelMath.BREAK_TENSION);
    }
}
