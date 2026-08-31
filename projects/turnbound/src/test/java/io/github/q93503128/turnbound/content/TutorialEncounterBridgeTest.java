package io.github.q93503128.turnbound.content;

import io.github.q93503128.turnbound.combat.BattleOutcome;
import io.github.q93503128.turnbound.world.CampaignProgressStore;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TutorialEncounterBridgeTest {
    @Test void bridgeUsesOnlyExistingP0RequiredCombinationsAndNoDirectEconomyReward(){
        assertEquals(List.of("E001","E001"),V04Catalogs.encounter("TUTORIAL_1").enemies());
        assertEquals(List.of("E001","E002"),V04Catalogs.encounter("TUTORIAL_2").enemies());
        assertEquals(List.of("E003","E002"),V04Catalogs.encounter("TUTORIAL_3").enemies());
        for(String id:List.of("TUTORIAL_1","TUTORIAL_2","TUTORIAL_3")){ assertEquals("TUTORIAL",V04Catalogs.encounter(id).region()); assertEquals(0,V04Catalogs.battleXp(V04Catalogs.encounter(id))); assertEquals(0,V04Catalogs.battleGold(V04Catalogs.encounter(id))); }
    }

    @Test void prologueQuestChainUnlocksMeadowOnlyAfterDirectorPartyAndThreeWins(){
        UUID id=UUID.randomUUID();
        try{
            CampaignProgressStore.ensureNewGame(id);
            CampaignProgressStore.questInteract(id,"Director Iven");
            assertTrue(CampaignProgressStore.quests(id).completed().contains("MQ_P00_01_arrival"));
            CampaignProgressStore.setActiveParty(id,List.of("P01","P03","P04","F03"));
            assertTrue(CampaignProgressStore.quests(id).completed().contains("MQ_P00_02_first_party"));
            for(String battle:List.of("TUTORIAL_1","TUTORIAL_2")) CampaignProgressStore.commit(id,battle,BattleOutcome.ALLY_VICTORY);
            assertFalse(CampaignProgressStore.quests(id).unlockFlags().contains("REGION_MEADOW"));
            CampaignProgressStore.commit(id,"TUTORIAL_3",BattleOutcome.ALLY_VICTORY);
            assertTrue(CampaignProgressStore.quests(id).completed().contains("MQ_P00_03_south_gate"));
            assertTrue(CampaignProgressStore.quests(id).unlockFlags().contains("REGION_MEADOW"));
        }finally{CampaignProgressStore.removeRuntime(id);}
    }
}
