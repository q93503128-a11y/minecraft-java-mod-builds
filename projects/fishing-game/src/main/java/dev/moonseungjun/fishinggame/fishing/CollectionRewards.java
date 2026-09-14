package dev.moonseungjun.fishinggame.fishing;

import java.util.List;

import dev.moonseungjun.fishinggame.profile.FishRecord;

public final class CollectionRewards {
    private CollectionRewards() {
    }

    public static int discoveryReward(FishRarity rarity) {
        return switch (rarity) {
            case COMMON -> 12;
            case UNCOMMON -> 20;
            case RARE -> 35;
            case EPIC -> 65;
            case LEGENDARY -> 120;
        };
    }

    public static int locationCompletionReward(FishingLocation location) {
        return switch (location) {
            case LAKESIDE -> 180;
            case COAST -> 300;
            case DEEP_SEA -> 500;
        };
    }

    public static int speciesCount(FishingLocation location) {
        return (int) FishCatalog.all().stream()
                .filter(species -> species.location() == location)
                .count();
    }

    public static int discoveredCount(List<FishRecord> records, FishingLocation location) {
        return (int) FishCatalog.all().stream()
                .filter(species -> species.location() == location)
                .filter(species -> hasRecord(records, species.id()))
                .count();
    }

    public static int remainingCount(List<FishRecord> records, FishingLocation location) {
        return Math.max(0, speciesCount(location) - discoveredCount(records, location));
    }

    public static boolean locationComplete(List<FishRecord> records, FishingLocation location) {
        int total = speciesCount(location);
        return total > 0 && discoveredCount(records, location) >= total;
    }

    public static Reward rewardForCatch(
            List<FishRecord> beforeRecords,
            List<FishRecord> afterRecords,
            FishSpecies species
    ) {
        boolean firstDiscovery = !hasRecord(beforeRecords, species.id())
                && hasRecord(afterRecords, species.id());
        boolean locationCompleted = !locationComplete(beforeRecords, species.location())
                && locationComplete(afterRecords, species.location());

        return new Reward(
                firstDiscovery ? discoveryReward(species.rarity()) : 0,
                locationCompleted ? locationCompletionReward(species.location()) : 0
        );
    }

    private static boolean hasRecord(List<FishRecord> records, String speciesId) {
        return records.stream().anyMatch(record -> record.speciesId().equals(speciesId));
    }

    public record Reward(int discoveryCoins, int locationCompletionCoins) {
        public Reward {
            discoveryCoins = Math.max(0, discoveryCoins);
            locationCompletionCoins = Math.max(0, locationCompletionCoins);
        }

        public int totalCoins() {
            return discoveryCoins + locationCompletionCoins;
        }

        public boolean firstDiscovery() {
            return discoveryCoins > 0;
        }

        public boolean locationCompleted() {
            return locationCompletionCoins > 0;
        }
    }
}
