package dev.moonseungjun.fishinggame.fishing;

public enum FishingLocation {
    LAKESIDE("청람 호수"),
    COAST("갈매기 항구"),
    DEEP_SEA("심해 수로");

    private final String displayName;

    FishingLocation(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
