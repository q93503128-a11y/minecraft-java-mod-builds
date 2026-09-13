package dev.moonseungjun.fishinggame.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class PlayerFishingProfileTest {
    @Test
    void catchesBuildPersistentSpeciesRecordsAndPersonalBests() {
        CatchEntry first = new CatchEntry("carp", 3100, 540, 120);
        CatchEntry heavier = new CatchEntry("carp", 5200, 510, 180);
        CatchEntry longer = new CatchEntry("carp", 4500, 680, 160);

        PlayerFishingProfile profile = PlayerFishingProfile.empty()
                .addCatch(first)
                .addCatch(heavier)
                .addCatch(longer);

        FishRecord record = profile.recordFor("carp").orElseThrow();
        assertEquals(3, record.caughtCount());
        assertEquals(5200, record.bestWeightGrams());
        assertEquals(680, record.bestLengthMm());
        assertEquals(3, profile.catches().size());
    }

    @Test
    void sellingClearsBagButKeepsCollectionHistory() {
        PlayerFishingProfile profile = PlayerFishingProfile.empty()
                .addCatch(new CatchEntry("bluegill", 420, 210, 35))
                .addCatch(new CatchEntry("trout", 1600, 430, 75));

        PlayerFishingProfile sold = profile.sellAll();

        assertTrue(sold.catches().isEmpty());
        assertEquals(110, sold.coins());
        assertEquals(2, sold.records().size());
        assertEquals(1, sold.recordFor("bluegill").orElseThrow().caughtCount());
        assertEquals(1, sold.recordFor("trout").orElseThrow().caughtCount());
    }

    @Test
    void legacyBagCanSeedRecordsWhenOldSaveHasNoRecordField() {
        PlayerFishingProfile migrated = new PlayerFishingProfile(
                250,
                1,
                List.of(
                        new CatchEntry("perch", 900, 330, 50),
                        new CatchEntry("perch", 1200, 310, 58)
                ),
                List.of()
        );

        FishRecord record = migrated.recordFor("perch").orElseThrow();
        assertEquals(2, record.caughtCount());
        assertEquals(1200, record.bestWeightGrams());
        assertEquals(330, record.bestLengthMm());
    }
}
