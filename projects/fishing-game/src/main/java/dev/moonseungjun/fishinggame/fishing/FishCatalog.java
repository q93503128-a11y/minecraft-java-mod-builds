package dev.moonseungjun.fishinggame.fishing;

import java.util.List;

public final class FishCatalog {
    private static final List<FishSpecies> TECH_SLICE = List.of(
            new FishSpecies("bluegill", "블루길", 0.18, 0.85, 0.82f, 60),
            new FishSpecies("carp", "잉어", 1.4, 7.5, 1.00f, 30),
            new FishSpecies("catfish", "메기", 2.2, 14.0, 1.28f, 10)
    );

    private static final int TOTAL_WEIGHT = TECH_SLICE.stream().mapToInt(FishSpecies::selectionWeight).sum();

    private FishCatalog() {
    }

    public static List<FishSpecies> all() {
        return TECH_SLICE;
    }

    public static FishSpecies pick(double unitRoll) {
        double clamped = Math.max(0.0, Math.min(Math.nextDown(1.0), unitRoll));
        int target = (int) Math.floor(clamped * TOTAL_WEIGHT);
        int cursor = 0;

        for (FishSpecies species : TECH_SLICE) {
            cursor += species.selectionWeight();
            if (target < cursor) return species;
        }

        return TECH_SLICE.getLast();
    }
}
