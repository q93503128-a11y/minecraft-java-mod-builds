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
    void characterQuestMenuUsesAuthoredChapterAndOwnershipGatesWithoutLeakingIdentifiers() {
        CampaignProgressStore.ensureNewGame(playerId);
        CampaignProgressStore.commit(playerId, "TUTORIAL_1", BattleOutcome.ALLY_VICTORY);
        CampaignProgressStore.commit(playerId, "TUTORIAL_2", BattleOutcome.ALLY_VICTORY);
        assertTrue(CampaignProgressStore.ownedCharacters(playerId).contains("P03"));
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
        assertTrue(wire.contains("인연 · 카이렌"));
        assertTrue(wire.contains("인연 · 라제"));
        assertTrue(wire.contains("전용 장비 시험"));
        assertFalse(wire.contains("CQ_P01"));
        assertFalse(wire.contains("CQ_P08"));
        assertFalse(wire.contains("Signature Trial"));
    }
}
