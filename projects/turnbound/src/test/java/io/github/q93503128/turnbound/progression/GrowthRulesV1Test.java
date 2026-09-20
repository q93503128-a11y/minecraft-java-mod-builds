package io.github.q93503128.turnbound.progression;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrowthRulesV1Test {
    @Test
    void enhancementCostCurveMatchesDocumentedZeroToTenGoldTargets() {
        assertEquals(4_000, total("T1"));
        assertEquals(9_000, total("T2"));
        assertEquals(18_000, total("T3"));
        assertEquals(35_000, total("T4"));
        assertEquals(10, GrowthRulesV1.maxEnhancement());
    }

    @Test
    void levelCurveIsEarlyFastLaterFlatterAndEndsInsideV1TargetBand() {
        double lv10 = GrowthRulesV1.characterLevelMultiplier(10);
        double lv30 = GrowthRulesV1.characterLevelMultiplier(30);
        double lv60 = GrowthRulesV1.characterLevelMultiplier(60);
        assertTrue(lv10 > 1.0);
        assertTrue(lv30 > lv10);
        assertTrue(lv60 > lv30);
        assertTrue((lv30 - lv10) > (lv60 - lv30) * 0.4);
        assertEquals(2.3, lv60, 0.000001);
    }

    private static int total(String tier) {
        int sum = 0;
        for (int i = 0; i < GrowthRulesV1.maxEnhancement(); i++) sum += GrowthRulesV1.enhancementCost(tier, i);
        return sum;
    }
}
