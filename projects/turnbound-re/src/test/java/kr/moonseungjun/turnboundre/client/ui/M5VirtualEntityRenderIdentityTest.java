package kr.moonseungjun.turnboundre.client.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class M5VirtualEntityRenderIdentityTest {
    @Test
    void syntheticRenderIdsStayNegativeAndUnique() {
        int first = VirtualEntityRenderIdentity.nextSyntheticId();
        int second = VirtualEntityRenderIdentity.nextSyntheticId();

        assertTrue(first < 0);
        assertTrue(second < 0);
        assertNotEquals(first, second);
    }

    @Test
    void unifiedMenuHitTestingUsesExactFrameBounds() {
        UiLayoutMetrics.Rect rect = new UiLayoutMetrics.Rect(10, 20, 30, 40);

        assertTrue(TurnboundMenuScreen.contains(rect, 10, 20));
        assertTrue(TurnboundMenuScreen.contains(rect, 39, 59));
        assertTrue(!TurnboundMenuScreen.contains(rect, 40, 59));
        assertTrue(!TurnboundMenuScreen.contains(rect, 39, 60));
    }
}
