package io.github.q93503128.turnbound.progression;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CharacterGrowthRulesTest {
    @Test
    void canonicalPromotionCostsCapsAndCumulativeMultiplierAreStable() {
        assertEquals(20, CharacterGrowthRules.promotionCost(1));
        assertEquals(50, CharacterGrowthRules.promotionCost(2));
        assertEquals(120, CharacterGrowthRules.promotionCost(3));
        assertEquals(250, CharacterGrowthRules.promotionCost(4));
        assertEquals(500, CharacterGrowthRules.promotionCost(5));
        assertEquals(60, CharacterGrowthRules.levelCap(6));
        assertEquals(1.232, CharacterGrowthRules.promotionMultiplier(4, 6), 0.000001);
    }

    @Test
    void initialStateUsesNativeStarAndAwakeningCannotExistBelowSixStars() {
        assertEquals(5, CharacterGrowthRules.initial("P02").currentStar());
        assertEquals(2, CharacterGrowthRules.initial("F03").currentStar());
        assertThrows(IllegalArgumentException.class,
                () -> new CharacterGrowthRules.State(5, true, true, true));
    }
}
