package kr.moonseungjun.arcanecircle.magic;

/**
 * Shared ninth-circle magnitude contract.
 *
 * Ninth-circle catastrophe spells are not ordinary combat spells with larger numbers.  Their
 * authoritative footprint controls projectile count, projectile body scale, fall altitude and
 * terminal shock size.  Precision/self laws stay compact elsewhere; this class is deliberately
 * about spatial catastrophe magnitude.
 */
public final class NinthCircleMagnitude {
    public static final double BASE_METEOR_CAST_RANGE = 72.0;
    public static final double BASE_METEOR_CITY_RADIUS = 112.0;

    private NinthCircleMagnitude() {}

    /** Around five times the old ~22 block Crown footprint at the canonical 72m cast range. */
    public static double meteorFieldRadius(double effectiveRange) {
        double range = Double.isFinite(effectiveRange) ? Math.max(1.0, effectiveRange) : BASE_METEOR_CAST_RANGE;
        return clamp(Math.max(110.0, range * 1.5555555556), 110.0, 168.0);
    }

    /** Dozens of bodies; more range means more actual falling meteors rather than empty visual area. */
    public static int meteorStrikeCount(double effectiveRange) {
        double field = meteorFieldRadius(effectiveRange);
        return clampInt((int) Math.round(31.0 + field * .16), 49, 61);
    }

    public static double meteorOrdinaryScale(double effectiveRange, double radialFraction, double variation) {
        double fieldFactor = meteorFieldRadius(effectiveRange) / BASE_METEOR_CITY_RADIUS;
        double radial = clamp(radialFraction, 0.0, 1.0);
        double jitter = clamp(variation, 0.0, 1.0);
        return clamp((1.45 + radial * 1.18 + jitter * .62) * Math.sqrt(fieldFactor), 1.50, 4.20);
    }

    public static double meteorFallHeight(double effectiveRange, double radialFraction, double variation) {
        double field = meteorFieldRadius(effectiveRange);
        return clamp(70.0 + field * .20 + clamp(radialFraction, 0.0, 1.0) * 24.0
                + clamp(variation, 0.0, 1.0) * 18.0, 88.0, 148.0);
    }

    public static double crownScale(double effectiveRange) {
        return clamp(5.25 + meteorFieldRadius(effectiveRange) / BASE_METEOR_CITY_RADIUS * .72, 5.80, 6.80);
    }

    public static double crownFallHeight(double effectiveRange) {
        return clamp(126.0 + meteorFieldRadius(effectiveRange) * .23, 150.0, 178.0);
    }

    public static double crownLethalRadius(double effectiveRange) {
        return meteorFieldRadius(effectiveRange) * .50;
    }

    public static double crownShockRadius(double effectiveRange) {
        return meteorFieldRadius(effectiveRange);
    }

    public static double cityfallSkyHeight(double effectiveRange) {
        return clamp(86.0 + meteorFieldRadius(effectiveRange) * .42, 132.0, 158.0);
    }

    public static double cityfallCrownRadius(double effectiveRange) {
        return meteorFieldRadius(effectiveRange) * .74;
    }

    private static int clampInt(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
