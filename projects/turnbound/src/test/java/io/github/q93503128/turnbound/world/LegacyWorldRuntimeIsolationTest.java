package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyWorldRuntimeIsolationTest {
    @Test
    void legacyWritersRequireAnActualLegacySession() {
        assertFalse(LegacyWorldRuntimeIsolation.allowed(false, false));
        assertTrue(LegacyWorldRuntimeIsolation.allowed(false, true));
    }

    @Test
    void externalWorldAlwaysWinsIfStatesEverOverlap() {
        assertFalse(LegacyWorldRuntimeIsolation.allowed(true, false));
        assertFalse(LegacyWorldRuntimeIsolation.allowed(true, true));
    }
}
