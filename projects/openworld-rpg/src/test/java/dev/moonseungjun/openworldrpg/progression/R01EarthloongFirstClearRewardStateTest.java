package dev.moonseungjun.openworldrpg.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.state.EquipmentCombatAffixKind;
import dev.moonseungjun.openworldrpg.combat.state.ProjectWeaponFamily;
import dev.moonseungjun.openworldrpg.inventory.PlayerInventoryState;
import dev.moonseungjun.openworldrpg.inventory.ProjectItemGrade;
import dev.moonseungjun.openworldrpg.progression.r01.R01EarthloongRewardChoice;
import dev.moonseungjun.openworldrpg.progression.r01.R01PlayerState;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class R01EarthloongFirstClearRewardStateTest {
    @Test
    void firstClearCreatesChoiceAndScalePendingClaims() {
        R01PlayerState state = R01PlayerState.initial().markEarthloongFirstClear(10L);

        assertTrue(state.quarry().firstClear());
        assertTrue(state.quarry().rewardChoicePending());
        assertTrue(state.economy().pendingRewardClaimIds().contains(
                R01PlayerState.EARTHLOONG_CHOICE_CLAIM_ID
        ));
        assertTrue(state.economy().pendingRewardClaimIds().contains(
                R01PlayerState.EARTHLOONG_SCALE_CLAIM_ID
        ));
    }

    @Test
    void committedChoiceCannotBeChangedAfterCrashOrReconnect() {
        R01PlayerState first = R01PlayerState.initial()
                .markEarthloongFirstClear(10L)
                .commitEarthloongRewardChoice(
                        R01EarthloongRewardChoice.IRONROOT_LONGSWORD.choiceFlag(),
                        11L
                );

        assertEquals(
                R01EarthloongRewardChoice.IRONROOT_LONGSWORD.choiceFlag(),
                first.earthloongRewardChoiceFlag().orElseThrow()
        );
        assertThrows(
                IllegalStateException.class,
                () -> first.commitEarthloongRewardChoice(
                        R01EarthloongRewardChoice.RIVERTHORN_BOW.choiceFlag(),
                        12L
                )
        );

        R01PlayerState delivered = first.markEarthloongRewardChoiceDelivered(
                R01EarthloongRewardChoice.IRONROOT_LONGSWORD.choiceFlag(),
                13L
        );
        assertTrue(delivered.quarry().firstClearRewardClaimed());
        assertFalse(delivered.quarry().rewardChoicePending());
        assertFalse(delivered.economy().pendingRewardClaimIds().contains(
                R01PlayerState.EARTHLOONG_CHOICE_CLAIM_ID
        ));
    }

    @Test
    void deterministicChoicePayloadsMatchSuperiorLv8Canon() {
        assertChoice(
                R01EarthloongRewardChoice.IRONROOT_LONGSWORD,
                "openworld_rpg:ironroot_longsword",
                ProjectWeaponFamily.SWORD,
                Map.of(
                        EquipmentCombatAffixKind.STR, 3.0,
                        EquipmentCombatAffixKind.PHYSICAL_POWER, 0.07,
                        EquipmentCombatAffixKind.GUARD_STRENGTH, 0.135
                )
        );
        assertChoice(
                R01EarthloongRewardChoice.RIVERTHORN_BOW,
                "openworld_rpg:riverthorn_bow",
                ProjectWeaponFamily.BOW,
                Map.of(
                        EquipmentCombatAffixKind.DEX, 3.0,
                        EquipmentCombatAffixKind.CRITICAL_CHANCE, 0.031,
                        EquipmentCombatAffixKind.ATTACK_SPEED, 0.055
                )
        );
        assertChoice(
                R01EarthloongRewardChoice.LUMENWOOD_STAFF,
                "openworld_rpg:lumenwood_staff",
                ProjectWeaponFamily.STAFF,
                Map.of(
                        EquipmentCombatAffixKind.INT, 3.0,
                        EquipmentCombatAffixKind.MAX_MANA, 0.08,
                        EquipmentCombatAffixKind.MAGIC_POWER, 0.07
                )
        );
    }

    @Test
    void importantChoiceDeliveryIsIdempotentAndKeepsSuperiorGrade() {
        var item = R01EarthloongRewardChoice.LUMENWOOD_STAFF.inventoryItem();
        var first = PlayerInventoryState.initial().deliverImportantOnce(
                "openworld_rpg:r01/earthloong_first_clear/choice_item",
                item
        );
        var retry = first.state().deliverImportantOnce(
                "openworld_rpg:r01/earthloong_first_clear/choice_item",
                item
        );

        assertEquals(PlayerInventoryState.DeliveryStatus.DELIVERED, first.status());
        assertEquals(
                ProjectItemGrade.SUPERIOR,
                first.state().backpack().itemAt(0).orElseThrow()
                        .resolvedEquipmentGrade().orElseThrow()
        );
        assertEquals(PlayerInventoryState.DeliveryStatus.ALREADY_COMPLETED, retry.status());
    }

    private static void assertChoice(
            R01EarthloongRewardChoice choice,
            String itemId,
            ProjectWeaponFamily family,
            Map<EquipmentCombatAffixKind, Double> expectedAffixes
    ) {
        var item = choice.inventoryItem();
        var equipment = item.equipmentProjection().orElseThrow();
        assertEquals(itemId, item.itemId());
        assertEquals(8, equipment.itemLevel());
        assertEquals(family, equipment.weaponFamily().orElseThrow());
        assertEquals(ProjectItemGrade.SUPERIOR, item.resolvedEquipmentGrade().orElseThrow());
        assertEquals(125L, item.unitSellValue());

        Map<EquipmentCombatAffixKind, Double> actual = equipment.affixes().stream()
                .collect(Collectors.toMap(affix -> affix.kind(), affix -> affix.value()));
        assertEquals(expectedAffixes, actual);
    }
}
