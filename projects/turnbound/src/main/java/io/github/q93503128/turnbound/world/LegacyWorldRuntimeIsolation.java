package io.github.q93503128.turnbound.world;

/** Pure exclusivity rule for retaining the retired Aster runtime without letting it touch Drehmal/arbitrary worlds. */
final class LegacyWorldRuntimeIsolation {
    private LegacyWorldRuntimeIsolation() {}

    static boolean allowed(boolean externalActive, boolean legacyActive) {
        return legacyActive && !externalActive;
    }
}
