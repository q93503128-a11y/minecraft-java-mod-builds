package dev.moonseungjun.openworldrpg.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.state.ProjectShieldFamily;
import dev.moonseungjun.openworldrpg.combat.state.ProjectWeaponFamily;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.progression.r01.R01StarterEquipment;
import org.junit.jupiter.api.Test;

class R01StarterEquipmentTest {
    @Test
    void openingEquipmentMatchesClosedR01Canon() {
        var sword = R01StarterEquipment.openingSword();
        var buckler = R01StarterEquipment.openingBuckler();

        assertEquals("openworld_rpg:heartland_arming_sword", sword.itemId());
        assertEquals(1, sword.itemLevel());
        assertEquals(ProjectWeaponFamily.SWORD, sword.weaponFamily().orElseThrow());

        assertEquals("openworld_rpg:watch_buckler", buckler.itemId());
        assertEquals(1, buckler.itemLevel());
        assertEquals(ProjectShieldFamily.BUCKLER, buckler.shieldFamily().orElseThrow());
    }

    @Test
    void firstClassStarterWeaponsMatchClosedR01Canon() {
        assertTrue(R01StarterEquipment.firstClassWeapon(RootClass.WARRIOR).isEmpty());
        assertTrue(R01StarterEquipment.firstClassWeapon(RootClass.GUARDIAN).isEmpty());

        assertEquals(
                ProjectWeaponFamily.BOW,
                R01StarterEquipment.firstClassWeapon(RootClass.HUNTER)
                        .orElseThrow()
                        .weaponFamily()
                        .orElseThrow()
        );
        assertEquals(
                ProjectWeaponFamily.STAFF,
                R01StarterEquipment.firstClassWeapon(RootClass.CLERIC)
                        .orElseThrow()
                        .weaponFamily()
                        .orElseThrow()
        );
        assertEquals(
                ProjectWeaponFamily.WAND,
                R01StarterEquipment.firstClassWeapon(RootClass.MAGE)
                        .orElseThrow()
                        .weaponFamily()
                        .orElseThrow()
        );
    }
}
