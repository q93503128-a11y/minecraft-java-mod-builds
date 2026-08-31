package io.github.q93503128.turnbound.progression;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EquipmentPendingRewardTest {
    @Test
    void fullInventoryPreservesIncomingNormalRewardAndAllowsImmediateSale() {
        EquipmentInventory inventory = fullInventory();
        PlayerProfile profile = profileWithGold(0);

        EquipmentInventory.Item reward = inventory.grantReward("W05");
        assertEquals(EquipmentInventory.MAX_INSTANCES, inventory.size());
        assertEquals(List.of(reward), inventory.pendingRewards());
        assertEquals(15_000, inventory.sellPending(reward.instanceId(), profile));
        assertTrue(inventory.pendingRewards().isEmpty());
        assertEquals(15_000, profile.currency(PlayerProfile.Currency.GOLD));
        assertEquals(EquipmentInventory.MAX_INSTANCES, inventory.size());
    }

    @Test
    void signatureRewardCannotBeSoldAndCanBeClaimedAfterExistingSaleFreesSpace() {
        EquipmentInventory inventory = fullInventory();
        PlayerProfile profile = profileWithGold(0);
        EquipmentInventory.Item signature = inventory.grantReward("sig_p01_unending_vow");

        assertThrows(IllegalStateException.class, () -> inventory.sellPending(signature.instanceId(), profile));
        String existing = inventory.items().keySet().stream().sorted().findFirst().orElseThrow();
        inventory.sell(existing, profile);
        EquipmentInventory.Item claimed = inventory.claimPending(signature.instanceId());

        assertEquals(signature, claimed);
        assertEquals(EquipmentInventory.MAX_INSTANCES, inventory.size());
        assertTrue(inventory.pendingRewards().isEmpty());
        assertEquals(signature.itemId(), inventory.item(signature.instanceId()).itemId());
    }

    private static EquipmentInventory fullInventory() {
        EquipmentInventory inventory = EquipmentInventory.empty();
        for (int i = 0; i < EquipmentInventory.MAX_INSTANCES; i++) inventory.grant("W01");
        return inventory;
    }

    private static PlayerProfile profileWithGold(long gold) {
        return PlayerProfile.restore(new PlayerProfile.Snapshot(gold, 0, 0, 0, Set.of("P01"), 0, false, false));
    }
}
