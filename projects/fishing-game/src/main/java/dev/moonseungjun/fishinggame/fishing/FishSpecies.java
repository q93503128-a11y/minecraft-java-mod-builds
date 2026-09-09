package dev.moonseungjun.fishinggame.fishing;

public record FishSpecies(
        String id,
        String displayName,
        FishRarity rarity,
        FishingLocation location,
        double minWeightKg,
        double maxWeightKg,
        double minLengthCm,
        double maxLengthCm,
        float resistance,
        int selectionWeight,
        int baseValue
) {
    public FishSpecies {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id must not be blank");
        if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("displayName must not be blank");
        if (rarity == null || location == null) throw new IllegalArgumentException("rarity/location required");
        if (minWeightKg <= 0 || maxWeightKg < minWeightKg) throw new IllegalArgumentException("invalid weight range");
        if (minLengthCm <= 0 || maxLengthCm < minLengthCm) throw new IllegalArgumentException("invalid length range");
        if (resistance <= 0) throw new IllegalArgumentException("resistance must be positive");
        if (selectionWeight <= 0 || baseValue <= 0) throw new IllegalArgumentException("weights/value must be positive");
    }

    public double rollWeight(double unitRoll) {
        double clamped = Math.max(0.0, Math.min(1.0, unitRoll));
        return minWeightKg + (maxWeightKg - minWeightKg) * clamped;
    }

    public double rollLength(double unitRoll) {
        double clamped = Math.max(0.0, Math.min(1.0, unitRoll));
        return minLengthCm + (maxLengthCm - minLengthCm) * clamped;
    }

    public int valueFor(double weightKg) {
        double average = (minWeightKg + maxWeightKg) * 0.5;
        double sizeFactor = Math.max(0.55, weightKg / average);
        return Math.max(1, (int) Math.round(baseValue * rarity.valueMultiplier() * sizeFactor));
    }
}
