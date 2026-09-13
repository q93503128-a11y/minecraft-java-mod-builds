package kr.moonseungjun.turnboundre.data;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import kr.moonseungjun.turnboundre.fixtures.ProductionDefinitionFixture;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class M6EquipmentDefinitionTest {
    @Test
    void productionEquipmentIsSmallDeterministicAndUsesMinecraftMaterials() throws IOException {
        DefinitionRegistry registry = ProductionDefinitionFixture.load().registry();

        assertEquals(3, registry.equipment().size());
        assertEquals("minecraft:iron_ingot", registry.equipment().get("turnbound_re:iron_bulwark").ingredientItem());
        assertEquals("minecraft:copper_ingot", registry.equipment().get("turnbound_re:copper_edge").ingredientItem());
        assertEquals("minecraft:gold_ingot", registry.equipment().get("turnbound_re:golden_heart").ingredientItem());
        registry.equipment().values().forEach(definition -> assertEquals(3, definition.maxLevel(), definition.id()));

        List<String> bonusFields = Arrays.stream(EquipmentDefinition.Bonus.class.getRecordComponents())
                .map(component -> component.getName()).toList();
        assertEquals(List.of("hpPercent", "atkPercent", "defPercent", "poisePercent"), bonusFields);
        assertFalse(bonusFields.contains("spdPercent"));
        assertFalse(bonusFields.contains("rarity"));
        assertFalse(bonusFields.contains("affixes"));
    }

    @Test
    void legacyDefinitionBundleWithoutEquipmentStillDecodes() {
        DefinitionBundle bundle = DefinitionBundle.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{}"))
                .getOrThrow();
        assertTrue(bundle.equipment().isEmpty());
        assertTrue(DefinitionRegistry.create(bundle).equipment().isEmpty());
    }

    @Test
    void validatorRejectsPowerCreepAndBrokenTierSequences() {
        EquipmentDefinition gap = new EquipmentDefinition(
                "turnbound_re:gap",
                "minecraft:iron_ingot",
                List.of(new EquipmentDefinition.Tier(2, 10, 1,
                        new EquipmentDefinition.Bonus(0, 1, 0, 0))));
        assertTrue(EquipmentDefinitionValidator.validate(List.of(gap)).stream()
                .anyMatch(error -> error.contains("consecutive")));

        EquipmentDefinition oversized = new EquipmentDefinition(
                "turnbound_re:oversized",
                "minecraft:iron_ingot",
                List.of(new EquipmentDefinition.Tier(1, 10, 1,
                        new EquipmentDefinition.Bonus(0, 21, 0, 0))));
        assertTrue(EquipmentDefinitionValidator.validate(List.of(oversized)).stream()
                .anyMatch(error -> error.contains("0..20")));

        EquipmentDefinition decreasing = new EquipmentDefinition(
                "turnbound_re:decreasing",
                "minecraft:iron_ingot",
                List.of(
                        new EquipmentDefinition.Tier(1, 20, 2, new EquipmentDefinition.Bonus(0, 4, 0, 0)),
                        new EquipmentDefinition.Tier(2, 10, 1, new EquipmentDefinition.Bonus(0, 3, 0, 0))));
        List<String> errors = EquipmentDefinitionValidator.validate(List.of(decreasing));
        assertTrue(errors.stream().anyMatch(error -> error.contains("coinCost must not decrease")));
        assertTrue(errors.stream().anyMatch(error -> error.contains("materialCount must not decrease")));
        assertTrue(errors.stream().anyMatch(error -> error.contains("stat bonuses must not decrease")));
    }
}
