package io.github.q93503128.turnbound.progression;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CharacterGrowthRulesTest {
    @Test
    void v1UsesOneToSixtyLevelAxisWithoutRepeatRarityPromotion() {
        assertEquals(60, CharacterGrowthRules.levelCap(3));
        assertEquals(60, CharacterGrowthRules.levelCap(4));
        assertEquals(60, CharacterGrowthRules.levelCap(5));
        assertEquals(5, CharacterGrowthRules.initial("P02").currentStar());
        assertEquals(2, CharacterGrowthRules.initial("F03").currentStar());
        assertEquals(1.0, CharacterGrowthRules.promotionMultiplier(4, 6), 0.000001);
        assertThrows(UnsupportedOperationException.class, () -> CharacterGrowthRules.promotionCost(4));
    }

    @Test
    void awakeningNoLongerRequiresLegacyStarSixState() {
        var state = new CharacterGrowthRules.State(4, false, true, false).withAwakened();
        assertEquals(4, state.currentStar());
        assertEquals(true, state.awakened());
    }

    @Test
    void dataDrivenLevelCurveHitsV1ProductionTarget() {
        assertEquals(1.0, GrowthRulesV1.characterLevelMultiplier(1), 0.000001);
        assertEquals(2.3, GrowthRulesV1.characterLevelMultiplier(60), 0.000001);
    }
}
