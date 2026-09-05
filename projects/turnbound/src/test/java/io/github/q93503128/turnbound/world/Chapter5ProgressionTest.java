package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.BattleOutcome;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Chapter5ProgressionTest {
    private final UUID playerId = UUID.randomUUID();

    @AfterEach
    void cleanup(){CampaignProgressStore.removeRuntime(playerId);}

    @Test
    void relayFragmentsRecordsSerakAndConsoleCompleteMainStory(){
        reachAfterKolvak();
        assertEquals(Set.of(RelayFragmentBridgeService.MEADOW,RelayFragmentBridgeService.AQUEDUCT,RelayFragmentBridgeService.QUARRY),RelayFragmentBridgeService.availableForId(playerId));
        assertEquals(3,RelayFragmentBridgeService.submitAvailableForId(playerId));
        assertTrue(CampaignProgressStore.quests(playerId).completed().contains("MQ_C05_01_relay_key"));
        assertTrue(CampaignProgressStore.quests(playerId).unlockFlags().contains("OLD_RELAY_ENTRANCE"));

        for(int i=0;i<3;i++)CampaignProgressStore.questInteract(playerId,"SERAK_RECORD");
        assertFalse(CampaignProgressStore.quests(playerId).unlockFlags().contains("B05_GATE"));
        CampaignProgressStore.questInteract(playerId,"SERAK_RECORD");
        assertTrue(CampaignProgressStore.quests(playerId).completed().contains("MQ_C05_02_serak_record"));
        assertTrue(CampaignProgressStore.quests(playerId).unlockFlags().contains("B05_GATE"));

        CampaignProgressStore.commit(playerId,"BATTLE_B05",BattleOutcome.ALLY_VICTORY);
        assertFalse(CampaignProgressStore.quests(playerId).completed().contains("MQ_C05_03_reconnect"));
        CampaignProgressStore.questInteract(playerId,"RELAY_CONSOLE");
        assertTrue(CampaignProgressStore.quests(playerId).completed().contains("MQ_C05_03_reconnect"));
        for(String flag:List.of("ENDGAME","RIFT_GATE","HARD_BOSSES","SIGNATURE_TRIALS"))assertTrue(CampaignProgressStore.quests(playerId).unlockFlags().contains(flag));
    }

    private void reachAfterKolvak(){
        CampaignProgressStore.ensureNewGame(playerId);
        CampaignProgressStore.questInteract(playerId,"Director Iven");
        CampaignProgressStore.setActiveParty(playerId,List.of("P01","F03"));
        for(String id:List.of("TUTORIAL_1","TUTORIAL_2","TUTORIAL_3"))CampaignProgressStore.commit(playerId,id,BattleOutcome.ALLY_VICTORY);
        CampaignProgressStore.commit(playerId,"ENC_M01",BattleOutcome.ALLY_VICTORY);CampaignProgressStore.commit(playerId,"ENC_M02",BattleOutcome.ALLY_VICTORY);CampaignProgressStore.commit(playerId,"ENC_M04",BattleOutcome.ALLY_VICTORY);CampaignProgressStore.commit(playerId,"BATTLE_B01",BattleOutcome.ALLY_VICTORY);
        for(int i=0;i<3;i++)CampaignProgressStore.questInteract(playerId,"SPORE_LANTERN");CampaignProgressStore.commit(playerId,"ENC_G02",BattleOutcome.ALLY_VICTORY);CampaignProgressStore.commit(playerId,"ENC_G05",BattleOutcome.ALLY_VICTORY);CampaignProgressStore.commit(playerId,"BATTLE_B02",BattleOutcome.ALLY_VICTORY);
        CampaignProgressStore.questInteract(playerId,"AQUEDUCT_VALVE");CampaignProgressStore.questInteract(playerId,"AQUEDUCT_VALVE");CampaignProgressStore.commit(playerId,"ENC_A04",BattleOutcome.ALLY_VICTORY);CampaignProgressStore.commit(playerId,"BATTLE_B03",BattleOutcome.ALLY_VICTORY);
        CampaignProgressStore.commit(playerId,"ENC_Q01",BattleOutcome.ALLY_VICTORY);CampaignProgressStore.commit(playerId,"ENC_Q02",BattleOutcome.ALLY_VICTORY);
        CampaignProgressStore.commit(playerId,"ENC_Q03",BattleOutcome.ALLY_VICTORY);CampaignProgressStore.recordKill(playerId,"E014",1);CampaignProgressStore.recordLoot(playerId,"CORE_FRAGMENT",1);
        CampaignProgressStore.commit(playerId,"ENC_Q05",BattleOutcome.ALLY_VICTORY);CampaignProgressStore.recordKill(playerId,"E014",1);CampaignProgressStore.recordLoot(playerId,"CORE_FRAGMENT",1);
        CampaignProgressStore.commit(playerId,"BATTLE_B04",BattleOutcome.ALLY_VICTORY);
        assertTrue(CampaignProgressStore.quests(playerId).completed().contains("MQ_C04_03_kolvak"));
    }
}
