package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WorldSessionTransitionRulesTest {
    @Test
    void sharedPhysicalGateNeverOverridesPersonalChapterEligibility() {
        assertFalse(SharedWorldSessionRules.mayCrossSharedSeam(false, false));
        assertFalse(SharedWorldSessionRules.mayCrossSharedSeam(false, true));
        assertFalse(SharedWorldSessionRules.mayCrossSharedSeam(true, false));
        assertTrue(SharedWorldSessionRules.mayCrossSharedSeam(true, true));
    }
}
