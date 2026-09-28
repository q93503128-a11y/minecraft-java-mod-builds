package dev.moonseungjun.openworldrpg.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.state.ProjectEquipmentSlot;
import dev.moonseungjun.openworldrpg.inventory.ProjectItemGrade;
import org.junit.jupiter.api.Test;

class R01NessaEquipmentMaterializationTest {
    @Test
    void currentWeaponStockFailsClosedOnRealRemainingRuntimeBlockers() {
        var stock = new R01NessaMarketRules.StockItem(
                1,
                R01NessaMarketRules.RIVERWOOD_BOW,
                R01NessaMarketRules.MarketCategory.WEAPON,
                ProjectEquipmentSlot.MAIN_WEAPON,
                ProjectItemGrade.REFINED,
                4,
                240L,
                778899L
        );

        var resolution = R01NessaEquipmentMaterialization.resolve(stock);

        assertEquals(
                R01NessaEquipmentMaterialization.ResolutionStatus
                        .RUNTIME_AFFIX_BLOCKED,
                resolution.status()
        );
        assertTrue(resolution.item().isEmpty());
        assertTrue(resolution.blockers().contains(
                "openworld_rpg:affix/attack_speed"
        ));
        assertTrue(resolution.blockers().contains(
                "openworld_rpg:affix/weak_point_damage"
        ));
        assertFalse(resolution.blockers().contains(
                "openworld_rpg:affix/movement_speed"
        ));

        // Matching weapon-family power and Movement Speed are live and therefore must not block.
        assertFalse(resolution.blockers().contains(
                "openworld_rpg:affix/weapon_family/bow_power"
        ));
    }

    @Test
    void currentArmorStockReportsUnsupportedGenericAffixesWithoutDroppingThem() {
        var stock = new R01NessaMarketRules.StockItem(
                3,
                R01NessaMarketRules.IRONBOUND_GUARD,
                R01NessaMarketRules.MarketCategory.ARMOR,
                ProjectEquipmentSlot.CHEST,
                ProjectItemGrade.SUPERIOR,
                6,
                420L,
                112233L
        );

        var resolution = R01NessaEquipmentMaterialization.resolve(stock);

        assertEquals(
                R01NessaEquipmentMaterialization.ResolutionStatus
                        .RUNTIME_AFFIX_BLOCKED,
                resolution.status()
        );
        assertTrue(resolution.blockers().contains(
                "openworld_rpg:affix/attack_speed"
        ));
        assertTrue(resolution.blockers().contains(
                "openworld_rpg:affix/healing_received"
        ));
        assertFalse(resolution.blockers().contains(
                "openworld_rpg:affix/critical_chance"
        ));
    }
}
