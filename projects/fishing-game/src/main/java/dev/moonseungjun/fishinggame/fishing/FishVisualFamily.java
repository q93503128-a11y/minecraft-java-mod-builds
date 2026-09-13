package dev.moonseungjun.fishinggame.fishing;

public enum FishVisualFamily {
    SMALL,
    TALL,
    FAT,
    LONG,
    ANGLER;

    public static FishVisualFamily forSpecies(String speciesId) {
        return switch (speciesId) {
            case "bluegill" -> TALL;
            case "mackerel" -> SMALL;
            case "catfish", "oarfish", "ancient_sturgeon" -> LONG;
            case "angler" -> ANGLER;
            default -> FAT;
        };
    }
}
