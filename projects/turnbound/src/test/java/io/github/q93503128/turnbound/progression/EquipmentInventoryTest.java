package io.github.q93503128.turnbound.progression;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EquipmentInventoryTest {
    @Test
    void enhancementUsesGoldOnlyAndCanonicalMainSubScaling() {
        EquipmentInventory inventory = EquipmentInventory.empty();
        EquipmentInventory.Item weapon = inventory.grant("W01");
        PlayerProfile profile = PlayerProfile.restore(new PlayerProfile.Snapshot(
                1_000_000, 0, 0, 0, Set.of("P01"), 0, false, false));

        long before = profile.currency(PlayerProfile.Currency.GOLD);
        weapon = inventory.enhance(weapon.instanceId(), profile);
        assertEquals(1, weapon.enhancementLevel());
        assertEquals(before - 50, profile.currency(PlayerProfile.Currency.GOLD));
        assertEquals(0.05 * 1.04, EquipmentInventory.scaledMain(0.05, 1), 0.000001);
        assertEquals(0.03, EquipmentInventory.scaledSub(0.03, 4), 0.000001);
        assertEquals(0.03 * 1.25, EquipmentInventory.scaledSub(0.03, 5), 0.000001);
        assertEquals(0.03 * 2.00, EquipmentInventory.scaledSub(0.03, 20), 0.000001);
    }

    @Test
    void signatureRequiresCorrectOwnerAndSixStarsAndUnlocksMilestonesByEnhancement() {
        EquipmentInventory inventory = EquipmentInventory.empty();
        EquipmentInventory.Item signature = inventory.grant("sig_p01_unending_vow");
        assertThrows(IllegalStateException.class, () -> inventory.equip("P01", signature.instanceId(), 5));
        assertThrows(IllegalArgumentException.class, () -> inventory.equip("P03", signature.instanceId(), 6));
        inventory.equip("P01", signature.instanceId(), 6);
        assertTrue(inventory.fixedRules("P01").contains("FOCUS3_ACTIVE1_GAUGE_60"));
    }

    @Test
    void choiceTokenCanOnlyClaimMatchingTier() {
        EquipmentInventory inventory = EquipmentInventory.empty();
        inventory.grantChoiceToken("T2", 1);
        assertThrows(IllegalArgumentException.class, () -> inventory.claimChoice("T2", "W01"));
        EquipmentInventory.Item claimed = inventory.claimChoice("T2", "W03");
        assertEquals("W03", claimed.itemId());
        assertEquals(0, inventory.choiceTokens("T2"));
    }
}
