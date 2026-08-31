package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.BattleOutcome;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Chapter2ProgressionTest {
    private final UUID playerId = UUID.randomUUID();

    @AfterEach
    void cleanup() { CampaignProgressStore.removeRuntime(playerId); }

    @Test
    void prologueChapterOneAndGloamwoodQuestChainReachB02Unlocks() {
        CampaignProgressStore.ensureNewGame(playerId);
        CampaignProgressStore.questInteract(playerId, "Director Iven");
        CampaignProgressStore.setActiveParty(playerId, List.of("P01", "P03", "P04", "F03"));
        for (String id : List.of("TUTORIAL_1", "TUTORIAL_2", "TUTORIAL_3")) {
            CampaignProgressStore.commit(playerId, id, BattleOutcome.ALLY_VICTORY);
        }
        assertTrue(CampaignProgressStore.quests(playerId).completed().contains("MQ_P00_03_south_gate"));

        CampaignProgressStore.commit(playerId, "ENC_M01", BattleOutcome.ALLY_VICTORY);
        CampaignProgressStore.commit(playerId, "ENC_M02", BattleOutcome.ALLY_VICTORY);
        CampaignProgressStore.commit(playerId, "ENC_M04", BattleOutcome.ALLY_VICTORY);
        CampaignProgressStore.commit(playerId, "BATTLE_B01", BattleOutcome.ALLY_VICTORY);
        assertTrue(CampaignProgressStore.quests(playerId).completed().contains("MQ_C01_03_graul"));
        assertFalse(CampaignProgressStore.quests(playerId).unlockFlags().contains("GLOAM_DEEP_PATH"));

        CampaignProgressStore.questInteract(playerId, "SPORE_LANTERN");
        CampaignProgressStore.questInteract(playerId, "SPORE_LANTERN");
        CampaignProgressStore.questInteract(playerId, "SPORE_LANTERN");
        assertTrue(CampaignProgressStore.quests(playerId).completed().contains("MQ_C02_01_spores"));
        assertTrue(CampaignProgressStore.quests(playerId).unlockFlags().contains("GLOAM_DEEP_PATH"));
        assertFalse(CampaignProgressStore.quests(playerId).unlockFlags().contains("B02_GATE"));

        CampaignProgressStore.commit(playerId, "ENC_G02", BattleOutcome.ALLY_VICTORY);
        CampaignProgressStore.commit(playerId, "ENC_G05", BattleOutcome.ALLY_VICTORY);
        assertTrue(CampaignProgressStore.quests(playerId).completed().contains("MQ_C02_02_root_wall"));
        assertTrue(CampaignProgressStore.quests(playerId).unlockFlags().contains("B02_GATE"));

        CampaignProgressStore.commit(playerId, "BATTLE_B02", BattleOutcome.ALLY_VICTORY);
        assertTrue(CampaignProgressStore.quests(playerId).completed().contains("MQ_C02_03_verna"));
        assertTrue(CampaignProgressStore.quests(playerId).unlockFlags().contains("ACCESSORY_SLOT"));
        assertTrue(CampaignProgressStore.quests(playerId).unlockFlags().contains("ELITE_ENCOUNTERS"));
    }
}
