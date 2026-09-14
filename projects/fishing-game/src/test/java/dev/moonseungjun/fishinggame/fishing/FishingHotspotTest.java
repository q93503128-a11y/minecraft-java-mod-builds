package dev.moonseungjun.fishinggame.fishing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FishingHotspotTest {
    @Test
    void classifiesHookPositionIntoLocationHotspots() {
        assertEquals(FishingHotspot.LAKESIDE_REEDS, FishingHotspot.at(FishingLocation.LAKESIDE, -12.0, 0.0));
        assertEquals(FishingHotspot.LAKESIDE_DEEP, FishingHotspot.at(FishingLocation.LAKESIDE, 0.0, -8.0));
        assertEquals(FishingHotspot.LAKESIDE_ROCKS, FishingHotspot.at(FishingLocation.LAKESIDE, 12.0, 0.0));

        assertEquals(FishingHotspot.COAST_BREAKWATER, FishingHotspot.at(FishingLocation.COAST, -22.0, -18.0));
        assertEquals(FishingHotspot.COAST_CHANNEL, FishingHotspot.at(FishingLocation.COAST, 0.0, -18.0));
        assertEquals(FishingHotspot.COAST_OUTER, FishingHotspot.at(FishingLocation.COAST, 22.0, -18.0));

        assertEquals(FishingHotspot.DEEP_LIGHTS, FishingHotspot.at(FishingLocation.DEEP_SEA, -24.0, 16.0));
        assertEquals(FishingHotspot.DEEP_TRENCH, FishingHotspot.at(FishingLocation.DEEP_SEA, 0.0, -24.0));
        assertEquals(FishingHotspot.DEEP_ANCIENT, FishingHotspot.at(FishingLocation.DEEP_SEA, 24.0, 16.0));
    }

    @Test
    void hotspotBiasRaisesPreferredSpeciesShareWithoutHardGatingOthers() {
        int neutralPreferred = 0;
        int hotspotPreferred = 0;
        int hotspotOther = 0;

        for (int i = 0; i < 1000; i++) {
            double roll = (i + 0.5) / 1000.0;
            FishSpecies neutral = FishCatalog.pick(FishingLocation.LAKESIDE, roll, 0.0f);
            FishSpecies biased = FishCatalog.pick(
                    FishingLocation.LAKESIDE,
                    roll,
                    0.0f,
                    FishingHotspot.LAKESIDE_REEDS
            );
            if (FishingHotspot.LAKESIDE_REEDS.prefers(neutral.id())) neutralPreferred++;
            if (FishingHotspot.LAKESIDE_REEDS.prefers(biased.id())) hotspotPreferred++;
            else hotspotOther++;
        }

        assertTrue(hotspotPreferred > neutralPreferred);
        assertTrue(hotspotOther > 0, "hotspots must bias rather than hard-gate the rest of the pool");
    }

    @Test
    void everyCatalogSpeciesHasARecommendedHotspotInItsOwnLocation() {
        for (FishSpecies species : FishCatalog.all()) {
            FishingHotspot hotspot = FishingHotspot.primaryForSpecies(species.id());
            assertEquals(species.location(), hotspot.location());
            assertTrue(hotspot.prefers(species.id()));
        }
    }
}
