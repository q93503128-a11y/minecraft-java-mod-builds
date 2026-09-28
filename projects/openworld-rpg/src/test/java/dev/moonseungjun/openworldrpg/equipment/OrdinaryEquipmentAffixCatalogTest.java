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
        assertEquals(20, data.implementedDefinitions().size());
        assertTrue(data.resourceAuthorityReady());
        assertTrue(data.criticalAuthorityReady());

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

        var moveSpeed = data.affixes().stream()
                .filter(value -> value.id().equals(
                        "openworld_rpg:affix/movement_speed"
                ))
                .findFirst()
                .orElseThrow();
        assertEquals(1.0, moveSpeed.min(), 0.000001);
        assertEquals(3.5, moveSpeed.max(), 0.000001);
        assertFalse(moveSpeed.runtimeImplemented());
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
