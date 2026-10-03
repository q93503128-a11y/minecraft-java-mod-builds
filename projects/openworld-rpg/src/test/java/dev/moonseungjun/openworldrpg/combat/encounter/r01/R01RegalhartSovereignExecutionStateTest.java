package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class R01RegalhartSovereignExecutionStateTest {
    @Test
    void transitionDamageReductionEndsExactlyWhenSovereignMovementBegins() {
        var data = R01RegalhartEncounterDataLoader.load();
        var state = new R01RegalhartSovereignExecutionState(
                data.sovereign()
        );
        var start =
                R01RegalhartActionController.Decision
                        .sovereignTransitionStart(7L, 130L);

        assertTrue(state.begin(start, 100L));
        assertTrue(state.begun());
        assertEquals(130L, state.transitionEndTick());

        assertEquals(1.0, state.incomingDamageMultiplier(99L), 0.000001);
        assertEquals(0.50, state.incomingDamageMultiplier(100L), 0.000001);
        assertEquals(0.50, state.incomingDamageMultiplier(129L), 0.000001);
        assertEquals(1.0, state.incomingDamageMultiplier(130L), 0.000001);

        assertEquals(1.0, state.movementSpeedMultiplier(100L), 0.000001);
        assertEquals(1.0, state.movementSpeedMultiplier(129L), 0.000001);
        assertEquals(1.10, state.movementSpeedMultiplier(130L), 0.000001);
        assertEquals(1.10, state.movementSpeedMultiplier(500L), 0.000001);
    }

    @Test
    void onlyExactTransitionStartDecisionCanOpenTheOneTimeState() {
        var data = R01RegalhartEncounterDataLoader.load();
        var state = new R01RegalhartSovereignExecutionState(
                data.sovereign()
        );

        assertFalse(state.begin(
                R01RegalhartActionController.Decision
                        .sovereignTransitionHold(3L, 130L),
                100L
        ));
        assertFalse(state.begin(
                R01RegalhartActionController.Decision
                        .sovereignTransitionStart(3L, 129L),
                100L
        ));

        assertTrue(state.begin(
                R01RegalhartActionController.Decision
                        .sovereignTransitionStart(3L, 130L),
                100L
        ));
        assertFalse(state.begin(
                R01RegalhartActionController.Decision
                        .sovereignTransitionStart(4L, 160L),
                130L
        ));
    }
}
