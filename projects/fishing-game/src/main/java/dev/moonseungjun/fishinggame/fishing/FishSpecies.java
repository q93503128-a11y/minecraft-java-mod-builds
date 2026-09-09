package dev.moonseungjun.fishinggame.fishing;

public record FishSpecies(
        String id,
        String displayName,
        double minWeightKg,
        double maxWeightKg,
        float resistance,
        int selectionWeight
) {
    public FishSpecies {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id must not be blank");
        if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("displayName must not be blank");
        if (minWeightKg <= 0 || maxWeightKg < minWeightKg) throw new IllegalArgumentException("invalid weight range");
        if (resistance <= 0) throw new IllegalArgumentException("resistance must be positive");
        if (selectionWeight <= 0) throw new IllegalArgumentException("selectionWeight must be positive");
    }

    public double rollWeight(double unitRoll) {
        double clamped = Math.max(0.0, Math.min(1.0, unitRoll));
        return minWeightKg + (maxWeightKg - minWeightKg) * clamped;
    }
}
