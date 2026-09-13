package dev.moonseungjun.fishinggame.fishing;

public enum FishingLocation {
    LAKESIDE("청람 호수", 0),
    COAST("갈매기 항구", 1),
    DEEP_SEA("심해 수로", 2);

    private final String displayName;
    private final int minRodTier;

    FishingLocation(String displayName, int minRodTier) {
        this.displayName = displayName;
        this.minRodTier = minRodTier;
    }

    public String displayName() {
        return displayName;
    }

    public int minRodTier() {
        return minRodTier;
    }
}
