package dev.moonseungjun.fishinggame.fishing;

public final class CastChargeMath {
    public static final int FULL_CHARGE_TICKS = 18;
    public static final float MIN_SPEED_MULTIPLIER = 0.78f;
    public static final float MAX_SPEED_MULTIPLIER = 1.45f;

    private CastChargeMath() {
    }

    public static float normalizedCharge(long heldTicks) {
        if (heldTicks <= 0L) return 0.0f;
        return Math.min(1.0f, heldTicks / (float) FULL_CHARGE_TICKS);
    }

    public static float speedMultiplier(long heldTicks) {
        float charge = normalizedCharge(heldTicks);
        float eased = charge * charge * (3.0f - 2.0f * charge);
        return MIN_SPEED_MULTIPLIER + (MAX_SPEED_MULTIPLIER - MIN_SPEED_MULTIPLIER) * eased;
    }
}
