package dev.moonseungjun.openworldrpg.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.state.ProjectEquipmentSlot;
import dev.moonseungjun.openworldrpg.inventory.ProjectItemGrade;
import org.junit.jupiter.api.Test;

class R01NessaEquipmentMaterializationTest {
    @Test
    void currentWeaponStockMaterializesWithAllStaticAffixesRuntimeReady() {
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
                R01NessaEquipmentMaterialization.ResolutionStatus.READY,
                resolution.status()
        );
        assertTrue(resolution.blockers().isEmpty());
        assertTrue(resolution.item().isPresent());
    }

    @Test
    void currentArmorStockMaterializesWithoutDroppingUtilityAffixes() {
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
                R01NessaEquipmentMaterialization.ResolutionStatus.READY,
                resolution.status()
        );
        assertTrue(resolution.blockers().isEmpty());
        assertTrue(resolution.item().isPresent());
    }
}
