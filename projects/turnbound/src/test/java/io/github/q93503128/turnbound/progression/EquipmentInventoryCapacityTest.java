package io.github.q93503128.turnbound.progression;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EquipmentInventoryCapacityTest {
    @Test
    void inventoryStopsAtCanonicalThreeHundredInstances() {
        EquipmentInventory inventory = EquipmentInventory.empty();
        for (int i = 0; i < EquipmentInventory.MAX_INSTANCES; i++) inventory.grant("W01");
        assertEquals(300, inventory.size());
        assertFalse(inventory.hasFreeSlot());
        assertThrows(IllegalStateException.class, () -> inventory.grant("W01"));
        assertEquals(300, inventory.size());
    }

    @Test
    void fullInventoryQueuesEarnedChoiceRewardButStillRejectsShopPurchaseWithoutSpendingGold() {
        EquipmentInventory inventory = EquipmentInventory.empty();
        for (int i = 0; i < EquipmentInventory.MAX_INSTANCES; i++) inventory.grant("W01");

        inventory.grantChoiceToken("T2", 1);
        EquipmentInventory.Item reward = inventory.claimChoice("T2", "W03");
        assertEquals(0, inventory.choiceTokens("T2"));
        assertEquals(300, inventory.size());
        assertEquals(1, inventory.pendingRewards().size());
        assertEquals(reward, inventory.pendingRewards().getFirst());
        assertEquals("W03", reward.itemId());

        PlayerProfile profile = PlayerProfile.newGame();
        assertEquals(5_000, profile.currency(PlayerProfile.Currency.GOLD));
        assertThrows(IllegalStateException.class, () -> EquipmentRules.buyNormal(inventory, profile, "W01", 1));
        assertEquals(5_000, profile.currency(PlayerProfile.Currency.GOLD));
        assertEquals(300, inventory.size());
        assertEquals(1, inventory.pendingRewards().size());
    }
}
