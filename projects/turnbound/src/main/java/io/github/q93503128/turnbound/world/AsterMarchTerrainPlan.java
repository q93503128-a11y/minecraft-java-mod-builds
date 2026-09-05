package io.github.q93503128.turnbound.world;

/**
 * Single source of truth for authored Aster March terrain heights.
 *
 * The legacy build used one raw Y value for the whole 1024x1024 playfield.  Radia now owns a
 * deterministic coastal terrain mask while the remaining v0.4 field regions keep the stable
 * campaign base height until their authored terrain passes are migrated.
 */
public final class AsterMarchTerrainPlan {
    public static final int WORLD_BASE_Y = 65;
    public static final int RADIA_SEA_Y = 61;
    public static final int RADIA_FLOOR_Y = 58;

    public enum Kind { FIELD, RADIA_LAND, RADIA_WATER }
    public record Column(Kind kind, int surfaceY) {}

    private AsterMarchTerrainPlan() {}

    public static Column column(int x, int z) {
        if (AsterMarchRegionCatalog.RADIA.contains(x, z)) {
            if (radiaLand(x, z)) return new Column(Kind.RADIA_LAND, radiaSurfaceY(x, z));
            return new Column(Kind.RADIA_WATER, RADIA_SEA_Y);
        }
        return new Column(Kind.FIELD, WORLD_BASE_Y);
    }

    /**
     * Union of several hand-shaped lobes plus a carved northern harbor.
     * A tiny deterministic shoreline wobble keeps the silhouette from reading as circles/rectangles.
     */
    public static boolean radiaLand(int x, int z) {
        if (!AsterMarchRegionCatalog.RADIA.contains(x, z)) return false;

        // Authored inland neck: South Gate must physically connect Radia to Southgate Meadow.
        if (z >= 88 && Math.abs(x) <= 42 - Math.max(0, z - 88) / 3) return true;

        double wobble = 0.055 * Math.sin(x * 0.173)
                + 0.040 * Math.cos(z * 0.137)
                + 0.025 * Math.sin((x + z) * 0.091);

        boolean body = ellipse(x, z, 0, 18, 101, 88) <= 1.0 + wobble
                || ellipse(x, z, -58, 15, 54, 66) <= 1.0 + wobble
                || ellipse(x, z, 58, 18, 52, 65) <= 1.0 + wobble
                || ellipse(x, z, 0, -43, 87, 59) <= 1.0 + wobble;

        if (!body) return false;

        // Deep inlet. Side cliffs remain land so Rift/Memorial/Clock districts frame the harbor.
        boolean harbor = z < -54 && ellipse(x, z, 0, -80, 39, 29) < 1.0;
        boolean harborMouth = z < -76 && Math.abs(x) < 50 + ((-z - 76) / 2);
        return !(harbor || harborMouth);
    }

    public static int radiaSurfaceY(int x, int z) {
        int y;
        if (z <= -70) {
            y = 64;
        } else if (z <= -20) {
            y = 64 + (int)Math.round((z + 70) * 10.0 / 50.0);
        } else if (z <= 20) {
            y = 74 + (int)Math.round((z + 20) / 40.0);
        } else if (z <= 80) {
            y = 75 - (int)Math.round((z - 20) * 6.0 / 60.0);
        } else {
            y = 69 - (int)Math.round((z - 80) * 2.0 / 28.0);
        }

        int texture = (int)Math.round(
                0.65 * Math.sin(x * 0.121)
                        + 0.45 * Math.cos(z * 0.113)
                        + 0.30 * Math.sin((x - z) * 0.071));
        y += Math.max(-1, Math.min(1, texture));

        // Keep the authored range safely inside the existing Radia containment envelope.
        return Math.max(64, Math.min(76, y));
    }

    private static double ellipse(double x, double z, double cx, double cz, double rx, double rz) {
        double dx = (x - cx) / rx;
        double dz = (z - cz) / rz;
        return dx * dx + dz * dz;
    }
}
