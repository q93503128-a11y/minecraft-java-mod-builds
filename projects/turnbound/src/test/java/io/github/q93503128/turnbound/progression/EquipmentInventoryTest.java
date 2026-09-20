package io.github.q93503128.turnbound.progression;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EquipmentInventoryTest {
    @Test
    void enhancementUsesGoldOnlyStopsAtTenAndOnlyScalesMainStat() {
        EquipmentInventory inventory = EquipmentInventory.empty();
        EquipmentInventory.Item weapon = inventory.grant("W01");
        PlayerProfile profile = PlayerProfile.restore(new PlayerProfile.Snapshot(
                1_000_000, 0, 0, 0, Set.of("P01"), 0, false, false));

        long before = profile.currency(PlayerProfile.Currency.GOLD);
        String weaponId = weapon.instanceId();
        for (int i = 0; i < GrowthRulesV1.maxEnhancement(); i++) {
            weapon = inventory.enhance(weaponId, profile);
        }

        assertEquals(10, weapon.enhancementLevel());
        assertEquals(before - 4_000, profile.currency(PlayerProfile.Currency.GOLD));
        assertEquals(0.05 * 1.40, EquipmentInventory.scaledMain(0.05, 10), 0.000001);
        assertEquals(0.03, EquipmentInventory.scaledSub(0.03, 10), 0.000001);
        assertThrows(IllegalStateException.class, () -> inventory.enhance(weaponId, profile));
    }

    @Test
    void signatureRequiresCorrectOwnerButNotLegacyStarPromotionAndHasOneTenMilestone() {
        EquipmentInventory inventory = EquipmentInventory.empty();
        EquipmentInventory.Item signature = inventory.grant("sig_p01_unending_vow");
        String signatureId = signature.instanceId();
        assertThrows(IllegalArgumentException.class, () -> inventory.equip("P03", signatureId));
        inventory.equip("P01", signatureId);
        assertTrue(inventory.fixedRules("P01").contains("FOCUS3_ACTIVE1_GAUGE_60"));

        PlayerProfile profile = PlayerProfile.restore(new PlayerProfile.Snapshot(
                1_000_000, 0, 0, 0, Set.of("P01"), 0, false, false));
        for (int i = 0; i < GrowthRulesV1.maxEnhancement(); i++) {
            signature = inventory.enhance(signatureId, profile);
        }
        assertTrue(inventory.fixedRules("P01").contains("FOCUS3_KILL_NEXT_FOCUS_PLUS_1"));
        assertFalse(inventory.fixedRules("P01").contains("ACTIVE1_FOCUS_KILL_CD_MINUS_1"));
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
