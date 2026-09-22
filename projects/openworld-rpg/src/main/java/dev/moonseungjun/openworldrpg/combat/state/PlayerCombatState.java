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
    public static final long NATURAL_HP_RECOVERY_DELAY_TICKS = 160L;
    public static final long SPRINT_STOP_REGEN_DELAY_TICKS = 7L;
    public static final double SPRINT_STAMINA_PER_SECOND = 5.0;

    private int will;
    private double mana;
    private int endurance;
    private double stamina;
    private long lastRefreshTick;
    private long lastManaSpendTick = Long.MIN_VALUE / 4;
    private long lastCombatActivityTick = Long.MIN_VALUE / 4;
    private long lastHostileHpActivityTick = Long.MIN_VALUE / 4;
    private long staminaRegenBlockedUntilTick = Long.MIN_VALUE / 4;
    private boolean sprintingLastTick;
    private String acceptedSpellId;
    private long acceptedSpellReentryUntilTick = Long.MIN_VALUE;
    private final Map<String, Long> cooldownEndTick = new HashMap<>();

    public PlayerCombatState(int will, long nowTick) {
        if (will < 0) {
            throw new IllegalArgumentException("WIL must be non-negative.");
        }
        this.will = will;
        this.mana = maxManaForWill(will);
        this.endurance = 5;
        this.stamina = maxStaminaForEndurance(endurance);
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

    /**
     * Synchronizes effective WIL while preserving the current Mana percentage.
     *
     * <p>This prevents equipment/stat swaps from becoming an implicit Mana refill while still
     * scaling the usable pool immediately when Max Mana changes.</p>
     */
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

    public int endurance() {
        return endurance;
    }

    public int maxStamina() {
        return maxStaminaForEndurance(endurance);
    }

    public double stamina(long nowTick) {
        refresh(nowTick);
        return stamina;
    }

    public void synchronizeEndurance(int newEndurance, long nowTick) {
        if (newEndurance < 0) {
            throw new IllegalArgumentException("END must be non-negative.");
        }
        refresh(nowTick);
        int oldMax = maxStaminaForEndurance(endurance);
        this.endurance = newEndurance;
        int newMax = maxStaminaForEndurance(newEndurance);
        if (oldMax <= 0) {
            stamina = newMax;
        } else {
            stamina = Math.min(newMax, stamina * newMax / oldMax);
        }
    }

    public boolean canSpendStamina(double amount, long nowTick) {
        validateStaminaAmount(amount);
        refresh(nowTick);
        return stamina + 1.0e-9 >= amount;
    }

    public boolean spendStamina(
            double amount,
            long regenDelayTicks,
            long nowTick
    ) {
        validateStaminaAmount(amount);
        if (regenDelayTicks < 0L) {
            throw new IllegalArgumentException("Stamina regen delay cannot be negative.");
        }
        refresh(nowTick);
        if (stamina + 1.0e-9 < amount) {
            return false;
        }
        stamina = Math.max(0.0, stamina - amount);
        staminaRegenBlockedUntilTick = Math.max(
                staminaRegenBlockedUntilTick,
                nowTick + regenDelayTicks
        );
        return true;
    }

    public void restoreStamina(double amount, long nowTick) {
        validateStaminaAmount(amount);
        refresh(nowTick);
        stamina = Math.min(maxStamina(), stamina + amount);
    }

    /**
     * Server sprint authority. Continuous sprint drains 5 Stamina/s. Regeneration is blocked while
     * sprinting and for 7 ticks after the transition to not sprinting.
     *
     * @return true while sprint may continue; false when Stamina was insufficient for this tick.
     */
    public boolean updateSprinting(boolean sprinting, long nowTick) {
        refresh(nowTick);

        if (!sprinting) {
            if (sprintingLastTick) {
                staminaRegenBlockedUntilTick = Math.max(
                        staminaRegenBlockedUntilTick,
                        nowTick + SPRINT_STOP_REGEN_DELAY_TICKS
                );
            }
            sprintingLastTick = false;
            return true;
        }

        sprintingLastTick = true;
        double perTick = SPRINT_STAMINA_PER_SECOND / 20.0;
        if (stamina + 1.0e-9 < perTick) {
            stamina = Math.max(0.0, stamina);
            staminaRegenBlockedUntilTick = Math.max(
                    staminaRegenBlockedUntilTick,
                    nowTick + SPRINT_STOP_REGEN_DELAY_TICKS
            );
            return false;
        }

        stamina -= perTick;
        staminaRegenBlockedUntilTick = Math.max(
                staminaRegenBlockedUntilTick,
                nowTick + 1L
        );
        return true;
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

    /**
     * Marks hostile HP interaction by this player, either dealing or receiving it.
     *
     * <p>Authored DoT/status systems with no live attacking entity must call this explicitly when
     * they deal HP damage. Ordinary fall/environment damage is not hostile combat activity.</p>
     */
    public void markHostileHpActivity(long nowTick) {
        if (nowTick > lastHostileHpActivityTick) {
            lastHostileHpActivityTick = nowTick;
        }
        markCombatActivity(nowTick);
    }

    public boolean canNaturalHpRecover(long nowTick) {
        long blockedUntil = Math.max(
                lastHostileHpActivityTick + NATURAL_HP_RECOVERY_DELAY_TICKS,
                lastCombatActivityTick + NATURAL_HP_RECOVERY_DELAY_TICKS
        );
        return nowTick >= blockedUntil;
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

    /**
     * Same-tick/short-window duplicate callback protection for one accepted cast.
     */
    public boolean isAcceptedCastReentry(String spellId, long nowTick) {
        return acceptedSpellId != null
                && acceptedSpellId.equals(spellId)
                && nowTick <= acceptedSpellReentryUntilTick;
    }

    /**
     * Long-lived transaction identity used only when the external engine proves that it is
     * continuing the same in-flight cast process.
     */
    public boolean isAcceptedCastContinuation(String spellId) {
        return acceptedSpellId != null && acceptedSpellId.equals(spellId);
    }

    public boolean hasCompetingAcceptedCast(String spellId, long nowTick) {
        return acceptedSpellId != null
                && !acceptedSpellId.equals(spellId)
                && nowTick <= acceptedSpellReentryUntilTick;
    }

    public void beginAcceptedCast(String spellId, long reentryUntilTick, long nowTick) {
        Objects.requireNonNull(spellId, "spellId");
        if (acceptedSpellId != null
                && !acceptedSpellId.equals(spellId)
                && nowTick <= acceptedSpellReentryUntilTick) {
            throw new IllegalStateException("Competing project spell cast is already accepted: " + acceptedSpellId);
        }
        acceptedSpellId = spellId;
        acceptedSpellReentryUntilTick = reentryUntilTick;
    }

    public void requireAcceptedCast(String spellId, long nowTick) {
        Objects.requireNonNull(spellId, "spellId");
        if (acceptedSpellId == null || !acceptedSpellId.equals(spellId)) {
            throw new IllegalStateException("No accepted project spell transaction for " + spellId + " at tick " + nowTick);
        }
    }

    public void completeAcceptedCast(String spellId) {
        if (acceptedSpellId == null || !acceptedSpellId.equals(spellId)) {
            throw new IllegalStateException("Cannot complete missing project spell transaction: " + spellId);
        }
        acceptedSpellId = null;
        acceptedSpellReentryUntilTick = Long.MIN_VALUE;
    }

    private void refresh(long nowTick) {
        if (nowTick < lastRefreshTick) {
            throw new IllegalArgumentException("Server combat time must be monotonic.");
        }
        if (nowTick == lastRefreshTick) {
            mana = Math.min(mana, maxMana());
            stamina = Math.min(stamina, maxStamina());
            return;
        }

        long start = lastRefreshTick;
        long end = nowTick;

        if (mana < maxMana()) {
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
        }

        if (stamina < maxStamina()) {
            long regenStart = Math.max(start, staminaRegenBlockedUntilTick);
            if (regenStart < end) {
                long regenTicks = end - regenStart;
                stamina += regenTicks
                        * baseStaminaRegenPerSecondForEndurance(endurance)
                        / 20.0;
                stamina = Math.min(maxStamina(), stamina);
            }
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

    public static int maxStaminaForEndurance(int endurance) {
        int x = Math.max(0, endurance - 5);
        double value = 100.0
                + 1.2 * Math.min(x, 25)
                + 0.8 * Math.min(Math.max(x - 25, 0), 30)
                + 0.4 * Math.max(x - 55, 0);
        return (int) Math.round(value);
    }

    public static double baseStaminaRegenPerSecondForEndurance(int endurance) {
        int x = Math.max(0, endurance - 5);
        return 24.0
                + 0.12 * Math.min(x, 55)
                + 0.05 * Math.max(x - 55, 0);
    }

    private static void validateStaminaAmount(double amount) {
        if (!Double.isFinite(amount) || amount < 0.0) {
            throw new IllegalArgumentException("Stamina amount must be finite and non-negative.");
        }
    }

    private static void validateManaAmount(double amount) {
        if (!Double.isFinite(amount) || amount < 0.0) {
            throw new IllegalArgumentException("Mana amount must be finite and non-negative.");
        }
    }
}
