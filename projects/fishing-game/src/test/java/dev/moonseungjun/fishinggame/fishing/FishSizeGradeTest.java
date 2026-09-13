package dev.moonseungjun.fishinggame.fishing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FishSizeGradeTest {
    private static final FishSpecies TEST_SPECIES = new FishSpecies(
            "test_fish",
            "테스트 물고기",
            FishRarity.COMMON,
            FishingLocation.LAKESIDE,
            1.0,
            5.0,
            20.0,
            60.0,
            1.0f,
            10,
            10
    );

    @Test
    void classifiesCatchByCombinedWeightAndLengthPosition() {
        assertEquals(FishSizeGrade.STANDARD, FishSizeGrade.classify(TEST_SPECIES, 1400, 240));
        assertEquals(FishSizeGrade.LARGE, FishSizeGrade.classify(TEST_SPECIES, 3400, 440));
        assertEquals(FishSizeGrade.TROPHY, FishSizeGrade.classify(TEST_SPECIES, 4400, 540));
        assertEquals(FishSizeGrade.MONSTER, FishSizeGrade.classify(TEST_SPECIES, 4920, 592));
    }

    @Test
    void clampsMeasurementsOutsideConfiguredRange() {
        assertEquals(FishSizeGrade.STANDARD, FishSizeGrade.classify(TEST_SPECIES, 1, 1));
        assertEquals(FishSizeGrade.MONSTER, FishSizeGrade.classify(TEST_SPECIES, 9000, 1000));
    }
}
