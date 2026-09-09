package dev.moonseungjun.fishinggame.fishing;

public final class ReelMath {
    public static final float BREAK_TENSION = 1.0f;
    public static final float SAFE_MIN = 0.28f;
    public static final float SAFE_MAX = 0.78f;

    private ReelMath() {
    }

    public static float safeMin(float controlBonus) {
        return Math.max(0.18f, SAFE_MIN - Math.max(0.0f, controlBonus) * 0.45f);
    }

    public static float safeMax(float controlBonus) {
        return Math.min(0.91f, SAFE_MAX + Math.max(0.0f, controlBonus));
    }

    public static float updateTension(float tension, boolean reelHeld, float resistance, float rodStrength) {
        float effectiveResistance = resistance / Math.max(0.65f, rodStrength);
        float next = reelHeld
                ? tension + (0.052f * effectiveResistance)
                : tension - (0.022f + Math.max(0.0f, effectiveResistance - 1.0f) * 0.006f);
        return Math.max(0.0f, Math.min(1.2f, next));
    }

    public static float updateTension(float tension, boolean reelHeld, float resistance) {
        return updateTension(tension, reelHeld, resistance, 1.0f);
    }

    public static float updateProgress(float progress, float tension, float resistance, float controlBonus) {
        float next = progress;
        float safeMin = safeMin(controlBonus);
        float safeMax = safeMax(controlBonus);
        if (tension >= safeMin && tension <= safeMax) {
            next += 0.014f / Math.max(0.65f, resistance);
        } else if (tension < Math.max(0.10f, safeMin - 0.12f)) {
            next -= 0.008f * resistance;
        } else if (tension > safeMax) {
            next -= 0.003f * resistance;
        }
        return Math.max(0.0f, Math.min(1.0f, next));
    }

    public static float updateProgress(float progress, float tension, float resistance) {
        return updateProgress(progress, tension, resistance, 0.0f);
    }

    public static boolean isLineBroken(float tension) {
        return tension >= BREAK_TENSION;
    }
}
