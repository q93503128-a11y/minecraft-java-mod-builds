package io.github.q93503128.turnbound.world;

/** Pure retry cadence for recovering an opening route snapshot resolved before live chunks finish loading. */
final class OpeningRouteRecoveryRules {
    static final long INTERVAL_TICKS = 80L;
    static final int MAX_ATTEMPTS = 12;

    private OpeningRouteRecoveryRules() {}

    static boolean due(boolean openingReady, int attempts, long gameTime, long retryAt) {
        return !openingReady
                && attempts < MAX_ATTEMPTS
                && gameTime >= retryAt;
    }
}
