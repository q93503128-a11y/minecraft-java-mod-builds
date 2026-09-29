package dev.moonseungjun.openworldrpg.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.state.ProjectArmorArchetype;
import dev.moonseungjun.openworldrpg.combat.state.ProjectEquipmentSlot;
import dev.moonseungjun.openworldrpg.combat.state.ProjectWeaponFamily;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class R01OrdinaryEquipmentBaseCatalogTest {
    @Test
    void bundledRosterMatchesClosedR01BaseFamilies() {
        var data = R01OrdinaryEquipmentBaseCatalogLoader.loadBundled();

        assertEquals(
                R01OrdinaryEquipmentBaseCatalogData.CANONICAL_ID,
                data.id()
        );
        assertEquals(16, data.bases().size());

        assertEquals(
                ProjectWeaponFamily.SWORD,
                data.base("openworld_rpg:heartland_arming_sword")
                        .weaponFamily().orElseThrow()
        );
        assertEquals(
                ProjectWeaponFamily.BOW,
                data.base("openworld_rpg:riverwood_bow")
                        .weaponFamily().orElseThrow()
        );
        assertEquals(
                ProjectArmorArchetype.HEAVY,
                data.base("openworld_rpg:ironbound_guard")
                        .armorArchetype().orElseThrow()
        );
        assertEquals(
                ProjectEquipmentSlot.NECKLACE,
                data.base("openworld_rpg:greenwater_pendant")
                        .fixedSlot().orElseThrow()
        );
    }

    @Test
    void normalWeaponsUsePositiveCanonicalCategoriesPlusTheirOwnFamilyPower() {
        var bases = R01OrdinaryEquipmentBaseCatalogLoader.loadBundled();
        var staticCatalog = OrdinaryEquipmentAffixCatalogLoader.loadBundled();
        var parameterized =
                OrdinaryEquipmentParameterizedAffixLoader.loadBundled();

        var sword = bases.eligibleAffixes(
                "openworld_rpg:heartland_arming_sword",
                staticCatalog,
                parameterized
        );

        assertEquals(25, sword.size());
        assertTrue(sword.stream().noneMatch(value ->
                value.category()
                        == OrdinaryEquipmentAffixRoller.AffixCategory.DEFENSE
        ));
        assertTrue(sword.stream().anyMatch(value ->
                value.id().equals(
                        "openworld_rpg:affix/weapon_family/sword_power"
                )
        ));
        assertFalse(sword.stream().anyMatch(value ->
                value.id().equals(
                        "openworld_rpg:affix/weapon_family/bow_power"
                )
        ));
    }

    @Test
    void armorAndAccessoriesKeepAllGenericStaticCategories() {
        var bases = R01OrdinaryEquipmentBaseCatalogLoader.loadBundled();
        var staticCatalog = OrdinaryEquipmentAffixCatalogLoader.loadBundled();
        var parameterized =
                OrdinaryEquipmentParameterizedAffixLoader.loadBundled();

        for (String baseId : Set.of(
                "openworld_rpg:river_scholar_garb",
                "openworld_rpg:wayfarer_leathers",
                "openworld_rpg:ironbound_guard",
                "openworld_rpg:greenwater_pendant",
                "openworld_rpg:roadworn_band",
                "openworld_rpg:wayfarers_token",
                "openworld_rpg:quarry_seal",
                "openworld_rpg:watch_buckler",
                "openworld_rpg:apprentice_focus"
        )) {
            var eligible = bases.eligibleAffixes(
                    baseId,
                    staticCatalog,
                    parameterized
            );
            assertEquals(29, eligible.size());
            assertEquals(
                    Set.of(
                            OrdinaryEquipmentAffixRoller.AffixCategory.PRIMARY,
                            OrdinaryEquipmentAffixRoller.AffixCategory.OFFENSE,
                            OrdinaryEquipmentAffixRoller.AffixCategory.DEFENSE,
                            OrdinaryEquipmentAffixRoller.AffixCategory.RESOURCE,
                            OrdinaryEquipmentAffixRoller.AffixCategory.UTILITY
                    ),
                    eligible.stream()
                            .map(OrdinaryEquipmentAffixRoller.AffixDefinition::category)
                            .collect(Collectors.toSet())
            );
        }
    }

    @Test
    void runtimeBlockersRemainVisibleInsteadOfSilentlyChangingPoolOdds() {
        var bases = R01OrdinaryEquipmentBaseCatalogLoader.loadBundled();
        var staticCatalog = OrdinaryEquipmentAffixCatalogLoader.loadBundled();
        var parameterized =
                OrdinaryEquipmentParameterizedAffixLoader.loadBundled();

        var weaponBlockers = bases.runtimeBlockers(
                "openworld_rpg:riverwood_bow",
                staticCatalog,
                parameterized
        );
        assertFalse(weaponBlockers.contains(
                "openworld_rpg:affix/attack_speed"
        ));
        assertTrue(weaponBlockers.contains(
                "openworld_rpg:affix/weak_point_damage"
        ));
        assertFalse(weaponBlockers.contains(
                "openworld_rpg:affix/movement_speed"
        ));
        assertFalse(weaponBlockers.contains(
                "openworld_rpg:affix/healing_done"
        ));
        assertFalse(weaponBlockers.contains(
                "openworld_rpg:affix/dodge_sprint_stamina_cost_reduction"
        ));
        assertFalse(weaponBlockers.contains(
                "openworld_rpg:affix/critical_chance"
        ));
        assertFalse(weaponBlockers.contains(
                "openworld_rpg:affix/weapon_family/bow_power"
        ));
    }

    @Test
    void armorFamilyMaterializationDerivesStableSlotSpecificItemId() {
        var bases = R01OrdinaryEquipmentBaseCatalogLoader.loadBundled();

        var profile = bases.materializerProfile(
                "openworld_rpg:ironbound_guard",
                ProjectEquipmentSlot.CHEST
        );

        assertEquals(
                "openworld_rpg:ironbound_guard/chest",
                profile.itemId()
        );
        assertEquals(ProjectEquipmentSlot.CHEST, profile.slot());
        assertEquals(
                ProjectArmorArchetype.HEAVY,
                profile.armorArchetype().orElseThrow()
        );
    }
}
