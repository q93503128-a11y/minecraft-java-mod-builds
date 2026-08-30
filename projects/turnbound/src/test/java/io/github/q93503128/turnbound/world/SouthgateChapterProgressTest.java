package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.SouthgateEncounterCatalog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SouthgateChapterProgressTest {
    @Test
    void bossUnlocksAfterCanonicalMainQuestStagesNotAllOptionalEncounters() {
        SouthgateChapterProgress progress = new SouthgateChapterProgress();
        progress.recordVictory(SouthgateEncounterCatalog.ENC_M01);
        assertFalse(progress.meadowRouteUnlocked());
        progress.recordVictory(SouthgateEncounterCatalog.ENC_M02);
        assertTrue(progress.meadowRouteUnlocked());
        assertFalse(progress.bossUnlocked());
        progress.recordVictory(SouthgateEncounterCatalog.ENC_M04);
        assertTrue(progress.bossUnlocked());
        assertFalse(progress.cleared(SouthgateEncounterCatalog.ENC_M03));
        assertFalse(progress.cleared(SouthgateEncounterCatalog.ENC_M05));
    }

    @Test
    void graulFirstClearAwardsCanonicalChapterOneUnlocksOnce() {
        SouthgateChapterProgress progress = new SouthgateChapterProgress();
        progress.recordVictory(SouthgateEncounterCatalog.ENC_M01);
        progress.recordVictory(SouthgateEncounterCatalog.ENC_M02);
        progress.recordVictory(SouthgateEncounterCatalog.ENC_M04);
        var reward = progress.recordVictory(SouthgateEncounterCatalog.B01_GRAUL);
        assertEquals(5000, reward.xp());
        assertEquals(12000, reward.gold());
        assertEquals(1200, reward.crystal());
        assertEquals(60, reward.starEssence());
        assertTrue(reward.t2ChoiceBox());
        assertTrue(reward.p08Unlocked());
        assertTrue(progress.archiveUnlocked());
        assertTrue(progress.autoUnlocked());
        assertTrue(progress.speedUnlocked());
        assertTrue(progress.chapterCleared());

        var duplicate = progress.recordVictory(SouthgateEncounterCatalog.B01_GRAUL);
        assertFalse(duplicate.firstClear());
        assertEquals(0, duplicate.gold());
        assertEquals(1200, progress.summonCrystal());
        assertEquals(60, progress.starEssence());
        assertEquals(1, progress.t2ChoiceBoxes());
    }

    @Test
    void lockedBossCannotBeClearedEarly() {
        SouthgateChapterProgress progress = new SouthgateChapterProgress();
        assertThrows(IllegalStateException.class, () -> progress.recordVictory(SouthgateEncounterCatalog.B01_GRAUL));
    }
}
