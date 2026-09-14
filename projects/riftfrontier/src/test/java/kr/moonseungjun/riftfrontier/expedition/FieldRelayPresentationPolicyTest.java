package kr.moonseungjun.riftfrontier.expedition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FieldRelayPresentationPolicyTest {
    @Test
    void relayBecomesReadyOnlyAtAuthoritativeSalvageThreshold() {
        assertFalse(FieldRelayPresentationPolicy.extractionReady(0, 3));
        assertFalse(FieldRelayPresentationPolicy.extractionReady(2, 3));
        assertTrue(FieldRelayPresentationPolicy.extractionReady(3, 3));
        assertTrue(FieldRelayPresentationPolicy.extractionReady(4, 3));
    }

    @Test
    void rejectsInvalidObjectiveThreshold() {
        assertThrows(IllegalArgumentException.class, () -> FieldRelayPresentationPolicy.extractionReady(0, 0));
        assertThrows(IllegalArgumentException.class, () -> FieldRelayPresentationPolicy.extractionReady(0, -1));
    }
}
