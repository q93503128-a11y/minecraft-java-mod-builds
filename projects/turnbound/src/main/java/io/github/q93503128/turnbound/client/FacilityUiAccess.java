package io.github.q93503128.turnbound.client;

/** Client-only transient permission describing which physical Radia facility opened the current UI. */
final class FacilityUiAccess {
    enum Mode { NONE, ARCHIVE, MARKET, FORGE }

    private static Mode mode = Mode.NONE;

    private FacilityUiAccess() {}

    static void applyHint(String hint) {
        mode = switch (hint == null ? "" : hint) {
            case "ARCHIVE" -> Mode.ARCHIVE;
            case "MARKET" -> Mode.MARKET;
            case "FORGE" -> Mode.FORGE;
            default -> Mode.NONE;
        };
    }

    static void clear() { mode = Mode.NONE; }
    static boolean archive() { return mode == Mode.ARCHIVE; }
    static boolean market() { return mode == Mode.MARKET; }
    static boolean forge() { return mode == Mode.FORGE; }
}
