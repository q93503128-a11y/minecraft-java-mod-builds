package io.github.q93503128.turnbound.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleResultTimingTest {
    @Test
    void finishedBattleOnlyKeepsAShortUiHandoffBeat() {
        int victory = BattleResultTiming.revealTicks("ALLY_VICTORY");
        int defeat = BattleResultTiming.revealTicks("ENEMY_VICTORY");
        assertEquals(4, victory);
        assertEquals(2, defeat);
        assertTrue(victory < 10);
    }
}
