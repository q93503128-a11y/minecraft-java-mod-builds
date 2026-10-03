package dev.moonseungjun.openworldrpg.profession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import dev.moonseungjun.openworldrpg.combat.state.EquipmentCombatAffixKind;
import dev.moonseungjun.openworldrpg.gathering.R01GatheringRules;
import dev.moonseungjun.openworldrpg.inventory.ProjectItemGrade;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class R01SmithingBackendTest {
    @Test
    void canonicalRecipeTableMatchesR01ForgeCanon() {
        assertEquals(12, R01SmithingRecipe.values().length);

        assertEquals(
                Map.of(
                        R01GatheringRules.IRON_ORE, 6,
                        R01GatheringRules.HARDWOOD, 2
                ),
                R01SmithingRecipe.HEARTLAND_SWORD_REFINED
                        .materialCosts()
        );
        assertEquals(
                60L,
                R01SmithingRecipe.HEARTLAND_SWORD_REFINED
                        .goldCost()
        );
        assertEquals(
                ProjectItemGrade.REFINED,
                R01SmithingRecipe.QUARRY_MAUL_REFINED.grade()
        );
        assertEquals(
                5,
                R01SmithingRecipe.QUARRY_MAUL_REFINED.itemLevel()
        );

        assertEquals(
                2,
                R01SmithingRecipe.RIVERWOOD_BOW_SUPERIOR
                        .materialCosts()
                        .get(R01GatheringRules.VERDANT_CRYSTAL)
        );
        assertEquals(
                130L,
                R01SmithingRecipe.RIVERWOOD_BOW_SUPERIOR
                        .goldCost()
        );
        assertTrue(
                R01SmithingRecipe.WATCH_BUCKLER_SUPERIOR
                        .requiresVerdantDiscovery()
        );
        assertFalse(
                R01SmithingRecipe.WATCH_BUCKLER_REFINED
                        .requiresVerdantDiscovery()
        );
    }

    @Test
    void smithingUsesCanonicalEquipmentSellBackBaseline() {
        assertEquals(
                60L,
                R01SmithingRecipe.HEARTLAND_SWORD_REFINED
                        .unitSellValue()
        );
        assertEquals(
                52L,
                R01SmithingRecipe.WATCH_BUCKLER_REFINED
                        .unitSellValue()
        );
        assertEquals(
                125L,
                R01SmithingRecipe.HEARTLAND_SWORD_SUPERIOR
                        .unitSellValue()
        );
        assertEquals(
                112L,
                R01SmithingRecipe.WATCH_BUCKLER_SUPERIOR
                        .unitSellValue()
        );
    }

    @Test
    void materializedForgeOutputPreservesFixedAffixAndGrade() {
        var item = R01SmithingService.materialize(
                R01SmithingRecipe.HEARTLAND_SWORD_SUPERIOR,
                123456L
        );

        assertEquals(
                ProjectItemGrade.SUPERIOR,
                item.resolvedEquipmentGrade().orElseThrow()
        );
        assertEquals(1, item.quantity());
        assertEquals(125L, item.unitSellValue());
        assertTrue(
                item.equipmentProjection()
                        .orElseThrow()
                        .affixes()
                        .stream()
                        .anyMatch(affix ->
                                affix.kind()
                                        == EquipmentCombatAffixKind.STR
                        )
        );
        assertEquals(
                3,
                item.equipmentProjection()
                        .orElseThrow()
                        .affixes()
                        .size()
        );
    }

    @Test
    void allCanonicalForgeRecipesMaterializeWithTheirGradeAndAffixCount() {
        long seed = 1000L;
        for (R01SmithingRecipe recipe : R01SmithingRecipe.values()) {
            var item = R01SmithingService.materialize(
                    recipe,
                    seed++
            );
            assertEquals(
                    recipe.grade(),
                    item.resolvedEquipmentGrade().orElseThrow()
            );
            assertEquals(
                    recipe.grade() == ProjectItemGrade.REFINED ? 2 : 3,
                    item.equipmentProjection()
                            .orElseThrow()
                            .affixes()
                            .size()
            );
        }
    }

    @Test
    void superiorRecipeUnlockIsPermanentInSmithingState() {
        var state = R01SmithingState.initial();
        assertFalse(state.superiorRecipesUnlocked());

        var unlocked = state.unlockSuperiorRecipes();
        assertTrue(unlocked.superiorRecipesUnlocked());
        assertEquals(unlocked, unlocked.unlockSuperiorRecipes());

        var encoded = R01SmithingState.CODEC
                .encodeStart(JsonOps.INSTANCE, unlocked)
                .getOrThrow();
        var decoded = R01SmithingState.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();
        assertTrue(decoded.superiorRecipesUnlocked());
    }

    @Test
    void pendingSmithPersistsResolvedOutputAndDistinctRefinedEvidence() {
        String playerId = UUID.randomUUID().toString();
        var output = R01SmithingService.materialize(
                R01SmithingRecipe.RIVER_PIKE_REFINED,
                77L
        );
        var begin = R01SmithingState.initial().begin(
                playerId,
                R01SmithingRecipe.RIVER_PIKE_REFINED,
                output
        );
        var recorded = begin.state()
                .recordRefinedBaseCraft(
                        R01SmithingRecipe.RIVER_PIKE_REFINED
                                .baseId()
                );

        assertTrue(begin.created());
        assertEquals(output, begin.craft().output());
        assertEquals(1, recorded.refinedBaseCrafts().size());

        var encoded = R01SmithingState.CODEC
                .encodeStart(JsonOps.INSTANCE, recorded)
                .getOrThrow();
        var decoded = R01SmithingState.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();
        assertEquals(recorded, decoded);
    }
}
