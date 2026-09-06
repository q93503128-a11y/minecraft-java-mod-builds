package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldVanillaMobSanitizerTest {
    @Test
    void regionScanRunsAtMostOncePerTwentyServerTicks() {
        assertTrue(FieldVanillaMobSanitizer.due(null, 100));
        assertFalse(FieldVanillaMobSanitizer.due(100L, 100));
        assertFalse(FieldVanillaMobSanitizer.due(100L, 119));
        assertTrue(FieldVanillaMobSanitizer.due(100L, 120));
        assertTrue(FieldVanillaMobSanitizer.due(100L, 140));
    }

    @Test
    void clockResetAllowsMaintenanceAgain() {
        assertTrue(FieldVanillaMobSanitizer.due(500L, 10));
    }
}
