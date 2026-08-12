package kr.moonseungjun.livingkingdoms.world;

import java.util.List;

/**
 * Stable capital lots whose imported architecture must win over generic street/canopy repair.
 *
 * <p>These bounds mirror the citadel, landmark and residential compound exclusions used when the
 * continuous urban fabric is laid out. Keeping the geometry in one runtime predicate lets late
 * repair passes distinguish an authored building/yard from ordinary vegetation without guessing
 * from block palettes.</p>
 */
public final class ErdenCapitalProtectedGeometry {
    private static final List<Lot> LOTS = List.of(
            new Lot(0, 0, 150, 150),
            new Lot(-390, -520, 90, 82), new Lot(390, -520, 90, 82),
            new Lot(-700, -610, 94, 90), new Lot(-700, -350, 94, 90),
            new Lot(710, -560, 110, 98), new Lot(720, -270, 110, 98),
            new Lot(-720, 540, 82, 82), new Lot(-400, 610, 82, 82),
            new Lot(760, 590, 82, 82), new Lot(360, 300, 92, 82),
            new Lot(650, 220, 92, 82), new Lot(620, 520, 92, 82),
            new Lot(-170, 600, 92, 82), new Lot(170, 600, 92, 82),
            new Lot(-990, -420, 92, 82), new Lot(-980, 250, 92, 82),

            new Lot(-1020, -720, 88, 80), new Lot(-820, -720, 82, 80),
            new Lot(-180, -720, 88, 80), new Lot(180, -720, 88, 80),
            new Lot(820, -720, 82, 80), new Lot(1020, -720, 88, 80),
            new Lot(-1020, -180, 88, 80), new Lot(-780, -180, 94, 86),
            new Lot(-360, -180, 88, 80), new Lot(360, -180, 88, 80),
            new Lot(780, -180, 94, 86), new Lot(1020, -180, 88, 80),
            new Lot(-1020, 180, 88, 80), new Lot(-780, 180, 94, 86),
            new Lot(-360, 180, 88, 80), new Lot(360, 180, 88, 80),
            new Lot(780, 180, 94, 86), new Lot(1020, 180, 88, 80),
            new Lot(-1020, 720, 88, 80), new Lot(-820, 720, 82, 80),
            new Lot(-180, 720, 88, 80), new Lot(180, 720, 88, 80),
            new Lot(820, 720, 82, 80), new Lot(1020, 720, 88, 80)
    );

    private ErdenCapitalProtectedGeometry() {
    }

    public static boolean protectsAuthoredStructure(int x, int z) {
        for (Lot lot : LOTS) {
            if (x >= lot.centerX - lot.halfWidth
                    && x <= lot.centerX + lot.halfWidth
                    && z >= lot.centerZ - lot.halfLength
                    && z <= lot.centerZ + lot.halfLength) {
                return true;
            }
        }
        return false;
    }

    public static int lotCount() {
        return LOTS.size();
    }

    private record Lot(int centerX, int centerZ, int halfWidth, int halfLength) {
    }
}
