package dev.moonseungjun.fishinggame.fishing;

import java.util.List;

public final class FishCatalog {
    private static final List<FishSpecies> SPECIES = List.of(
            new FishSpecies("bluegill", "블루길", FishRarity.COMMON, FishingLocation.LAKESIDE, 0.18, 0.85, 12, 28, 0.78f, 70, 22),
            new FishSpecies("crucian", "붕어", FishRarity.COMMON, FishingLocation.LAKESIDE, 0.25, 1.25, 15, 36, 0.82f, 62, 26),
            new FishSpecies("perch", "농어", FishRarity.UNCOMMON, FishingLocation.LAKESIDE, 0.35, 2.40, 18, 48, 0.92f, 45, 34),
            new FishSpecies("trout", "송어", FishRarity.UNCOMMON, FishingLocation.LAKESIDE, 0.55, 3.60, 24, 58, 1.02f, 38, 42),
            new FishSpecies("carp", "잉어", FishRarity.RARE, FishingLocation.LAKESIDE, 1.40, 8.20, 35, 82, 1.13f, 22, 68),
            new FishSpecies("largemouth", "큰입배스", FishRarity.RARE, FishingLocation.LAKESIDE, 1.10, 6.10, 30, 72, 1.18f, 18, 75),
            new FishSpecies("catfish", "메기", FishRarity.EPIC, FishingLocation.LAKESIDE, 2.20, 14.0, 42, 105, 1.31f, 9, 118),
            new FishSpecies("golden_carp", "황금 잉어", FishRarity.LEGENDARY, FishingLocation.LAKESIDE, 3.20, 11.5, 48, 92, 1.42f, 3, 210),

            new FishSpecies("mackerel", "고등어", FishRarity.COMMON, FishingLocation.COAST, 0.30, 1.30, 20, 42, 0.92f, 65, 32),
            new FishSpecies("sea_bream", "참돔", FishRarity.UNCOMMON, FishingLocation.COAST, 0.70, 5.40, 28, 68, 1.07f, 42, 52),
            new FishSpecies("salmon", "연어", FishRarity.RARE, FishingLocation.COAST, 1.50, 9.50, 45, 95, 1.22f, 21, 92),
            new FishSpecies("tuna", "참치", FishRarity.EPIC, FishingLocation.COAST, 8.0, 55.0, 80, 175, 1.48f, 8, 180),

            new FishSpecies("angler", "심해아귀", FishRarity.UNCOMMON, FishingLocation.DEEP_SEA, 1.2, 5.8, 35, 78, 1.18f, 44, 70),
            new FishSpecies("oarfish", "산갈치", FishRarity.RARE, FishingLocation.DEEP_SEA, 8.0, 38.0, 190, 520, 1.35f, 19, 145),
            new FishSpecies("ancient_sturgeon", "고대 철갑상어", FishRarity.LEGENDARY, FishingLocation.DEEP_SEA, 24.0, 120.0, 140, 330, 1.70f, 3, 390)
    );

    private FishCatalog() {
    }

    public static List<FishSpecies> all() {
        return SPECIES;
    }

    public static FishSpecies byId(String id) {
        return SPECIES.stream().filter(species -> species.id().equals(id)).findFirst().orElse(SPECIES.getFirst());
    }

    public static FishSpecies pick(FishingLocation location, double unitRoll, float luck) {
        List<FishSpecies> pool = SPECIES.stream().filter(species -> species.location() == location).toList();
        int total = pool.stream().mapToInt(species -> effectiveWeight(species, luck)).sum();
        int target = (int) Math.floor(Math.max(0.0, Math.min(Math.nextDown(1.0), unitRoll)) * total);
        int cursor = 0;
        for (FishSpecies species : pool) {
            cursor += effectiveWeight(species, luck);
            if (target < cursor) return species;
        }
        return pool.getLast();
    }

    public static FishSpecies pick(double unitRoll) {
        return pick(FishingLocation.LAKESIDE, unitRoll, 0.0f);
    }

    private static int effectiveWeight(FishSpecies species, float luck) {
        double rarityBoost = 1.0 + Math.max(0.0f, luck) * species.rarity().ordinal() * 1.8;
        return Math.max(1, (int) Math.round(species.selectionWeight() * rarityBoost));
    }
}
