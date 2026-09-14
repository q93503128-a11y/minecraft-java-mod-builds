package dev.moonseungjun.fishinggame.fishing;

public enum FishFightStyle {
    STEADY("꾸준한 힘싸움", 0.90f, 1.15f, 1.12f, 0.92f, 0.88f, 0.35f),
    DARTER("연속 질주", 1.00f, 0.72f, 0.70f, 1.24f, 1.34f, 0.22f),
    DIVER("깊은 잠수", 1.08f, 1.00f, 0.88f, 0.88f, 0.82f, 1.20f),
    BRUISER("묵직한 버팀", 1.22f, 1.30f, 1.16f, 0.74f, 0.68f, 0.72f),
    ERRATIC("불규칙 난동", 0.98f, 0.82f, 0.76f, 1.34f, 1.48f, 0.55f);

    private final String displayName;
    private final float burstStrengthMultiplier;
    private final float burstDurationMultiplier;
    private final float burstCooldownMultiplier;
    private final float lateralRangeMultiplier;
    private final float orbitSpeedMultiplier;
    private final float diveMultiplier;

    FishFightStyle(
            String displayName,
            float burstStrengthMultiplier,
            float burstDurationMultiplier,
            float burstCooldownMultiplier,
            float lateralRangeMultiplier,
            float orbitSpeedMultiplier,
            float diveMultiplier
    ) {
        this.displayName = displayName;
        this.burstStrengthMultiplier = burstStrengthMultiplier;
        this.burstDurationMultiplier = burstDurationMultiplier;
        this.burstCooldownMultiplier = burstCooldownMultiplier;
        this.lateralRangeMultiplier = lateralRangeMultiplier;
        this.orbitSpeedMultiplier = orbitSpeedMultiplier;
        this.diveMultiplier = diveMultiplier;
    }

    public String displayName() {
        return displayName;
    }

    public float burstStrength(float baseStrength) {
        return Math.max(0.25f, Math.min(1.80f, baseStrength * burstStrengthMultiplier));
    }

    public int burstDurationTicks(int baseTicks) {
        return Math.max(3, Math.round(baseTicks * burstDurationMultiplier));
    }

    public int burstCooldownTicks(int baseTicks, int randomExtraTicks) {
        int raw = Math.max(1, baseTicks) + Math.max(0, randomExtraTicks);
        return Math.max(14, Math.round(raw * burstCooldownMultiplier));
    }

    public float lateralRangeMultiplier() {
        return lateralRangeMultiplier;
    }

    public float orbitSpeedMultiplier() {
        return orbitSpeedMultiplier;
    }

    public float diveMultiplier() {
        return diveMultiplier;
    }

    public static FishFightStyle forSpecies(String speciesId) {
        return switch (speciesId) {
            case "bluegill", "largemouth", "golden_carp" -> ERRATIC;
            case "perch", "trout", "mackerel", "salmon", "tuna" -> DARTER;
            case "catfish", "angler", "oarfish" -> DIVER;
            case "carp", "ancient_sturgeon" -> BRUISER;
            case "crucian", "sea_bream" -> STEADY;
            default -> STEADY;
        };
    }
}
