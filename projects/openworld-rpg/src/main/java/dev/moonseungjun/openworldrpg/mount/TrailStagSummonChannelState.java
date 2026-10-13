package dev.moonseungjun.openworldrpg.mount;

/** Immutable server-side state for one Trail Stag summon channel. */
public record TrailStagSummonChannelState(
        long sequence,
        long startedAtTick,
        long hostileHpActivityTickAtStart
) {
    public TrailStagSummonChannelState {
        if (sequence < 0L || startedAtTick < 0L) {
            throw new IllegalArgumentException("Trail Stag channel sequence and start tick must be non-negative.");
        }
    }

    public long completesAtTick() {
        return Math.addExact(
                startedAtTick,
                TrailStagMountRules.SUMMON_CHANNEL_TICKS
        );
    }

    public boolean complete(long nowTick) {
        requireTick(nowTick);
        return nowTick >= completesAtTick();
    }

    public long remainingTicks(long nowTick) {
        requireTick(nowTick);
        return Math.max(0L, completesAtTick() - nowTick);
    }

    /** Any newer hostile HP exchange means hostile damage occurred after this channel began. */
    public boolean canceledByHostileActivity(long latestHostileHpActivityTick) {
        return latestHostileHpActivityTick > hostileHpActivityTickAtStart;
    }

    private static void requireTick(long tick) {
        if (tick < 0L) {
            throw new IllegalArgumentException("Game tick must be non-negative.");
        }
    }
}
