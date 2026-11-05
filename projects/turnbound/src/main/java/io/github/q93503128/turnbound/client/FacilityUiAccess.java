package io.github.q93503128.turnbound.client;

/** Client-only transient permission describing which physical New Drabyel service opened the current UI. */
final class FacilityUiAccess {
    enum Mode { NONE, MARKET, FORGE, TRAVEL, SUMMON }

    private static Mode mode = Mode.NONE;

    private FacilityUiAccess() {}

    static void applyHint(String hint) {
        mode = switch (hint == null ? "" : hint) {
            case "MARKET" -> Mode.MARKET;
            case "FORGE" -> Mode.FORGE;
            case "TRAVEL" -> Mode.TRAVEL;
            case "SUMMON" -> Mode.SUMMON;
            default -> Mode.NONE;
        };
    }

    static void clear() { mode = Mode.NONE; }
    static boolean market() { return mode == Mode.MARKET; }
    static boolean forge() { return mode == Mode.FORGE; }
    static boolean travel() { return mode == Mode.TRAVEL; }
    static boolean summon() { return mode == Mode.SUMMON; }
    static boolean archive() { return summon(); }
}
