package dev.moonseungjun.augmentationsurvival.augment;

import dev.moonseungjun.augmentationsurvival.core.AugmentTier;
import java.util.Objects;

/** Immutable runtime identity for one canonical augment roster entry. */
public record AugmentDefinition(
        String id,
        String canonicalNameKo,
        AugmentTier tier,
        int maxLevel
) {
    public AugmentDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(canonicalNameKo, "canonicalNameKo");
        Objects.requireNonNull(tier, "tier");
        if (id.isBlank() || !id.contains(":")) {
            throw new IllegalArgumentException("Augment id must be a namespaced stable id: " + id);
        }
        if (canonicalNameKo.isBlank()) {
            throw new IllegalArgumentException("Augment canonical name must not be blank: " + id);
        }
        if (maxLevel <= 0) {
            throw new IllegalArgumentException("Augment max level must be positive: " + id);
        }
    }

    public boolean isMaxed(int currentLevel) {
        return currentLevel >= maxLevel;
    }

    public double powerScore(int level) {
        if (level <= 0) {
            return 0.0;
        }
        return Math.min(level, maxLevel) * tier.powerWeightPerLevel();
    }
}
