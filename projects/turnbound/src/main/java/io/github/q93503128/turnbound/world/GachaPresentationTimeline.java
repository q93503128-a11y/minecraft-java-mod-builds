package io.github.q93503128.turnbound.world;

/**
 * Pure timing/intensity contract for the world-first summon reveal.
 *
 * <p>Rarity changes presentation intensity rather than extending the player's wait. Server actor timing and the
 * client overlay both consume this class so the 3D appearance, pose, name card and audio beats stay synchronized.</p>
 */
public final class GachaPresentationTimeline {
    public enum Phase { SIGNAL, SILHOUETTE, REVEAL, NAME, COMPLETE }

    public static final int SIGNAL_TICKS = 8;
    public static final int SILHOUETTE_TICKS = 8;
    public static final int REVEAL_TICK = SIGNAL_TICKS + SILHOUETTE_TICKS;
    public static final int NAME_TICK = REVEAL_TICK + 7;
    public static final int SLOT_TICKS = 40;

    private GachaPresentationTimeline() {}

    public static Phase phase(int slotTick) {
        int tick = Math.max(0, slotTick);
        if (tick < SIGNAL_TICKS) return Phase.SIGNAL;
        if (tick < REVEAL_TICK) return Phase.SILHOUETTE;
        if (tick < NAME_TICK) return Phase.REVEAL;
        if (tick < SLOT_TICKS) return Phase.NAME;
        return Phase.COMPLETE;
    }

    public static int intensity(int stars) {
        return switch (Math.max(1, Math.min(5, stars))) {
            case 5 -> 4;
            case 4 -> 3;
            case 3 -> 2;
            case 2 -> 1;
            default -> 0;
        };
    }

    public static int summaryTicks(int pullCount) {
        return pullCount <= 1 ? 38 : 86;
    }

    public static int totalTicks(int revealCount, int pullCount) {
        return Math.max(1, revealCount) * SLOT_TICKS + summaryTicks(pullCount);
    }
}
