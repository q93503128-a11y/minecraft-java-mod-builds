package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.BattleOutcome;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Chapter3ProgressionTest {
    private final UUID playerId = UUID.randomUUID();

    @AfterEach
    void cleanup() { CampaignProgressStore.removeRuntime(playerId); }

    @Test
    void aqueductValvesEliteAndOro7CompleteChapterThreeInCanonicalOrder() {
        CampaignProgressStore.ensureNewGame(playerId);
        CampaignProgressStore.questInteract(playerId, "Director Iven");
        CampaignProgressStore.setActiveParty(playerId, List.of("P01", "P03", "P04", "F03"));
        for (String id : List.of("TUTORIAL_1", "TUTORIAL_2", "TUTORIAL_3")) CampaignProgressStore.commit(playerId, id, BattleOutcome.ALLY_VICTORY);
        CampaignProgressStore.commit(playerId, "ENC_M01", BattleOutcome.ALLY_VICTORY);
        CampaignProgressStore.commit(playerId, "ENC_M02", BattleOutcome.ALLY_VICTORY);
        CampaignProgressStore.commit(playerId, "ENC_M04", BattleOutcome.ALLY_VICTORY);
        CampaignProgressStore.commit(playerId, "BATTLE_B01", BattleOutcome.ALLY_VICTORY);
        for (int i = 0; i < 3; i++) CampaignProgressStore.questInteract(playerId, "SPORE_LANTERN");
        CampaignProgressStore.commit(playerId, "ENC_G02", BattleOutcome.ALLY_VICTORY);
        CampaignProgressStore.commit(playerId, "ENC_G05", BattleOutcome.ALLY_VICTORY);
        CampaignProgressStore.commit(playerId, "BATTLE_B02", BattleOutcome.ALLY_VICTORY);
        assertTrue(CampaignProgressStore.quests(playerId).completed().contains("MQ_C02_03_verna"));

        CampaignProgressStore.questInteract(playerId, "AQUEDUCT_VALVE");
        assertFalse(CampaignProgressStore.quests(playerId).unlockFlags().contains("AQUEDUCT_LOWER"));
        CampaignProgressStore.questInteract(playerId, "AQUEDUCT_VALVE");
        assertTrue(CampaignProgressStore.quests(playerId).completed().contains("MQ_C03_01_dry_channel"));
        assertTrue(CampaignProgressStore.quests(playerId).unlockFlags().contains("AQUEDUCT_LOWER"));
        assertFalse(CampaignProgressStore.quests(playerId).unlockFlags().contains("ORO_ROOM"));

        CampaignProgressStore.commit(playerId, "ENC_A04", BattleOutcome.ALLY_VICTORY);
        assertTrue(CampaignProgressStore.quests(playerId).completed().contains("MQ_C03_02_old_orders"));
        assertTrue(CampaignProgressStore.quests(playerId).unlockFlags().contains("ORO_ROOM"));

        CampaignProgressStore.commit(playerId, "BATTLE_B03", BattleOutcome.ALLY_VICTORY);
        assertTrue(CampaignProgressStore.quests(playerId).completed().contains("MQ_C03_03_oro7"));
        assertTrue(CampaignProgressStore.quests(playerId).unlockFlags().contains("T3_DETAIL"));
        assertTrue(CampaignProgressStore.quests(playerId).unlockFlags().contains("CODEX_DETAIL"));
    }
}
