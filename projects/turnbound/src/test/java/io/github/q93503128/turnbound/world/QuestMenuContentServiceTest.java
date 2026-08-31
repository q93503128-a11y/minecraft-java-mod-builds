package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.BattleOutcome;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class QuestMenuContentServiceTest {
    private final UUID playerId = UUID.randomUUID();

    @AfterEach
    void cleanup() { CampaignProgressStore.removeRuntime(playerId); }

    @Test
    void characterQuestMenuUsesAuthoredChapterAndOwnershipGatesWithoutInventingObjectives() {
        CampaignProgressStore.ensureNewGame(playerId);
        assertFalse(QuestMenuContentService.available(playerId, "P03"));

        CampaignProgressStore.commit(playerId, "BATTLE_B02", BattleOutcome.ALLY_VICTORY);
        assertTrue(QuestMenuContentService.available(playerId, "P03"));
        assertFalse(QuestMenuContentService.available(playerId, "P01"), "P01 also requires Lv20");
        assertFalse(QuestMenuContentService.available(playerId, "P08"));

        CampaignProgressStore.commit(playerId, "BATTLE_B01", BattleOutcome.ALLY_VICTORY);
        CampaignProgressStore.commit(playerId, "BATTLE_B04", BattleOutcome.ALLY_VICTORY);
        assertTrue(CampaignProgressStore.ownedCharacters(playerId).contains("P08"));
        assertTrue(QuestMenuContentService.available(playerId, "P08"));

        String wire = QuestMenuContentService.encode(playerId);
        assertTrue(wire.contains("CQ_P01|CHARACTER · P01 · 끝까지 남은 길"));
        assertTrue(wire.contains("CQ_P08|CHARACTER · P08 · 불길 속에서 웃는 법"));
        assertTrue(wire.contains("Signature Trial"));
    }
}
