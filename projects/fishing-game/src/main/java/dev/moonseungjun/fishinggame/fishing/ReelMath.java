package dev.moonseungjun.fishinggame.fishing;

public final class ReelMath {
    public static final float BREAK_TENSION = 1.0f;
    public static final float SAFE_MIN = 0.28f;
    public static final float SAFE_MAX = 0.78f;

    private ReelMath() {
    }

    public static float updateTension(float tension, boolean reelImpulse, float resistance) {
        float next = reelImpulse
                ? tension + (0.052f * resistance)
                : tension - (0.022f + Math.max(0.0f, resistance - 1.0f) * 0.006f);
        return Math.max(0.0f, Math.min(1.2f, next));
    }

    public static float updateProgress(float progress, float tension, float resistance) {
        float next = progress;
        if (tension >= SAFE_MIN && tension <= SAFE_MAX) {
            next += 0.014f / Math.max(0.65f, resistance);
        } else if (tension < 0.16f) {
            next -= 0.008f * resistance;
        } else if (tension > SAFE_MAX) {
            next -= 0.003f * resistance;
        }
        return Math.max(0.0f, Math.min(1.0f, next));
    }

    public static boolean isLineBroken(float tension) {
        return tension >= BREAK_TENSION;
    }
}
