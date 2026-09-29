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
    private double maxManaBonus;
    private double maxStaminaBonus;
    private double manaRecoveryBonus;
    private double staminaRecoveryBonus;
    private double manaCostReduction;
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
        return maxManaForWill(will, maxManaBonus);
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
        int oldMax = maxMana();
        this.will = newWill;
        int newMax = maxMana();
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
        return maxStaminaForEndurance(endurance, maxStaminaBonus);
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
        int oldMax = maxStamina();
        this.endurance = newEndurance;
        int newMax = maxStamina();
        if (oldMax <= 0) {
            stamina = newMax;
        } else {
            stamina = Math.min(newMax, stamina * newMax / oldMax);
        }
    }


    /**
     * Synchronizes equipped resource modifiers while preserving current Mana/Stamina percentages.
     */
    public void synchronizeResourceModifiers(
            EquipmentResourceModifiers modifiers,
            long nowTick
    ) {
        Objects.requireNonNull(modifiers, "modifiers");
        refresh(nowTick);

        int oldMaxMana = maxMana();
        int oldMaxStamina = maxStamina();
        double manaFraction = oldMaxMana > 0
                ? Math.max(0.0, Math.min(1.0, mana / oldMaxMana))
                : 1.0;
        double staminaFraction = oldMaxStamina > 0
                ? Math.max(0.0, Math.min(1.0, stamina / oldMaxStamina))
                : 1.0;

        maxManaBonus = modifiers.maxManaBonus();
        maxStaminaBonus = modifiers.maxStaminaBonus();
        manaRecoveryBonus = modifiers.manaRecoveryBonus();
        staminaRecoveryBonus = modifiers.staminaRecoveryBonus();
        manaCostReduction = modifiers.manaCostReduction();

        mana = Math.min(maxMana(), maxMana() * manaFraction);
        stamina = Math.min(maxStamina(), maxStamina() * staminaFraction);
    }

    public EquipmentResourceModifiers resourceModifiers() {
        return new EquipmentResourceModifiers(
                0.0,
                maxManaBonus,
                maxStaminaBonus,
                manaRecoveryBonus,
                staminaRecoveryBonus,
                manaCostReduction
        );
    }

    public PlayerCombatSessionState persistentSnapshot(long nowTick) {
        refresh(nowTick);
        Map<String, Long> activeCooldowns = new HashMap<>();
        cooldownEndTick.forEach((id, endTick) -> {
            if (endTick > nowTick) {
                activeCooldowns.put(id, endTick);
            }
        });
        return new PlayerCombatSessionState(
                PlayerCombatSessionState.CURRENT_SCHEMA_VERSION,
                mana,
                stamina,
                nowTick,
                lastManaSpendTick,
                lastCombatActivityTick,
                lastHostileHpActivityTick,
                staminaRegenBlockedUntilTick,
                Map.copyOf(activeCooldowns)
        );
    }

    public void restorePersistent(
            PlayerCombatSessionState snapshot,
            long nowTick
    ) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (nowTick < 0L) {
            throw new IllegalArgumentException("nowTick must be non-negative.");
        }

        long rebase = nowTick < snapshot.savedAtTick()
                ? nowTick - snapshot.savedAtTick()
                : 0L;
        mana = Math.min(maxMana(), snapshot.mana());
        stamina = Math.min(maxStamina(), snapshot.stamina());
        lastRefreshTick = Math.min(snapshot.savedAtTick(), nowTick);
        lastManaSpendTick = rebaseTick(snapshot.lastManaSpendTick(), rebase);
        lastCombatActivityTick = rebaseTick(snapshot.lastCombatActivityTick(), rebase);
        lastHostileHpActivityTick = rebaseTick(
                snapshot.lastHostileHpActivityTick(),
                rebase
        );
        staminaRegenBlockedUntilTick = rebaseTick(
                snapshot.staminaRegenBlockedUntilTick(),
                rebase
        );
        cooldownEndTick.clear();
        snapshot.cooldownEndTicks().forEach((id, endTick) -> {
            long rebased = rebaseTick(endTick, rebase);
            if (rebased > nowTick) {
                cooldownEndTick.put(id, rebased);
            }
        });

        sprintingLastTick = false;
        acceptedSpellId = null;
        acceptedSpellReentryUntilTick = Long.MIN_VALUE;
        refresh(nowTick);
    }

    public double effectiveManaCost(double authoredCost) {
        validateManaAmount(authoredCost);
        return authoredCost * (1.0 - manaCostReduction);
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
            sprintingLastTick = false;
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
        double effectiveCost = effectiveManaCost(amount);
        refresh(nowTick);
        return mana + 1.0e-9 >= effectiveCost;
    }

    public boolean spendMana(double amount, long nowTick) {
        double effectiveCost = effectiveManaCost(amount);
        refresh(nowTick);
        if (mana + 1.0e-9 < effectiveCost) {
            return false;
        }
        mana = Math.max(0.0, mana - effectiveCost);
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

                double perTick = baseManaRegenPerSecondForWill(will)
                        * (1.0 + manaRecoveryBonus)
                        / 20.0;
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
                        * (1.0 + staminaRecoveryBonus)
                        / 20.0;
                stamina = Math.min(maxStamina(), stamina);
            }
        }

        lastRefreshTick = nowTick;
    }

    public static int maxManaForWill(int will) {
        return maxManaForWill(will, 0.0);
    }

    public static int maxManaForWill(int will, double maxManaBonus) {
        if (will < 0) {
            throw new IllegalArgumentException("WIL must be non-negative.");
        }
        requirePercentBonus("maxManaBonus", maxManaBonus);
        int x = Math.max(0, will - 5);
        double value = 100.0
                + 2.5 * Math.min(x, 25)
                + 1.5 * Math.min(Math.max(x - 25, 0), 30)
                + 0.75 * Math.max(x - 55, 0);
        return (int) Math.round(value * (1.0 + maxManaBonus));
    }

    public static double baseManaRegenPerSecondForWill(int will) {
        int x = Math.max(0, will - 5);
        return 4.0 + 0.04 * x;
    }

    public static int maxStaminaForEndurance(int endurance) {
        return maxStaminaForEndurance(endurance, 0.0);
    }

    public static int maxStaminaForEndurance(
            int endurance,
            double maxStaminaBonus
    ) {
        if (endurance < 0) {
            throw new IllegalArgumentException("END must be non-negative.");
        }
        requirePercentBonus("maxStaminaBonus", maxStaminaBonus);
        int x = Math.max(0, endurance - 5);
        double value = 100.0
                + 1.2 * Math.min(x, 25)
                + 0.8 * Math.min(Math.max(x - 25, 0), 30)
                + 0.4 * Math.max(x - 55, 0);
        return (int) Math.round(value * (1.0 + maxStaminaBonus));
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

    private static long rebaseTick(long tick, long delta) {
        if (delta == 0L || tick <= Long.MIN_VALUE / 8) {
            return tick;
        }
        return Math.addExact(tick, delta);
    }

    private static void requirePercentBonus(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(
                    name + " must be finite and non-negative."
            );
        }
    }
}
