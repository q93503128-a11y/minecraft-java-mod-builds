package kr.moonseungjun.riftfrontier.expedition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Region01SalvageEventRuntimeTest {
    @Test
    void riftBlackoutTriggersOnlyAtSecondRecoveredSalvage() {
        assertFalse(Region01SalvageEventRules.isBlackoutDue(0));
        assertFalse(Region01SalvageEventRules.isBlackoutDue(1));
        assertTrue(Region01SalvageEventRules.isBlackoutDue(2));
        assertFalse(Region01SalvageEventRules.isBlackoutDue(3));
        assertFalse(Region01SalvageEventRules.isBlackoutDue(4));
    }
}
