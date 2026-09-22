package dev.moonseungjun.openworldrpg.combat.encounter.r01;

/** Pure horizontal coordinate math for committed Lightning Furrow lanes. */
public final class R01EarthloongFurrowGeometry {
    private R01EarthloongFurrowGeometry() {}

    public static Coordinates coordinates(
            double originX,
            double originZ,
            double forwardX,
            double forwardZ,
            double pointX,
            double pointZ
    ) {
        double length = Math.hypot(forwardX, forwardZ);
        if (!Double.isFinite(length) || length <= 1.0e-9) {
            throw new IllegalArgumentException("Furrow forward axis must be non-zero.");
        }
        double fx = forwardX / length;
        double fz = forwardZ / length;
        double lx = -fz;
        double lz = fx;
        double dx = pointX - originX;
        double dz = pointZ - originZ;
        return new Coordinates(
                dx * fx + dz * fz,
                dx * lx + dz * lz
        );
    }

    public static boolean insideLane(
            Coordinates coordinates,
            double laneCenterOffset,
            double halfWidth,
            double length
    ) {
        if (halfWidth < 0.0 || length < 0.0) {
            throw new IllegalArgumentException("Furrow lane dimensions must be non-negative.");
        }
        return coordinates.longitudinal() >= 0.0
                && coordinates.longitudinal() <= length
                && Math.abs(coordinates.lateral() - laneCenterOffset) <= halfWidth;
    }

    public record Coordinates(double longitudinal, double lateral) {}
}
