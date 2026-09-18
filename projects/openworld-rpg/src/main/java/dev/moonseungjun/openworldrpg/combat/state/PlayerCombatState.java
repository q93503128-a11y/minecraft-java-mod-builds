package dev.moonseungjun.openworldrpg.combat.state;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Server-owned runtime combat resources.
 *
 * <p>This is domain state, not a client prediction object. Persistence wiring is intentionally
 * separate so save/rejoin work can serialize the same state later without moving authority into a
 * dependency adapter.</p>
 */
public final class PlayerCombatState {
    private static final long MANA_REGEN_LOCK_TICKS = 20L;
    private static final long OUT_OF_COMBAT_BONUS_TICKS = 100L;

    private int will;
    private double mana;
    private long lastRefreshTick;
    private long lastManaSpendTick = Long.MIN_VALUE / 4;
    private long lastCombatActivityTick = Long.MIN_VALUE / 4;
    private final Map<String, Long> cooldownEndTick = new HashMap<>();

    public PlayerCombatState(int will, long nowTick) {
        if (will < 0) {
            throw new IllegalArgumentException("WIL must be non-negative.");
        }
        this.will = will;
        this.mana = maxManaForWill(will);
        this.lastRefreshTick = nowTick;
    }

    public int will() {
        return will;
    }

    public int maxMana() {
        return maxManaForWill(will);
    }

    public double mana(long nowTick) {
        refresh(nowTick);
        return mana;
    }

    public void synchronizeWill(int newWill, long nowTick) {
        if (newWill < 0) {
            throw new IllegalArgumentException("WIL must be non-negative.");
        }
        refresh(nowTick);
        int oldMax = maxManaForWill(will);
        this.will = newWill;
        int newMax = maxManaForWill(newWill);
        if (oldMax <= 0) {
            mana = newMax;
        } else {
            mana = Math.min(newMax, mana * newMax / oldMax);
        }
    }

    public boolean canSpendMana(double amount, long nowTick) {
        validateManaAmount(amount);
        refresh(nowTick);
        return mana + 1.0e-9 >= amount;
    }

    public boolean spendMana(double amount, long nowTick) {
        validateManaAmount(amount);
        refresh(nowTick);
        if (mana + 1.0e-9 < amount) {
            return false;
        }
        mana = Math.max(0.0, mana - amount);
        lastManaSpendTick = nowTick;
        markCombatActivity(nowTick);
        return true;
    }

    public void restoreMana(double amount, long nowTick) {
        validateManaAmount(amount);
        refresh(nowTick);
        mana = Math.min(maxMana(), mana + amount);
    }

    public void markCombatActivity(long nowTick) {
        if (nowTick > lastCombatActivityTick) {
            lastCombatActivityTick = nowTick;
        }
    }

    public boolean isCoolingDown(String actionId, long nowTick) {
        Objects.requireNonNull(actionId, "actionId");
        return cooldownEndTick.getOrDefault(actionId, Long.MIN_VALUE) > nowTick;
    }

    public long cooldownRemainingTicks(String actionId, long nowTick) {
        Objects.requireNonNull(actionId, "actionId");
        return Math.max(0L, cooldownEndTick.getOrDefault(actionId, Long.MIN_VALUE) - nowTick);
    }

    public void startCooldown(String actionId, int durationTicks, long nowTick) {
        Objects.requireNonNull(actionId, "actionId");
        if (durationTicks < 0) {
            throw new IllegalArgumentException("Cooldown duration must not be negative.");
        }
        cooldownEndTick.put(actionId, nowTick + durationTicks);
    }

    public void reduceCooldown(String actionId, int ticks, long nowTick) {
        Objects.requireNonNull(actionId, "actionId");
        if (ticks < 0) {
            throw new IllegalArgumentException("Cooldown reduction must not be negative.");
        }
        long current = cooldownEndTick.getOrDefault(actionId, nowTick);
        cooldownEndTick.put(actionId, Math.max(nowTick, current - ticks));
    }

    private void refresh(long nowTick) {
        if (nowTick < lastRefreshTick) {
            throw new IllegalArgumentException("Server combat time must be monotonic.");
        }
        if (nowTick == lastRefreshTick || mana >= maxMana()) {
            lastRefreshTick = nowTick;
            mana = Math.min(mana, maxMana());
            return;
        }

        long start = lastRefreshTick;
        long end = nowTick;
        long regenStart = Math.max(start, lastManaSpendTick + MANA_REGEN_LOCK_TICKS);
        if (regenStart < end) {
            long outOfCombatAt = lastCombatActivityTick + OUT_OF_COMBAT_BONUS_TICKS;
            long normalEnd = Math.min(end, Math.max(regenStart, outOfCombatAt));
            long normalTicks = Math.max(0L, normalEnd - regenStart);
            long bonusTicks = Math.max(0L, end - Math.max(regenStart, outOfCombatAt));

            double perTick = baseManaRegenPerSecondForWill(will) / 20.0;
            mana += normalTicks * perTick;
            mana += bonusTicks * perTick * 2.0;
            mana = Math.min(maxMana(), mana);
        }
        lastRefreshTick = nowTick;
    }

    public static int maxManaForWill(int will) {
        int x = Math.max(0, will - 5);
        double value = 100.0
                + 2.5 * Math.min(x, 25)
                + 1.5 * Math.min(Math.max(x - 25, 0), 30)
                + 0.75 * Math.max(x - 55, 0);
        return (int) Math.round(value);
    }

    public static double baseManaRegenPerSecondForWill(int will) {
        int x = Math.max(0, will - 5);
        return 4.0 + 0.04 * x;
    }

    private static void validateManaAmount(double amount) {
        if (!Double.isFinite(amount) || amount < 0.0) {
            throw new IllegalArgumentException("Mana amount must be finite and non-negative.");
        }
    }
}
