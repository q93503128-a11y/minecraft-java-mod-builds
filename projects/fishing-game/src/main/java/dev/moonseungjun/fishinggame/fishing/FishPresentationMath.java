package dev.moonseungjun.fishinggame.fishing;

public final class FishPresentationMath {
    private static final float MIN_SCALE = 0.65f;
    private static final float MAX_SCALE = 1.85f;

    private FishPresentationMath() {
    }

    public static float scaleFor(FishSpecies species) {
        return scaleForLength((species.minLengthCm() + species.maxLengthCm()) * 0.5);
    }

    public static float scaleForLength(double averageLengthCm) {
        double normalized = Math.sqrt(Math.max(0.20, averageLengthCm / 42.0));
        return (float) Math.max(MIN_SCALE, Math.min(MAX_SCALE, normalized));
    }

    public static double approachRadius(double progress, double startDistance, double hookDistance) {
        double t = clamp01(progress);
        double eased = 1.0 - Math.pow(1.0 - t, 3.0);
        return startDistance + (hookDistance - startDistance) * eased;
    }

    public static double approachWeave(double progress, double phase) {
        double t = clamp01(progress);
        return Math.sin(t * Math.PI * 3.0 + phase) * (0.58 * (1.0 - t) + 0.04);
    }

    public static double fightRadius(float progress, float resistance, float burstStrength, float tension) {
        double base = 0.52 + (1.0 - clamp01(progress)) * 1.75;
        double resistanceReach = Math.max(0.0, resistance - 0.8f) * 0.22;
        double tensionReach = Math.max(0.0, tension - ReelMath.SAFE_MAX) * 1.55;
        double burstReach = Math.max(0.0f, burstStrength) * 1.20;
        return Math.max(0.45, Math.min(4.2, base + resistanceReach + tensionReach + burstReach));
    }

    public static float burstPull(float tension, float resistance, float rodStrength, float burstStrength) {
        float pull = 0.017f * Math.max(0.0f, burstStrength) * Math.max(0.65f, resistance)
                / Math.max(0.70f, rodStrength);
        return Math.max(0.0f, Math.min(1.2f, tension + pull));
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
