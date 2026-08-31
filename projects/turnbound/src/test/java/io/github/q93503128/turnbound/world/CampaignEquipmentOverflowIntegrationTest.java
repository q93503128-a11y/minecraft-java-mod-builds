package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.progression.EquipmentInventory;
import io.github.q93503128.turnbound.progression.PlayerProfile;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CampaignEquipmentOverflowIntegrationTest {
    @Test
    void campaignRewardAtThreeHundredQueuesAndIncomingSaleIsPersistableMutation() {
        UUID playerId = UUID.randomUUID();
        try {
            CampaignProgressStore.ensureNewGame(playerId);
            for (int i = 0; i < EquipmentInventory.MAX_INSTANCES; i++) CampaignProgressStore.grantEquipment(playerId, "W01");
            var reward = CampaignProgressStore.grantEquipment(playerId, "W05");
            var before = CampaignProgressStore.snapshot(playerId);
            assertEquals(EquipmentInventory.MAX_INSTANCES, before.equipment().items().size());
            assertEquals(reward, before.equipment().pendingRewards().getFirst());

            long gold = CampaignProgressStore.currency(playerId, PlayerProfile.Currency.GOLD);
            CampaignProgressStore.markClean(playerId);
            assertEquals(15_000, CampaignProgressStore.sellPendingEquipment(playerId, reward.instanceId()));
            assertEquals(gold + 15_000, CampaignProgressStore.currency(playerId, PlayerProfile.Currency.GOLD));
            assertTrue(CampaignProgressStore.snapshot(playerId).equipment().pendingRewards().isEmpty());
            assertTrue(CampaignProgressStore.isDirty(playerId));
        } finally {
            CampaignProgressStore.resetForTests(playerId);
        }
    }
}
