package io.github.q93503128.turnbound.client;

/** Tiny deterministic easing set used by the 2D summon compositor. */
final class SummonEasing {
    private SummonEasing() {}

    static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    static float easeOutCubic(float t) {
        float x = 1.0F - clamp01(t);
        return 1.0F - x * x * x;
    }

    static float easeInOutCubic(float t) {
        float x = clamp01(t);
        return x < 0.5F ? 4.0F * x * x * x
                : 1.0F - (float) Math.pow(-2.0F * x + 2.0F, 3.0) / 2.0F;
    }

    static float backOut(float t) {
        float x = clamp01(t) - 1.0F;
        float c1 = 1.70158F;
        float c3 = c1 + 1.0F;
        return 1.0F + c3 * x * x * x + c1 * x * x;
    }

    static float pulse(float t) {
        return 0.5F + 0.5F * (float) Math.sin(clamp01(t) * Math.PI);
    }
}
