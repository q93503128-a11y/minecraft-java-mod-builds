package dev.moonseungjun.openworldrpg.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.state.EquipmentCombatAffixKind;
import dev.moonseungjun.openworldrpg.combat.state.ProjectWeaponFamily;
import dev.moonseungjun.openworldrpg.inventory.ProjectItemGrade;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrdinaryEquipmentParameterizedAffixTest {
    @Test
    void weaponFamilyPowerTemplateMatchesCanonicalRangeForEveryFamily() {
        var data = OrdinaryEquipmentParameterizedAffixLoader.loadBundled();

        assertEquals(
                OrdinaryEquipmentParameterizedAffixData.CANONICAL_ID,
                data.id()
        );
        assertEquals(
                ProjectWeaponFamily.values().length,
                data.weaponFamilyPower().allowedFamilies().size()
        );

        for (ProjectWeaponFamily family : ProjectWeaponFamily.values()) {
            var definition = data.weaponFamilyPower(family);
            assertEquals(
                    OrdinaryEquipmentAffixRoller.AffixCategory.OFFENSE,
                    definition.category()
            );
            assertEquals(4.0, definition.rawRange().min(), 0.000001);
            assertEquals(10.0, definition.rawRange().max(), 0.000001);
            assertTrue(data.matchesCanonical(definition));
        }
    }

    @Test
    void materializerProducesFamilyScopedCombatAffix() {
        var catalog = OrdinaryEquipmentAffixCatalogLoader.loadBundled();
        var parameterized =
                OrdinaryEquipmentParameterizedAffixLoader.loadBundled();
        var familyPower =
                parameterized.weaponFamilyPower(ProjectWeaponFamily.BOW);

        var result = OrdinaryEquipmentMaterializer.materialize(
                new OrdinaryEquipmentMaterializer.MaterializationRequest(
                        OrdinaryEquipmentMaterializer.BaseProfile.weapon(
                                "openworld_rpg:riverwood_bow",
                                ProjectWeaponFamily.BOW
                        ),
                        ProjectItemGrade.STANDARD,
                        2,
                        List.of(familyPower),
                        12345L,
                        37L
                ),
                catalog
        );

        var runtime = result.inventoryItem()
                .equipmentProjection()
                .orElseThrow()
                .affixes()
                .getFirst();

        assertEquals(
                EquipmentCombatAffixKind.WEAPON_FAMILY_POWER,
                runtime.kind()
        );
        assertEquals(
                ProjectWeaponFamily.BOW,
                runtime.weaponFamily().orElseThrow()
        );
        assertEquals(
                result.rolledAffixes().getFirst().value() / 100.0,
                runtime.value(),
                0.000001
        );
    }
}
