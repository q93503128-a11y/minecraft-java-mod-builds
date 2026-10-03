package dev.moonseungjun.openworldrpg.profession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import dev.moonseungjun.openworldrpg.gathering.R01GatheringRules;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class R01ProfessionCraftingStateTest {
    @Test
    void masteryUsesFiveRankInsightThresholdsAndDeduplicatesFlags() {
        var state = ProfessionMasteryState.initial();
        assertEquals(
                1,
                state.rank(ProfessionMasteryState.Profession.ALCHEMY)
        );

        for (int i = 0; i < 4; i++) {
            state = state.awardInsightOnce(
                    ProfessionMasteryState.Profession.ALCHEMY,
                    "openworld_rpg:test/alchemy_insight_" + i
            );
        }
        assertEquals(4, state.insights(
                ProfessionMasteryState.Profession.ALCHEMY
        ));
        assertEquals(
                2,
                state.rank(ProfessionMasteryState.Profession.ALCHEMY)
        );

        var duplicate = state.awardInsightOnce(
                ProfessionMasteryState.Profession.ALCHEMY,
                "openworld_rpg:test/alchemy_insight_3"
        );
        assertSame(state, duplicate);

        assertEquals(1, ProfessionMasteryState.rankForInsights(3));
        assertEquals(2, ProfessionMasteryState.rankForInsights(4));
        assertEquals(3, ProfessionMasteryState.rankForInsights(9));
        assertEquals(4, ProfessionMasteryState.rankForInsights(15));
        assertEquals(5, ProfessionMasteryState.rankForInsights(22));
        assertEquals(8, ProfessionMasteryState.feeReductionPercent(5));
    }

    @Test
    void canonicalR01RecipesPreserveCurrentCostsAndOutputCategories() {
        assertEquals(
                Map.of(R01GatheringRules.HEALING_HERB, 2),
                R01CraftingRecipe.HEALING_POTION.unitMaterialCosts()
        );
        assertEquals(5L, R01CraftingRecipe.HEALING_POTION.unitGoldFee());
        assertEquals(8L, R01CraftingRecipe.FOCUS_DRAUGHT.unitGoldFee());
        assertEquals(10L, R01CraftingRecipe.CLEANSING_TONIC.unitGoldFee());

        assertEquals(
                Map.of(
                        R01CraftingRecipe.LOUXIA_MEAT, 2,
                        R01GatheringRules.HEALING_HERB, 1
                ),
                R01CraftingRecipe.HERBED_LOUXIA_ROAST.unitMaterialCosts()
        );
        assertEquals(
                Map.of(
                        R01CraftingRecipe.LOUXIA_MEAT, 1,
                        R01GatheringRules.HEALING_HERB, 1
                ),
                R01CraftingRecipe.TRAIL_SKEWERS.unitMaterialCosts()
        );
        assertEquals(
                Map.of(
                        R01CraftingRecipe.LOUXIA_MEAT, 1,
                        R01CraftingRecipe.LOUXIA_GLOW, 1,
                        R01GatheringRules.HEALING_HERB, 1
                ),
                R01CraftingRecipe.GLOW_BROTH.unitMaterialCosts()
        );
        assertEquals(20, R01CraftingRecipe.HEALING_POTION.outputUnit().stackCap());
        assertEquals(50, R01CraftingRecipe.GLOW_BROTH.outputUnit().stackCap());
        assertEquals(
                10,
                R01CraftingRecipe.FOCUS_DRAUGHT
                        .materialCostsFor(5)
                        .values()
                        .stream()
                        .mapToInt(Integer::intValue)
                        .sum()
        );
        assertEquals(40L, R01CraftingRecipe.FOCUS_DRAUGHT.goldCostFor(5));
    }

    @Test
    void pendingCraftHasStableMonotonicIdentityAndCodecRoundTrip() {
        String playerId = UUID.randomUUID().toString();
        var first = R01CraftingState.initial().begin(
                playerId,
                R01CraftingRecipe.GLOW_BROTH.id(),
                5,
                0L
        );
        var retry = first.state().begin(
                playerId,
                R01CraftingRecipe.HEALING_POTION.id(),
                1,
                5L
        );

        assertTrue(first.created());
        assertFalse(retry.created());
        assertEquals(first.craft(), retry.craft());
        assertEquals(1L, first.state().nextCraftSerial());

        var encoded = R01CraftingState.CODEC
                .encodeStart(JsonOps.INSTANCE, first.state())
                .getOrThrow();
        var decoded = R01CraftingState.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();
        assertEquals(first.state(), decoded);
        assertEquals(
                R01CraftingState.CraftContext.SETTLEMENT,
                decoded.pending().orElseThrow().context()
        );

        var camp = R01CraftingState.initial().begin(
                playerId,
                R01CraftingRecipe.TRAIL_SKEWERS.id(),
                2,
                0L,
                R01CraftingState.CraftContext.CAMP
        );
        var campEncoded = R01CraftingState.CODEC
                .encodeStart(JsonOps.INSTANCE, camp.state())
                .getOrThrow();
        var campDecoded = R01CraftingState.CODEC
                .parse(JsonOps.INSTANCE, campEncoded)
                .getOrThrow();
        assertEquals(
                R01CraftingState.CraftContext.CAMP,
                campDecoded.pending().orElseThrow().context()
        );
        assertFalse(
                campDecoded.pending().orElseThrow()
                        .context()
                        .mayUseMaterialVault()
        );

        var second = first.state()
                .clearPending(first.craft().transactionId())
                .begin(
                        playerId,
                        R01CraftingRecipe.HEALING_POTION.id(),
                        1,
                        5L
                );
        assertEquals(2L, second.state().nextCraftSerial());
        assertFalse(
                first.craft().transactionId()
                        .equals(second.craft().transactionId())
        );
    }

    @Test
    void masteryStateSurvivesCodecRoundTrip() {
        var original = ProfessionMasteryState.initial()
                .awardInsightOnce(
                        ProfessionMasteryState.Profession.COOKING,
                        R01CraftingRecipe.GLOW_BROTH.insightFlag()
                );
        var encoded = ProfessionMasteryState.CODEC
                .encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow();
        var decoded = ProfessionMasteryState.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();
        assertEquals(original, decoded);
    }
}
