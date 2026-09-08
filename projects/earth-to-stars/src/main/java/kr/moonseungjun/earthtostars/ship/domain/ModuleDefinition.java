package kr.moonseungjun.earthtostars.ship.domain;

import java.util.Objects;

public record ModuleDefinition(
        String id,
        ModuleCategory category,
        ModuleSlotType slotType,
        int sizeClass,
        double mass,
        double powerUse,
        double powerGeneration,
        double capacity
) {
    public ModuleDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("module id must not be blank");
        }
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(slotType, "slotType");
        if (sizeClass < 1) {
            throw new IllegalArgumentException("sizeClass must be >= 1");
        }
        requireFiniteNonNegative(mass, "mass");
        requireFiniteNonNegative(powerUse, "powerUse");
        requireFiniteNonNegative(powerGeneration, "powerGeneration");
        requireFiniteNonNegative(capacity, "capacity");
    }

    private static void requireFiniteNonNegative(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and >= 0");
        }
    }
}
