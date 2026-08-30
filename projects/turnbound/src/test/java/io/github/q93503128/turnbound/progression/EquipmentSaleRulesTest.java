package io.github.q93503128.turnbound.progression;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EquipmentSaleRulesTest {
    @Test
    void canonicalTierSalePricesMatchV04NumericWiki() {
        assertEquals(2_000, EquipmentRules.salePrice("T1"));
        assertEquals(6_000, EquipmentRules.salePrice("T2"));
        assertEquals(15_000, EquipmentRules.salePrice("T3"));
        assertEquals(30_000, EquipmentRules.salePrice("T4"));
        assertThrows(IllegalArgumentException.class, () -> EquipmentRules.salePrice("SIGNATURE"));
    }

    @Test
    void sellingUnequippedNormalEquipmentRemovesOnlyThatInstanceAndGrantsGold() {
        EquipmentInventory inventory = EquipmentInventory.empty();
        EquipmentInventory.Item t1 = inventory.grant("W01");
        EquipmentInventory.Item t3 = inventory.grant("W05");
        PlayerProfile profile = profileWithGold(100);

        assertEquals(15_000, inventory.sell(t3.instanceId(), profile));
        assertEquals(15_100, profile.currency(PlayerProfile.Currency.GOLD));
        assertEquals(1, inventory.size());
        assertEquals("W01", inventory.item(t1.instanceId()).itemId());
        assertThrows(IllegalArgumentException.class, () -> inventory.item(t3.instanceId()));
    }

    @Test
    void equippedAndSignatureEquipmentStayProtectedWhenCanonDoesNotDefineSaleHandling() {
        EquipmentInventory inventory = EquipmentInventory.empty();
        EquipmentInventory.Item equipped = inventory.grant("W01");
        EquipmentInventory.Item signature = inventory.grant("sig_p01_unending_vow");
        PlayerProfile profile = profileWithGold(500);
        inventory.equip("P01", equipped.instanceId(), 4);

        assertThrows(IllegalStateException.class, () -> inventory.sell(equipped.instanceId(), profile));
        assertThrows(IllegalStateException.class, () -> inventory.sell(signature.instanceId(), profile));
        assertEquals(500, profile.currency(PlayerProfile.Currency.GOLD));
        assertEquals(2, inventory.size());
        assertEquals(equipped.instanceId(), inventory.loadout("P01").weapon());
    }

    @Test
    void saleFreesAFullInventorySlotInsteadOfDroppingRewardOnGround() {
        EquipmentInventory inventory = EquipmentInventory.empty();
        String first = "";
        for (int i = 0; i < EquipmentInventory.MAX_INSTANCES; i++) {
            EquipmentInventory.Item item = inventory.grant("W01");
            if (i == 0) first = item.instanceId();
        }
        assertEquals(0, inventory.freeSlots());

        PlayerProfile profile = profileWithGold(0);
        assertEquals(2_000, inventory.sell(first, profile));
        assertEquals(1, inventory.freeSlots());
        assertTrue(inventory.hasFreeSlot());
        assertEquals(2_000, profile.currency(PlayerProfile.Currency.GOLD));
    }

    private static PlayerProfile profileWithGold(long gold) {
        return PlayerProfile.restore(new PlayerProfile.Snapshot(
                gold, 0, 0, 0, Set.of("P01"), 0, false, false));
    }
}
