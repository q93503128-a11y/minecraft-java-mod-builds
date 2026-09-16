package dev.moonseungjun.fishinggame.fishing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FishVisualFamilyTest {
    @Test
    void lakesideSpeciesUsePurposefulSilhouettes() {
        assertEquals(FishVisualFamily.TALL, FishVisualFamily.forSpecies("bluegill"));
        assertEquals(FishVisualFamily.CYPRINID, FishVisualFamily.forSpecies("crucian"));
        assertEquals(FishVisualFamily.CYPRINID, FishVisualFamily.forSpecies("carp"));
        assertEquals(FishVisualFamily.CYPRINID, FishVisualFamily.forSpecies("golden_carp"));
        assertEquals(FishVisualFamily.FAT, FishVisualFamily.forSpecies("largemouth"));
        assertEquals(FishVisualFamily.CATFISH, FishVisualFamily.forSpecies("catfish"));
    }

    @Test
    void streamlinedAndCompressedSpeciesDoNotFallBackToGenericFatShape() {
        assertEquals(FishVisualFamily.PELAGIC, FishVisualFamily.forSpecies("trout"));
        assertEquals(FishVisualFamily.PELAGIC, FishVisualFamily.forSpecies("salmon"));
        assertEquals(FishVisualFamily.PELAGIC, FishVisualFamily.forSpecies("tuna"));
        assertEquals(FishVisualFamily.BREAM, FishVisualFamily.forSpecies("sea_bream"));
        assertEquals(FishVisualFamily.SMALL, FishVisualFamily.forSpecies("mackerel"));
    }

    @Test
    void rareLongAndAnglerFishKeepSpecializedFamilies() {
        assertEquals(FishVisualFamily.ANGLER, FishVisualFamily.forSpecies("angler"));
        assertEquals(FishVisualFamily.LONG, FishVisualFamily.forSpecies("oarfish"));
        assertEquals(FishVisualFamily.LONG, FishVisualFamily.forSpecies("ancient_sturgeon"));
    }
}
