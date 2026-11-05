package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.world.FieldUiSnapshot;

/** Pure projection rule: only the server-authored navigation target may appear on the minimap. */
final class DrehmalMinimapProjection {
    record Marker(double dx, double dz, int distance, String label, boolean clamped) {}

    private DrehmalMinimapProjection() {}

    static Marker navigation(FieldUiSnapshot.Navigation navigation, double playerX, double playerZ, double radius) {
        if (navigation == null || !navigation.active() || !Double.isFinite(radius) || radius <= 0.0) return null;
        double actualDx = navigation.x() - playerX;
        double actualDz = navigation.z() - playerZ;
        int distance = (int)Math.round(Math.hypot(actualDx, actualDz));

        double dx = actualDx;
        double dz = actualDz;
        double maxAxis = Math.max(Math.abs(dx), Math.abs(dz));
        boolean clamped = maxAxis > radius;
        if (clamped) {
            double scale = radius / maxAxis;
            dx *= scale;
            dz *= scale;
        }
        return new Marker(dx, dz, distance, navigation.label(), clamped);
    }
}
