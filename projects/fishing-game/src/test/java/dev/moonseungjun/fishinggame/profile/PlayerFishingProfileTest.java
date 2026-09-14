package dev.moonseungjun.fishinggame.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import dev.moonseungjun.fishinggame.fishing.CollectionRewards;
import dev.moonseungjun.fishinggame.fishing.FishCatalog;
import dev.moonseungjun.fishinggame.fishing.FishingLocation;
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
        assertEquals(CollectionRewards.discoveryReward(FishCatalog.byId("carp").rarity()), profile.coins());
    }

    @Test
    void repeatCatchDoesNotRepeatDiscoveryReward() {
        PlayerFishingProfile once = PlayerFishingProfile.empty()
                .addCatch(new CatchEntry("bluegill", 420, 210, 35));
        PlayerFishingProfile twice = once
                .addCatch(new CatchEntry("bluegill", 500, 230, 40));

        assertEquals(CollectionRewards.discoveryReward(FishCatalog.byId("bluegill").rarity()), once.coins());
        assertEquals(once.coins(), twice.coins());
    }

    @Test
    void completingLocationAwardsCompletionBonusExactlyOnce() {
        PlayerFishingProfile profile = PlayerFishingProfile.empty();
        for (var species : FishCatalog.all()) {
            if (species.location() != FishingLocation.LAKESIDE) continue;
            profile = profile.addCatch(new CatchEntry(
                    species.id(),
                    Math.max(1, (int) Math.round(species.minWeightKg() * 1000.0)),
                    Math.max(1, (int) Math.round(species.minLengthCm() * 10.0)),
                    1
            ));
        }

        int discoveryTotal = FishCatalog.all().stream()
                .filter(species -> species.location() == FishingLocation.LAKESIDE)
                .mapToInt(species -> CollectionRewards.discoveryReward(species.rarity()))
                .sum();
        assertEquals(
                discoveryTotal + CollectionRewards.locationCompletionReward(FishingLocation.LAKESIDE),
                profile.coins()
        );

        PlayerFishingProfile repeat = profile.addCatch(new CatchEntry("bluegill", 450, 220, 1));
        assertEquals(profile.coins(), repeat.coins());
    }

    @Test
    void sellingClearsBagButKeepsCollectionHistory() {
        PlayerFishingProfile profile = PlayerFishingProfile.empty()
                .addCatch(new CatchEntry("bluegill", 420, 210, 35))
                .addCatch(new CatchEntry("trout", 1600, 430, 75));

        int discoveryCoins = CollectionRewards.discoveryReward(FishCatalog.byId("bluegill").rarity())
                + CollectionRewards.discoveryReward(FishCatalog.byId("trout").rarity());
        PlayerFishingProfile sold = profile.sellAll();

        assertTrue(sold.catches().isEmpty());
        assertEquals(discoveryCoins + 110, sold.coins());
        assertEquals(2, sold.records().size());
        assertEquals(1, sold.recordFor("bluegill").orElseThrow().caughtCount());
        assertEquals(1, sold.recordFor("trout").orElseThrow().caughtCount());
    }

    @Test
    void legacyBagCanSeedRecordsWhenOldSaveHasNoRecordFieldWithoutRetroactiveRewards() {
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
        assertEquals(250, migrated.coins());
    }
}
