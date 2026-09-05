package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WorldSessionTransitionRulesTest {
    @Test
    void sharedPhysicalGateNeverOverridesPersonalChapterEligibility() {
        assertFalse(WorldSessionRouter.mayCrossSharedSeam(false, false));
        assertFalse(WorldSessionRouter.mayCrossSharedSeam(false, true));
        assertFalse(WorldSessionRouter.mayCrossSharedSeam(true, false));
        assertTrue(WorldSessionRouter.mayCrossSharedSeam(true, true));
    }
}
