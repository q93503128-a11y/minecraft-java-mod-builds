package dev.moonseungjun.fishinggame.fishing;

public enum FishSizeGrade {
    STANDARD("일반"),
    LARGE("대형"),
    TROPHY("트로피"),
    MONSTER("괴물급");

    private final String displayName;

    FishSizeGrade(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static FishSizeGrade classify(FishSpecies species, int weightGrams, int lengthMm) {
        double weightKg = Math.max(0.0, weightGrams / 1000.0);
        double lengthCm = Math.max(0.0, lengthMm / 10.0);
        double weightScore = normalize(weightKg, species.minWeightKg(), species.maxWeightKg());
        double lengthScore = normalize(lengthCm, species.minLengthCm(), species.maxLengthCm());
        double score = weightScore * 0.65 + lengthScore * 0.35;

        if (score >= 0.93) return MONSTER;
        if (score >= 0.78) return TROPHY;
        if (score >= 0.55) return LARGE;
        return STANDARD;
    }

    private static double normalize(double value, double min, double max) {
        if (max <= min) return 1.0;
        return Math.max(0.0, Math.min(1.0, (value - min) / (max - min)));
    }
}
