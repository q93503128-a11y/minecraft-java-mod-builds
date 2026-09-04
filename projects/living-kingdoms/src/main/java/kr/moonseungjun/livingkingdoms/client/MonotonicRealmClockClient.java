package kr.moonseungjun.livingkingdoms.client;

import net.minecraft.client.Minecraft;

/**
 * Presents the synchronized realm clock without visible client correction jitter.
 *
 * <p>Minecraft's client game-time sample can briefly stall or move backwards when an integrated
 * server catches up or resynchronizes. The kingdom HUD must never display that correction as time
 * travel. We accept forward server/client samples immediately and interpolate between them using a
 * monotonic clock, but cap prediction so a genuinely stalled server cannot drift indefinitely.</p>
 */
public final class MonotonicRealmClockClient {
    private static final long NANOS_PER_TICK = 50_000_000L;
    private static final long MAX_PREDICTION_TICKS = 200L;

    private static Object levelToken;
    private static long observedAnchor;
    private static long displayed;
    private static long anchorNanos;
    private static boolean initialized;

    private MonotonicRealmClockClient() {
    }

    public static long now(Minecraft minecraft) {
        if (minecraft.level == null) {
            reset();
            return 0L;
        }

        long observed = minecraft.level.getGameTime();
        long nanos = System.nanoTime();
        if (!initialized || levelToken != minecraft.level) {
            levelToken = minecraft.level;
            observedAnchor = observed;
            displayed = observed;
            anchorNanos = nanos;
            initialized = true;
            return displayed;
        }

        // A forward authoritative sample becomes the new anchor. Backward correction samples are
        // ignored for presentation so the visible kingdom clock is strictly monotonic.
        if (observed > observedAnchor) {
            observedAnchor = observed;
            anchorNanos = nanos;
        }

        long elapsedNanos = Math.max(0L, nanos - anchorNanos);
        long predictedTicks = Math.min(MAX_PREDICTION_TICKS, elapsedNanos / NANOS_PER_TICK);
        long candidate = Math.max(observed, observedAnchor + predictedTicks);
        if (candidate > displayed) displayed = candidate;
        return displayed;
    }

    static void reset() {
        levelToken = null;
        observedAnchor = 0L;
        displayed = 0L;
        anchorNanos = 0L;
        initialized = false;
    }

    /** Pure helper used by source/runtime diagnostics to prove correction samples cannot rewind. */
    static long monotonic(long displayedTime, long observedTime) {
        return Math.max(displayedTime, observedTime);
    }
}
