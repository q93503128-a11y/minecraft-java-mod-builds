package dev.moonseungjun.openworldrpg.combat.runtime;

/** Server-owned transient Warrior Momentum / counter / hyperarmor state. */
public final class WarriorMomentumRuntimeState {
    public static final int MAX_PIPS = 3;
    public static final long EXPIRY_TICKS = 140L;
    public static final long BASIC_GAIN_ICD_TICKS = 16L;

    private int pips;
    private long lastQualifyingActivityTick = Long.MIN_VALUE / 4;
    private long nextBasicGainAllowedTick = Long.MIN_VALUE / 4;
    private long lastBasicCyclePublicationTick = Long.MIN_VALUE / 4;
    private long counterUntilTick = Long.MIN_VALUE / 4;
    private long hyperarmorUntilTick = Long.MIN_VALUE / 4;
    private double hyperarmorMultiplier = 1.0;

    public int pips(long nowTick) {
        refresh(nowTick);
        return pips;
    }

    public double poiseOutputMultiplier(long nowTick) {
        return 1.0 + 0.05 * pips(nowTick);
    }

    public double ordinaryHitStaggerTakenMultiplier(long nowTick) {
        return pips(nowTick) > 0 ? 0.90 : 1.0;
    }

    public boolean recordMeleeBasicHit(
            boolean cycleFinisher,
            long nowTick
    ) {
        refresh(nowTick);
        lastQualifyingActivityTick = nowTick;
        if (!cycleFinisher || nowTick < nextBasicGainAllowedTick) {
            return false;
        }
        nextBasicGainAllowedTick = Math.addExact(
                nowTick,
                BASIC_GAIN_ICD_TICKS
        );
        int before = pips;
        pips = Math.min(MAX_PIPS, pips + 1);
        return pips != before;
    }

    public boolean claimBasicCyclePublication(long nowTick) {
        refresh(nowTick);
        if (lastBasicCyclePublicationTick == nowTick) {
            return false;
        }
        lastBasicCyclePublicationTick = nowTick;
        return true;
    }

    public void recordPerfectGuardActivity(long nowTick) {
        refresh(nowTick);
        lastQualifyingActivityTick = nowTick;
    }

    public boolean recordSuccessfulCounter(long nowTick) {
        refresh(nowTick);
        lastQualifyingActivityTick = nowTick;
        int before = pips;
        pips = Math.min(MAX_PIPS, pips + 1);
        return pips != before;
    }

    public boolean recordEliteBossPoiseBreak(long nowTick) {
        refresh(nowTick);
        lastQualifyingActivityTick = nowTick;
        int before = pips;
        pips = Math.min(MAX_PIPS, pips + 2);
        return pips != before;
    }

    public void recordMeleeActiveHit(long nowTick) {
        refresh(nowTick);
        lastQualifyingActivityTick = nowTick;
    }

    public boolean consumeSpenderIfFull(long nowTick) {
        refresh(nowTick);
        if (pips < MAX_PIPS) {
            return false;
        }
        pips = 0;
        return true;
    }

    public void beginCounter(long nowTick, long durationTicks) {
        if (durationTicks <= 0L) {
            throw new IllegalArgumentException(
                    "Counter duration must be positive."
            );
        }
        counterUntilTick = Math.addExact(nowTick, durationTicks);
    }

    public boolean consumeCounterWindow(long nowTick) {
        refresh(nowTick);
        if (nowTick >= counterUntilTick) {
            return false;
        }
        counterUntilTick = Long.MIN_VALUE / 4;
        return true;
    }

    public boolean counterActive(long nowTick) {
        refresh(nowTick);
        return nowTick < counterUntilTick;
    }

    public void beginHyperarmor(
            double multiplier,
            long nowTick,
            long durationTicks
    ) {
        if (!Double.isFinite(multiplier)
                || multiplier < 1.0
                || durationTicks <= 0L) {
            throw new IllegalArgumentException(
                    "Invalid Warrior hyperarmor window."
            );
        }
        hyperarmorMultiplier = multiplier;
        hyperarmorUntilTick = Math.addExact(nowTick, durationTicks);
    }

    public double hyperarmorMultiplier(long nowTick) {
        refresh(nowTick);
        return nowTick < hyperarmorUntilTick
                ? hyperarmorMultiplier
                : 1.0;
    }

    public void reset() {
        pips = 0;
        lastQualifyingActivityTick = Long.MIN_VALUE / 4;
        nextBasicGainAllowedTick = Long.MIN_VALUE / 4;
        lastBasicCyclePublicationTick = Long.MIN_VALUE / 4;
        counterUntilTick = Long.MIN_VALUE / 4;
        hyperarmorUntilTick = Long.MIN_VALUE / 4;
        hyperarmorMultiplier = 1.0;
    }

    private void refresh(long nowTick) {
        if (pips > 0
                && lastQualifyingActivityTick > Long.MIN_VALUE / 8
                && nowTick >= Math.addExact(
                        lastQualifyingActivityTick,
                        EXPIRY_TICKS
                )) {
            pips = 0;
        }
        if (nowTick >= hyperarmorUntilTick) {
            hyperarmorMultiplier = 1.0;
        }
    }
}
