package dev.moonseungjun.augmentationsurvival.core;

import java.util.Locale;
import java.util.Optional;

/** Canonical six-tier progression order shared by cores and augment definitions. */
public enum AugmentTier {
    COMMON("common", 1.0),
    UNCOMMON("uncommon", 1.5),
    RARE("rare", 2.0),
    EPIC("epic", 3.0),
    LEGENDARY("legendary", 5.0),
    MYTHIC("mythic", 8.0);

    private final String id;
    private final double powerWeightPerLevel;

    AugmentTier(String id, double powerWeightPerLevel) {
        this.id = id;
        this.powerWeightPerLevel = powerWeightPerLevel;
    }

    public String id() {
        return id;
    }

    public double powerWeightPerLevel() {
        return powerWeightPerLevel;
    }

    public Optional<AugmentTier> next() {
        int nextOrdinal = ordinal() + 1;
        return nextOrdinal < values().length ? Optional.of(values()[nextOrdinal]) : Optional.empty();
    }

    public static AugmentTier fromId(String id) {
        String normalized = id.toLowerCase(Locale.ROOT);
        for (AugmentTier tier : values()) {
            if (tier.id.equals(normalized)) {
                return tier;
            }
        }
        throw new IllegalArgumentException("Unknown augment tier: " + id);
    }
}
