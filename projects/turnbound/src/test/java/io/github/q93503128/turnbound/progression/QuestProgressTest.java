package io.github.q93503128.turnbound.progression;

import io.github.q93503128.turnbound.content.QuestCatalog;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestProgressTest {
    @Test
    void canonicalMainObjectivesAdvanceFromServerEvents() {
        QuestProgress progress = QuestProgress.empty();

        var arrival = QuestCatalog.quest("MQ_P00_01_arrival");
        assertTrue(progress.apply(arrival, QuestProgress.Event.interact("Director Iven")));
        assertTrue(progress.satisfied(arrival));
        progress.complete(arrival);

        var party = QuestCatalog.quest("MQ_P00_02_first_party");
        assertTrue(progress.apply(party, QuestProgress.Event.partyConfirm(Set.of("P01", "P03", "P04", "F03"))));
        assertTrue(progress.satisfied(party));
    }

    @Test
    void battleWinListsAndBattleWithCountersUseDifferentSemantics() {
        QuestProgress progress = QuestProgress.empty();
        var patrol = QuestCatalog.quest("MQ_C01_01_patrol");
        progress.apply(patrol, QuestProgress.Event.battleWin("ENC_M01", Set.of("E001")));
        assertFalse(progress.satisfied(patrol));
        progress.apply(patrol, QuestProgress.Event.battleWin("ENC_M02", Set.of("E001", "E002")));
        assertTrue(progress.satisfied(patrol));

        var rootWall = QuestCatalog.quest("MQ_C02_02_root_wall");
        progress.apply(rootWall, QuestProgress.Event.battleWin("ENC_G02", Set.of("E008", "E007")));
        assertFalse(progress.satisfied(rootWall));
        progress.apply(rootWall, QuestProgress.Event.battleWin("ENC_G05", Set.of("E008", "E002", "E007")));
        assertTrue(progress.satisfied(rootWall));
    }

    @Test
    void killAndLootRequiresCanonicalCountForBothTargets() {
        QuestProgress progress = QuestProgress.empty();
        var quest = QuestCatalog.quest("MQ_C04_02_core_fragment");
        progress.apply(quest, QuestProgress.Event.kill("E014", 2));
        assertFalse(progress.satisfied(quest));
        progress.apply(quest, QuestProgress.Event.loot("CORE_FRAGMENT", 1));
        assertFalse(progress.satisfied(quest));
        progress.apply(quest, QuestProgress.Event.loot("CORE_FRAGMENT", 1));
        assertTrue(progress.satisfied(quest));
    }

    @Test
    void trackerHonorsCanonicalThreeQuestLimit() {
        QuestProgress progress = QuestProgress.empty();
        progress.track("RQ_M01_broken_cart");
        progress.track("RQ_M02_missing_scout");
        progress.track("RQ_M03_fuse_nest");
        assertThrows(IllegalStateException.class, () -> progress.track("RQ_G01_lost_lantern"));
    }
}
