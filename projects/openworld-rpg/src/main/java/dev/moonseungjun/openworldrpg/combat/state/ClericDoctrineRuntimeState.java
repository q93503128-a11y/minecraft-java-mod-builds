package dev.moonseungjun.openworldrpg.combat.state;

/**
 * Server-owned Balanced Doctrine alternation state.
 *
 * <p>Only one side can be primed. The state is transient combat rhythm, not progression.</p>
 */
public final class ClericDoctrineRuntimeState {
    public static final long WINDOW_TICKS = 120L;
    public static final double HEALING_PROTECTION_OUTPUT_MULTIPLIER = 1.10;
    public static final double DIRECT_DAMAGE_MULTIPLIER = 1.08;

    private Primed primed = Primed.NONE;
    private long expiresAtTick = Long.MIN_VALUE / 4;

    public double consumeHealingProtectionMultiplier(long nowTick) {
        refresh(nowTick);
        if (primed != Primed.HEALING_PROTECTION) {
            return 1.0;
        }
        primed = Primed.NONE;
        expiresAtTick = Long.MIN_VALUE / 4;
        return HEALING_PROTECTION_OUTPUT_MULTIPLIER;
    }

    public double consumeDirectDamageMultiplier(long nowTick) {
        refresh(nowTick);
        if (primed != Primed.DIRECT_DAMAGE) {
            return 1.0;
        }
        primed = Primed.NONE;
        expiresAtTick = Long.MIN_VALUE / 4;
        return DIRECT_DAMAGE_MULTIPLIER;
    }

    public void afterDamagingActive(long nowTick) {
        prime(Primed.HEALING_PROTECTION, nowTick);
    }

    public void afterHealingProtectionActive(long nowTick) {
        prime(Primed.DIRECT_DAMAGE, nowTick);
    }

    public Primed primed(long nowTick) {
        refresh(nowTick);
        return primed;
    }

    public long expiresAtTick(long nowTick) {
        refresh(nowTick);
        return expiresAtTick;
    }

    public void reset() {
        primed = Primed.NONE;
        expiresAtTick = Long.MIN_VALUE / 4;
    }

    private void prime(Primed next, long nowTick) {
        validateTime(nowTick);
        primed = next;
        expiresAtTick = Math.addExact(nowTick, WINDOW_TICKS);
    }

    private void refresh(long nowTick) {
        validateTime(nowTick);
        if (primed != Primed.NONE && nowTick >= expiresAtTick) {
            reset();
        }
    }

    private static void validateTime(long nowTick) {
        if (nowTick < 0L) {
            throw new IllegalArgumentException(
                    "Balanced Doctrine time must be non-negative."
            );
        }
    }

    public enum Primed {
        NONE,
        HEALING_PROTECTION,
        DIRECT_DAMAGE
    }
}
