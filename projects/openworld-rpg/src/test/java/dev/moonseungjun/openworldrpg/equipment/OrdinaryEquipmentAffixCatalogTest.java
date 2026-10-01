package dev.moonseungjun.openworldrpg.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OrdinaryEquipmentAffixCatalogTest {
    @Test
    void bundledCatalogMatchesCanonicalStaticAffixRanges() {
        var data = OrdinaryEquipmentAffixCatalogLoader.loadBundled();

        assertEquals(
                OrdinaryEquipmentAffixCatalogData.CANONICAL_ID,
                data.id()
        );
        assertEquals(29, data.affixes().size());
        assertEquals(29, data.implementedDefinitions().size());
        assertTrue(data.resourceAuthorityReady());
        assertTrue(data.criticalAuthorityReady());
        assertTrue(data.attackSpeedAuthorityReady());
        assertTrue(data.movementAuthorityReady());
        assertTrue(data.healingDoneAuthorityReady());
        assertTrue(data.healingReceivedAuthorityReady());
        assertTrue(data.ultimateChargeAuthorityReady());
        assertTrue(data.affixes().stream().allMatch(
                OrdinaryEquipmentAffixCatalogData.AffixEntry::runtimeImplemented
        ));

        var weakPoint = data.affixes().stream()
                .filter(value -> value.id().equals(
                        "openworld_rpg:affix/weak_point_damage"
                ))
                .findFirst()
                .orElseThrow();
        assertTrue(weakPoint.runtimeImplemented());

        var negativeDuration = data.affixes().stream()
                .filter(value -> value.id().equals(
                        "openworld_rpg:affix/negative_status_duration_reduction"
                ))
                .findFirst()
                .orElseThrow();
        assertTrue(negativeDuration.runtimeImplemented());

        var potionFood = data.affixes().stream()
                .filter(value -> value.id().equals(
                        "openworld_rpg:affix/potion_food_effect_strength"
                ))
                .findFirst()
                .orElseThrow();
        assertTrue(potionFood.runtimeImplemented());

        var physical = data.affixes().stream()
                .filter(value -> value.id().equals(
                        "openworld_rpg:affix/physical_power"
                ))
                .findFirst()
                .orElseThrow();
        assertEquals(2.5, physical.min(), 0.000001);
        assertEquals(7.5, physical.max(), 0.000001);
        assertTrue(physical.runtimeImplemented());

        var critical = data.affixes().stream()
                .filter(value -> value.id().equals(
                        "openworld_rpg:affix/critical_chance"
                ))
                .findFirst()
                .orElseThrow();
        assertEquals("percent_tenth", critical.valueRule());
        assertTrue(critical.runtimeImplemented());

        var criticalDamage = data.affixes().stream()
                .filter(value -> value.id().equals(
                        "openworld_rpg:affix/critical_damage"
                ))
                .findFirst()
                .orElseThrow();
        assertTrue(criticalDamage.runtimeImplemented());

        var maxMana = data.affixes().stream()
                .filter(value -> value.id().equals(
                        "openworld_rpg:affix/max_mana"
                ))
                .findFirst()
                .orElseThrow();
        assertTrue(maxMana.runtimeImplemented());

        var manaCost = data.affixes().stream()
                .filter(value -> value.id().equals(
                        "openworld_rpg:affix/skill_mana_cost_reduction"
                ))
                .findFirst()
                .orElseThrow();
        assertTrue(manaCost.runtimeImplemented());

        var attackSpeed = data.affixes().stream()
                .filter(value -> value.id().equals(
                        "openworld_rpg:affix/attack_speed"
                ))
                .findFirst()
                .orElseThrow();
        assertEquals(2.0, attackSpeed.min(), 0.000001);
        assertEquals(6.0, attackSpeed.max(), 0.000001);
        assertTrue(attackSpeed.runtimeImplemented());

        var dodgeSprintCost = data.affixes().stream()
                .filter(value -> value.id().equals(
                        "openworld_rpg:affix/dodge_sprint_stamina_cost_reduction"
                ))
                .findFirst()
                .orElseThrow();
        assertEquals(3.0, dodgeSprintCost.min(), 0.000001);
        assertEquals(9.0, dodgeSprintCost.max(), 0.000001);
        assertTrue(dodgeSprintCost.runtimeImplemented());

        var healingDone = data.affixes().stream()
                .filter(value -> value.id().equals(
                        "openworld_rpg:affix/healing_done"
                ))
                .findFirst()
                .orElseThrow();
        assertEquals(4.0, healingDone.min(), 0.000001);
        assertEquals(12.0, healingDone.max(), 0.000001);
        assertTrue(healingDone.runtimeImplemented());

        var healingReceived = data.affixes().stream()
                .filter(value -> value.id().equals(
                        "openworld_rpg:affix/healing_received"
                ))
                .findFirst()
                .orElseThrow();
        assertEquals(4.0, healingReceived.min(), 0.000001);
        assertEquals(12.0, healingReceived.max(), 0.000001);
        assertTrue(healingReceived.runtimeImplemented());

        var ultimateCharge = data.affixes().stream()
                .filter(value -> value.id().equals(
                        "openworld_rpg:affix/ultimate_charge_gain"
                ))
                .findFirst()
                .orElseThrow();
        assertEquals(4.0, ultimateCharge.min(), 0.000001);
        assertEquals(10.0, ultimateCharge.max(), 0.000001);
        assertTrue(ultimateCharge.runtimeImplemented());

        var moveSpeed = data.affixes().stream()
                .filter(value -> value.id().equals(
                        "openworld_rpg:affix/movement_speed"
                ))
                .findFirst()
                .orElseThrow();
        assertEquals(1.0, moveSpeed.min(), 0.000001);
        assertEquals(3.5, moveSpeed.max(), 0.000001);
        assertTrue(moveSpeed.runtimeImplemented());
    }

    @Test
    void dataOwnedPrimaryCurveMatchesCanonicalReferencePoints() {
        var data = OrdinaryEquipmentAffixCatalogLoader.loadBundled();
        var curve = data.primaryCurve().toRuntimeCurve();

        assertEquals(
                new OrdinaryEquipmentAffixRoller.RawRange(1.0, 2.0),
                curve.range(1)
        );
        assertEquals(
                new OrdinaryEquipmentAffixRoller.RawRange(1.0, 3.0),
                curve.range(8)
        );
        assertEquals(
                new OrdinaryEquipmentAffixRoller.RawRange(2.0, 5.0),
                curve.range(20)
        );
        assertEquals(
                new OrdinaryEquipmentAffixRoller.RawRange(5.0, 11.0),
                curve.range(80)
        );
    }

    @Test
    void parameterizedAffixesAreNotFakedAsStaticCatalogEntries() {
        var data = OrdinaryEquipmentAffixCatalogLoader.loadBundled();

        assertTrue(data.affixes().stream().noneMatch(value ->
                value.id().equals(
                        "openworld_rpg:affix/weapon_family_power"
                )
        ));
        assertTrue(data.affixes().stream().noneMatch(value ->
                value.id().equals(
                        "openworld_rpg:affix/element_status_output"
                )
        ));
    }
}
