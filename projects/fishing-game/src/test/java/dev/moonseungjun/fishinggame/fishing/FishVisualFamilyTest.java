package dev.moonseungjun.fishinggame.fishing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FishVisualFamilyTest {
    @Test
    void lakesideSpeciesUsePurposefulSilhouettes() {
        assertEquals(FishVisualFamily.TALL, FishVisualFamily.forSpecies("bluegill"));
        assertEquals(FishVisualFamily.FAT, FishVisualFamily.forSpecies("carp"));
        assertEquals(FishVisualFamily.FAT, FishVisualFamily.forSpecies("largemouth"));
        assertEquals(FishVisualFamily.LONG, FishVisualFamily.forSpecies("catfish"));
        assertEquals(FishVisualFamily.ANGLER, FishVisualFamily.forSpecies("angler"));
    }

    @Test
    void futureLargeLongFishDoNotFallBackToGenericFatShape() {
        assertEquals(FishVisualFamily.LONG, FishVisualFamily.forSpecies("oarfish"));
        assertEquals(FishVisualFamily.LONG, FishVisualFamily.forSpecies("ancient_sturgeon"));
    }

    @Test
    void smallStreamlinedFishStayInSmallFamily() {
        assertEquals(FishVisualFamily.SMALL, FishVisualFamily.forSpecies("mackerel"));
    }
}
