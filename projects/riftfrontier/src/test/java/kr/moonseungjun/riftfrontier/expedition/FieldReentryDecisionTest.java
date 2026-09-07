package kr.moonseungjun.riftfrontier.expedition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FieldReentryDecisionTest {
    @Test
    void strandedTechnicalFieldPlayerReturnsOnlyWhenNoExpeditionIsActive() {
        FieldReentryDecision stranded = FieldReentryDecision.evaluate(false, true);
        assertTrue(stranded.returnToHub());
        assertTrue(stranded.reason().contains("terminal or reconciled"));

        FieldReentryDecision active = FieldReentryDecision.evaluate(true, true);
        assertFalse(active.returnToHub());
        assertEquals("active expedition owns field position", active.reason());
    }

    @Test
    void playersOutsideTechnicalFieldAreNeverMovedByReentryPolicy() {
        FieldReentryDecision inactiveOutside = FieldReentryDecision.evaluate(false, false);
        FieldReentryDecision activeOutside = FieldReentryDecision.evaluate(true, false);

        assertFalse(inactiveOutside.returnToHub());
        assertFalse(activeOutside.returnToHub());
    }
}
