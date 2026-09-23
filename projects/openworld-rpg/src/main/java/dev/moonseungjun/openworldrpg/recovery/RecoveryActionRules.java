package dev.moonseungjun.openworldrpg.recovery;

/** Locked R01 Recovery Belt timing/effect constants. */
public final class RecoveryActionRules {
    public static final int BELT_CAPACITY = 4;
    public static final int USE_DURATION_TICKS = 19;
    public static final int RESOLUTION_TICKS = 14;
    public static final int SHARED_LOCKOUT_TICKS = 120;
    public static final double ACTION_MOVEMENT_MULTIPLIER = 0.65;

    public static final double HEALING_POTION_MAX_HP_FRACTION = 0.35;
    public static final double FOCUS_DRAUGHT_IMMEDIATE_MAX_MANA_FRACTION = 0.25;
    public static final double FOCUS_DRAUGHT_TAIL_MAX_MANA_FRACTION = 0.15;
    public static final int FOCUS_DRAUGHT_TAIL_TICKS = 60;

    private RecoveryActionRules() {
    }

    public static double healingPotionAmount(double maxHp) {
        requireFinitePositive("maxHp", maxHp);
        return maxHp * HEALING_POTION_MAX_HP_FRACTION;
    }

    public static double focusImmediateAmount(double maxMana) {
        requireFinitePositive("maxMana", maxMana);
        return maxMana * FOCUS_DRAUGHT_IMMEDIATE_MAX_MANA_FRACTION;
    }

    public static double focusTailPerTick(double maxMana) {
        requireFinitePositive("maxMana", maxMana);
        return maxMana
                * FOCUS_DRAUGHT_TAIL_MAX_MANA_FRACTION
                / FOCUS_DRAUGHT_TAIL_TICKS;
    }

    private static void requireFinitePositive(String name, double value) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(name + " must be finite and positive.");
        }
    }
}
