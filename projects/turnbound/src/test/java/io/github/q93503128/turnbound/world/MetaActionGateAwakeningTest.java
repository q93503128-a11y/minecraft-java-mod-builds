package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetaActionGateAwakeningTest {
    @Test
    void materialAwakeningCannotBypassTheCanonGap() {
        UUID playerId = UUID.randomUUID();
        for (int i = 1; i <= 4; i++) {
            String denial = MetaActionGate.denial(playerId, "AWAKEN|F0" + i);
            assertTrue(denial.startsWith("CANON GAP"), denial);
        }
    }

    @Test
    void authoredHeroAwakeningContinuesToTheNormalServerValidation() {
        assertEquals("", MetaActionGate.denial(UUID.randomUUID(), "AWAKEN|P01"));
    }
}
