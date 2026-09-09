package dev.moonseungjun.fishinggame.fishing;

public enum FishRarity {
    COMMON("일반", 1.0),
    UNCOMMON("고급", 1.35),
    RARE("희귀", 1.9),
    EPIC("영웅", 3.1),
    LEGENDARY("전설", 6.0);

    private final String displayName;
    private final double valueMultiplier;

    FishRarity(String displayName, double valueMultiplier) {
        this.displayName = displayName;
        this.valueMultiplier = valueMultiplier;
    }

    public String displayName() {
        return displayName;
    }

    public double valueMultiplier() {
        return valueMultiplier;
    }
}
