package dev.moonseungjun.fishinggame.progression;

import java.util.List;

public final class FishingRods {
    private static final List<RodDefinition> RODS = List.of(
            new RodDefinition(0, "reed", "갈대 낚싯대", 0, 1.00f, 0.00f, 0.00f, 1.00f),
            new RodDefinition(1, "lake_pro", "호수 전문가", 550, 1.18f, 0.07f, 0.08f, 0.82f),
            new RodDefinition(2, "bluewater", "블루워터", 2600, 1.48f, 0.13f, 0.18f, 0.66f)
    );

    private FishingRods() {
    }

    public static RodDefinition byTier(int tier) {
        int index = Math.max(0, Math.min(RODS.size() - 1, tier));
        return RODS.get(index);
    }

    public static RodDefinition nextAfter(int tier) {
        int next = tier + 1;
        return next < RODS.size() ? RODS.get(next) : null;
    }

    public static List<RodDefinition> all() {
        return RODS;
    }
}
