package kr.moonseungjun.riftfrontier.expedition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Region01SalvageEventRuntimeTest {
    @Test
    void riftBlackoutTriggersOnlyAtSecondRecoveredSalvage() {
        assertFalse(Region01SalvageEventRuntime.isDue(0));
        assertFalse(Region01SalvageEventRuntime.isDue(1));
        assertTrue(Region01SalvageEventRuntime.isDue(2));
        assertFalse(Region01SalvageEventRuntime.isDue(3));
        assertFalse(Region01SalvageEventRuntime.isDue(4));
    }
}
