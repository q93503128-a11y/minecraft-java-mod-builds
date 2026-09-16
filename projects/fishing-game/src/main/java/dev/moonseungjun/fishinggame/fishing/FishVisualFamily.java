package dev.moonseungjun.fishinggame.fishing;

public enum FishVisualFamily {
    SMALL,
    TALL,
    FAT,
    LONG,
    ANGLER,
    CYPRINID,
    PELAGIC,
    BREAM,
    CATFISH;

    public static FishVisualFamily forSpecies(String speciesId) {
        return switch (speciesId) {
            case "bluegill" -> TALL;
            case "mackerel" -> SMALL;
            case "catfish" -> CATFISH;
            case "oarfish", "ancient_sturgeon" -> LONG;
            case "angler" -> ANGLER;
            case "crucian", "carp", "golden_carp" -> CYPRINID;
            case "trout", "salmon", "tuna" -> PELAGIC;
            case "sea_bream" -> BREAM;
            default -> FAT;
        };
    }
}
