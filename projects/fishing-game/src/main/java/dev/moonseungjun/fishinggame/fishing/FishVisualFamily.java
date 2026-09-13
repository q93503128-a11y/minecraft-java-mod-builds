package dev.moonseungjun.fishinggame.fishing;

public enum FishVisualFamily {
    SMALL,
    FAT,
    LONG;

    public static FishVisualFamily forSpecies(String speciesId) {
        return switch (speciesId) {
            case "bluegill", "mackerel" -> SMALL;
            case "catfish", "oarfish", "ancient_sturgeon", "angler" -> LONG;
            default -> FAT;
        };
    }
}
