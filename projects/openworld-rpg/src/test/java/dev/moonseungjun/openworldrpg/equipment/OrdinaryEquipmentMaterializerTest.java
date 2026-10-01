package dev.moonseungjun.openworldrpg.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.state.EquipmentCombatAffixKind;
import dev.moonseungjun.openworldrpg.combat.state.ProjectArmorArchetype;
import dev.moonseungjun.openworldrpg.combat.state.ProjectEquipmentSlot;
import dev.moonseungjun.openworldrpg.combat.state.ProjectShieldFamily;
import dev.moonseungjun.openworldrpg.combat.state.ProjectWeaponFamily;
import dev.moonseungjun.openworldrpg.inventory.ProjectItemGrade;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrdinaryEquipmentMaterializerTest {
    @Test
    void percentagePointsConvertToRuntimeFraction() {
        var catalog = OrdinaryEquipmentAffixCatalogLoader.loadBundled();
        var physical = definition(
                catalog,
                "openworld_rpg:affix/physical_power"
        );

        var result = OrdinaryEquipmentMaterializer.materialize(
                new OrdinaryEquipmentMaterializer.MaterializationRequest(
                        OrdinaryEquipmentMaterializer.BaseProfile.weapon(
                                "openworld_rpg:heartland_arming_sword",
                                ProjectWeaponFamily.SWORD
                        ),
                        ProjectItemGrade.STANDARD,
                        2,
                        List.of(physical),
                        1234L,
                        37L
                ),
                catalog
        );

        var item = result.inventoryItem();
        assertEquals(ProjectItemGrade.STANDARD, item.equipmentGrade().orElseThrow());
        assertEquals(37L, item.unitSellValue());
        var projection = item.equipmentProjection().orElseThrow();
        assertEquals(ProjectWeaponFamily.SWORD, projection.weaponFamily().orElseThrow());
        assertEquals(1, projection.affixes().size());
        assertEquals(
                EquipmentCombatAffixKind.PHYSICAL_POWER,
                projection.affixes().getFirst().kind()
        );
        assertEquals(
                result.rolledAffixes().getFirst().value() / 100.0,
                projection.affixes().getFirst().value(),
                0.000001
        );
    }

    @Test
    void movementSpeedPercentageProjectsToDedicatedRuntimeAffix() {
        var catalog = OrdinaryEquipmentAffixCatalogLoader.loadBundled();
        var movement = definition(
                catalog,
                "openworld_rpg:affix/movement_speed"
        );

        var result = OrdinaryEquipmentMaterializer.materialize(
                new OrdinaryEquipmentMaterializer.MaterializationRequest(
                        OrdinaryEquipmentMaterializer.BaseProfile.accessory(
                                "openworld_rpg:trail_charm",
                                ProjectEquipmentSlot.NECKLACE
                        ),
                        ProjectItemGrade.STANDARD,
                        4,
                        List.of(movement),
                        2468L,
                        40L
                ),
                catalog
        );

        var affix = result.inventoryItem()
                .equipmentProjection().orElseThrow()
                .affixes().getFirst();
        assertEquals(EquipmentCombatAffixKind.MOVEMENT_SPEED, affix.kind());
        assertEquals(
                result.rolledAffixes().getFirst().value() / 100.0,
                affix.value(),
                0.000001
        );
    }

    @Test
    void dodgeSprintCostReductionProjectsToDedicatedRuntimeAffix() {
        var catalog = OrdinaryEquipmentAffixCatalogLoader.loadBundled();
        var reduction = definition(
                catalog,
                "openworld_rpg:affix/dodge_sprint_stamina_cost_reduction"
        );

        var result = OrdinaryEquipmentMaterializer.materialize(
                new OrdinaryEquipmentMaterializer.MaterializationRequest(
                        OrdinaryEquipmentMaterializer.BaseProfile.accessory(
                                "openworld_rpg:trail_charm",
                                ProjectEquipmentSlot.NECKLACE
                        ),
                        ProjectItemGrade.STANDARD,
                        4,
                        List.of(reduction),
                        8642L,
                        40L
                ),
                catalog
        );

        var affix = result.inventoryItem()
                .equipmentProjection().orElseThrow()
                .affixes().getFirst();
        assertEquals(
                EquipmentCombatAffixKind.DODGE_SPRINT_STAMINA_COST_REDUCTION,
                affix.kind()
        );
        assertEquals(
                result.rolledAffixes().getFirst().value() / 100.0,
                affix.value(),
                0.000001
        );
    }

    @Test
    void healingDoneProjectsToOutgoingSkillHealingRuntimeAffix() {
        var catalog = OrdinaryEquipmentAffixCatalogLoader.loadBundled();
        var healingDone = definition(
                catalog,
                "openworld_rpg:affix/healing_done"
        );

        var result = OrdinaryEquipmentMaterializer.materialize(
                new OrdinaryEquipmentMaterializer.MaterializationRequest(
                        OrdinaryEquipmentMaterializer.BaseProfile.accessory(
                                "openworld_rpg:healers_band",
                                ProjectEquipmentSlot.RING_1
                        ),
                        ProjectItemGrade.STANDARD,
                        4,
                        List.of(healingDone),
                        4411L,
                        40L
                ),
                catalog
        );

        var affix = result.inventoryItem()
                .equipmentProjection().orElseThrow()
                .affixes().getFirst();
        assertEquals(EquipmentCombatAffixKind.HEALING_DONE, affix.kind());
        assertEquals(
                result.rolledAffixes().getFirst().value() / 100.0,
                affix.value(),
                0.000001
        );
    }

    @Test
    void healingReceivedProjectsToIncomingHealingRuntimeAffix() {
        var catalog = OrdinaryEquipmentAffixCatalogLoader.loadBundled();
        var healingReceived = definition(
                catalog,
                "openworld_rpg:affix/healing_received"
        );

        var result = OrdinaryEquipmentMaterializer.materialize(
                new OrdinaryEquipmentMaterializer.MaterializationRequest(
                        OrdinaryEquipmentMaterializer.BaseProfile.accessory(
                                "openworld_rpg:healers_band",
                                ProjectEquipmentSlot.RING_1
                        ),
                        ProjectItemGrade.STANDARD,
                        4,
                        List.of(healingReceived),
                        4422L,
                        40L
                ),
                catalog
        );

        var affix = result.inventoryItem()
                .equipmentProjection().orElseThrow()
                .affixes().getFirst();
        assertEquals(EquipmentCombatAffixKind.HEALING_RECEIVED, affix.kind());
        assertEquals(
                result.rolledAffixes().getFirst().value() / 100.0,
                affix.value(),
                0.000001
        );
    }

    @Test
    void primaryValuesRemainWholeRuntimePoints() {
        var catalog = OrdinaryEquipmentAffixCatalogLoader.loadBundled();
        var strength = definition(catalog, "openworld_rpg:affix/str");

        var result = OrdinaryEquipmentMaterializer.materialize(
                new OrdinaryEquipmentMaterializer.MaterializationRequest(
                        OrdinaryEquipmentMaterializer.BaseProfile.weapon(
                                "openworld_rpg:quarry_maul",
                                ProjectWeaponFamily.HAMMER_MACE
                        ),
                        ProjectItemGrade.STANDARD,
                        6,
                        List.of(strength),
                        555L,
                        30L
                ),
                catalog
        );

        double rolled = result.rolledAffixes().getFirst().value();
        double runtime = result.inventoryItem()
                .equipmentProjection().orElseThrow()
                .affixes().getFirst().value();
        assertEquals(Math.rint(rolled), rolled, 0.000001);
        assertEquals(rolled, runtime, 0.000001);
    }

    @Test
    void attackSpeedProjectsToRuntimeWhileUnsupportedCanonicalAffixStillFailsClosed() {
        var catalog = OrdinaryEquipmentAffixCatalogLoader.loadBundled();
        var physical = definition(
                catalog,
                "openworld_rpg:affix/physical_power"
        );
        var attackSpeed = definition(
                catalog,
                "openworld_rpg:affix/attack_speed"
        );
        assertTrue(catalog.runtimeImplemented(attackSpeed.id()));

        var attackSpeedResult = OrdinaryEquipmentMaterializer.materialize(
                new OrdinaryEquipmentMaterializer.MaterializationRequest(
                        OrdinaryEquipmentMaterializer.BaseProfile.weapon(
                                "openworld_rpg:heartland_arming_sword",
                                ProjectWeaponFamily.SWORD
                        ),
                        ProjectItemGrade.STANDARD,
                        4,
                        List.of(attackSpeed),
                        2L,
                        60L
                ),
                catalog
        );
        assertEquals(
                EquipmentCombatAffixKind.ATTACK_SPEED,
                attackSpeedResult.inventoryItem()
                        .equipmentProjection().orElseThrow()
                        .affixes().getFirst().kind()
        );

        var weakPoint = definition(
                catalog,
                "openworld_rpg:affix/weak_point_damage"
        );
        assertTrue(catalog.runtimeImplemented(weakPoint.id()));

        var weakPointResult = OrdinaryEquipmentMaterializer.materialize(
                new OrdinaryEquipmentMaterializer.MaterializationRequest(
                        OrdinaryEquipmentMaterializer.BaseProfile.weapon(
                                "openworld_rpg:heartland_arming_sword",
                                ProjectWeaponFamily.SWORD
                        ),
                        ProjectItemGrade.REFINED,
                        4,
                        List.of(physical, weakPoint),
                        1L,
                        60L
                ),
                catalog
        );
        assertTrue(
                weakPointResult.inventoryItem()
                        .equipmentProjection().orElseThrow()
                        .affixes().stream()
                        .anyMatch(affix ->
                                affix.kind()
                                        == EquipmentCombatAffixKind.WEAK_POINT_DAMAGE
                        )
        );
    }

    @Test
    void supportedSuperiorArmorProducesThreeDistinctAffixes() {
        var catalog = OrdinaryEquipmentAffixCatalogLoader.loadBundled();
        var supported = catalog.implementedDefinitions();

        var result = OrdinaryEquipmentMaterializer.materialize(
                new OrdinaryEquipmentMaterializer.MaterializationRequest(
                        OrdinaryEquipmentMaterializer.BaseProfile.armor(
                                "openworld_rpg:ironbound_guard_chest",
                                ProjectEquipmentSlot.CHEST,
                                ProjectArmorArchetype.HEAVY
                        ),
                        ProjectItemGrade.SUPERIOR,
                        6,
                        supported,
                        998877L,
                        105L
                ),
                catalog
        );

        assertEquals(3, result.rolledAffixes().size());
        assertEquals(
                3L,
                result.rolledAffixes().stream()
                        .map(OrdinaryEquipmentAffixRoller.GeneratedAffix::id)
                        .distinct()
                        .count()
        );
        assertEquals(
                ProjectArmorArchetype.HEAVY,
                result.inventoryItem()
                        .equipmentProjection().orElseThrow()
                        .armorArchetype().orElseThrow()
        );
    }

    @Test
    void shieldFocusAndAccessoryProfilesPreserveCanonicalSlots() {
        var shield = OrdinaryEquipmentMaterializer.BaseProfile.shield(
                "openworld_rpg:watch_buckler",
                ProjectShieldFamily.BUCKLER
        );
        var focus = OrdinaryEquipmentMaterializer.BaseProfile.focus(
                "openworld_rpg:apprentice_focus"
        );
        var accessory = OrdinaryEquipmentMaterializer.BaseProfile.accessory(
                "openworld_rpg:greenwater_pendant",
                ProjectEquipmentSlot.NECKLACE
        );

        assertEquals(ProjectEquipmentSlot.OFF_HAND, shield.slot());
        assertEquals(ProjectEquipmentSlot.OFF_HAND, focus.slot());
        assertEquals(ProjectEquipmentSlot.NECKLACE, accessory.slot());

        assertThrows(
                IllegalArgumentException.class,
                () -> OrdinaryEquipmentMaterializer.BaseProfile.accessory(
                        "openworld_rpg:not_an_accessory",
                        ProjectEquipmentSlot.CHEST
                )
        );
    }

    private static OrdinaryEquipmentAffixRoller.AffixDefinition definition(
            OrdinaryEquipmentAffixCatalogData catalog,
            String id
    ) {
        return catalog.definitions().stream()
                .filter(value -> value.id().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
