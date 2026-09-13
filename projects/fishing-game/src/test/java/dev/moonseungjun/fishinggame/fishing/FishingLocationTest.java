package dev.moonseungjun.fishinggame.fishing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class FishingLocationTest {
    @Test
    void locationsUnlockWithRodProgression() {
        assertEquals(0, FishingLocation.LAKESIDE.minRodTier());
        assertEquals(1, FishingLocation.COAST.minRodTier());
        assertEquals(2, FishingLocation.DEEP_SEA.minRodTier());
    }

    @Test
    void everyLocationHasCatchableSpecies() {
        for (FishingLocation location : FishingLocation.values()) {
            assertFalse(FishCatalog.all().stream().filter(species -> species.location() == location).toList().isEmpty());
        }
    }
}
