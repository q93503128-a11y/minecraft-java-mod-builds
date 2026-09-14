package dev.moonseungjun.fishinggame.fishing;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FishFightStyleTest {
    @Test
    void dartersBurstMoreOftenThanSteadyFish() {
        int darter = FishFightStyle.DARTER.burstCooldownTicks(40, 10);
        int steady = FishFightStyle.STEADY.burstCooldownTicks(40, 10);
        assertTrue(darter < steady);
    }

    @Test
    void bruisersPullLongerAndHarderThanDarters() {
        assertTrue(FishFightStyle.BRUISER.burstStrength(1.0f) > FishFightStyle.DARTER.burstStrength(1.0f));
        assertTrue(FishFightStyle.BRUISER.burstDurationTicks(10) > FishFightStyle.DARTER.burstDurationTicks(10));
    }

    @Test
    void diversHaveTheStrongestDownwardMotionBias() {
        assertTrue(FishFightStyle.DIVER.diveMultiplier() > FishFightStyle.DARTER.diveMultiplier());
        assertTrue(FishFightStyle.DIVER.diveMultiplier() > FishFightStyle.STEADY.diveMultiplier());
    }

    @Test
    void representativeSpeciesKeepDistinctFightIdentities() {
        assertSame(FishFightStyle.STEADY, FishFightStyle.forSpecies("crucian"));
        assertSame(FishFightStyle.DARTER, FishFightStyle.forSpecies("tuna"));
        assertSame(FishFightStyle.DIVER, FishFightStyle.forSpecies("catfish"));
        assertSame(FishFightStyle.BRUISER, FishFightStyle.forSpecies("ancient_sturgeon"));
        assertSame(FishFightStyle.ERRATIC, FishFightStyle.forSpecies("golden_carp"));
    }

    @Test
    void everyCatalogSpeciesHasAFightStyle() {
        for (FishSpecies species : FishCatalog.all()) {
            assertNotNull(FishFightStyle.forSpecies(species.id()));
        }
    }
}
