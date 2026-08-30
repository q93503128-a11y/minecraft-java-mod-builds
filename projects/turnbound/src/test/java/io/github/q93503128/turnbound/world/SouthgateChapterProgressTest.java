package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.SouthgateEncounterCatalog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SouthgateChapterProgressTest {
    @Test
    void bossUnlocksOnlyAfterAllFivePatrolFirstClearsAndRewardsAreIdempotent() {
        SouthgateChapterProgress progress = new SouthgateChapterProgress();
        assertFalse(progress.bossUnlocked());
        var first = progress.recordVictory(SouthgateEncounterCatalog.ENC_M01);
        assertTrue(first.firstClear());
        int xp = progress.earnedXp();
        int gold = progress.earnedGold();
        var duplicate = progress.recordVictory(SouthgateEncounterCatalog.ENC_M01);
        assertFalse(duplicate.firstClear());
        assertEquals(xp, progress.earnedXp());
        assertEquals(gold, progress.earnedGold());
        for (String id : SouthgateEncounterCatalog.normalEncounterIds().subList(1, 5)) progress.recordVictory(id);
        assertTrue(progress.bossUnlocked());
        assertFalse(progress.chapterCleared());
        progress.recordVictory(SouthgateEncounterCatalog.B01_GRAUL);
        assertTrue(progress.chapterCleared());
    }

    @Test
    void lockedBossCannotBeClearedEarly() {
        SouthgateChapterProgress progress = new SouthgateChapterProgress();
        assertThrows(IllegalStateException.class, () -> progress.recordVictory(SouthgateEncounterCatalog.B01_GRAUL));
    }
}
