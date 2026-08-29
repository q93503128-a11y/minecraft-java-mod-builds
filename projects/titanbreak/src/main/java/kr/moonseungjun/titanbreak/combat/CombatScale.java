package kr.moonseungjun.titanbreak.combat;

/**
 * Keeps player-facing combat values independent from Minecraft's internal health/damage scale.
 * The content bible baseline is 100 visible health while an unmodified Minecraft player has 20.
 */
public final class CombatScale {
    public static final double VISIBLE_PER_INTERNAL_HEALTH = 5.0D;

    private CombatScale() {}

    public static double toInternal(double visibleValue) {
        return visibleValue / VISIBLE_PER_INTERNAL_HEALTH;
    }

    public static double toVisible(double internalValue) {
        return internalValue * VISIBLE_PER_INTERNAL_HEALTH;
    }
}
