package io.github.q93503128.turnbound.client;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BattleControlRulesTest {
    @Test
    void lockedCampaignControlsAreVisibleAsLockedAndInactive() {
        var snapshot = snapshot(false, 1, false, false, false, false);
        var state = BattleControlRules.state(snapshot);
        assertEquals("자동 잠금", state.autoLabel());
        assertFalse(state.autoActive());
        assertEquals("×2 잠금", state.speedLabel());
        assertFalse(state.speedActive());
        assertEquals("도주 불가", state.fleeLabel());
        assertFalse(state.fleeActive());
    }

    @Test
    void unlockedFieldControlsAndFinishedReturnAreDistinct() {
        var running = BattleControlRules.state(snapshot(true, 2, true, true, true, false));
        assertEquals("자동✓", running.autoLabel());
        assertTrue(running.autoActive());
        assertEquals("×2", running.speedLabel());
        assertTrue(running.speedActive());
        assertEquals("도주", running.fleeLabel());
        assertTrue(running.fleeActive());

        var finished = BattleControlRules.state(snapshot(false, 1, false, false, false, true));
        assertEquals("복귀", finished.fleeLabel());
        assertEquals("자동", finished.autoLabel());
        assertTrue(finished.fleeActive());
        assertFalse(finished.autoActive());
        assertFalse(finished.speedActive());
    }

    private static ClientBattleState.Snapshot snapshot(boolean auto, int speed, boolean autoAllowed,
                                                       boolean speedAllowed, boolean fleeAllowed, boolean finished) {
        return new ClientBattleState.Snapshot(true, auto, speed, finished ? "ALLY_VICTORY" : "RUNNING",
                "ally_1", finished, autoAllowed, speedAllowed, fleeAllowed,
                List.of(), List.of(), List.of(), "", 0.0, 0.0, 0.0, 0.0F);
    }
}
