package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.progression.PlayerProfile;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CampaignEquipmentSaleIntegrationTest {
    @Test
    void campaignSaleCreditsGoldRemovesInstanceAndMarksSaveDirty() {
        UUID playerId = UUID.randomUUID();
        try {
            CampaignProgressStore.ensureNewGame(playerId);
            long beforeGold = CampaignProgressStore.currency(playerId, PlayerProfile.Currency.GOLD);
            var item = CampaignProgressStore.grantEquipment(playerId, "W05");
            CampaignProgressStore.markClean(playerId);

            assertEquals(15_000, CampaignProgressStore.sellEquipment(playerId, item.instanceId()));
            assertEquals(beforeGold + 15_000, CampaignProgressStore.currency(playerId, PlayerProfile.Currency.GOLD));
            assertFalse(CampaignProgressStore.equipment(playerId).items().containsKey(item.instanceId()));
            assertTrue(CampaignProgressStore.isDirty(playerId));
        } finally {
            CampaignProgressStore.resetForTests(playerId);
        }
    }
}
