package dev.moonseungjun.openworldrpg.recovery;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RecoveryUseActionStateTest {
    @Test
    void resolutionAndRecoveryFramesMatchCanonicalTicks() {
        var action = new RecoveryUseActionState(
                2,
                RecoveryConsumable.HEALING_POTION,
                100L,
                false
        );

        assertTrue(action.isPreResolution(113L));
        assertFalse(action.isPreResolution(114L));
        assertTrue(action.shouldResolve(114L));
        assertFalse(action.isComplete(118L));
        assertTrue(action.isComplete(119L));

        var resolved = action.markResolved(114L);
        assertTrue(resolved.resolved());
        assertFalse(resolved.isPreResolution(114L));
    }
}
