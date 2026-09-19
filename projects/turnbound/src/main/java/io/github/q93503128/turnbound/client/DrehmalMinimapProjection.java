package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.world.FieldUiSnapshot;

/** Pure projection rule: the minimap may only expose the server-authored verified navigation target. */
final class DrehmalMinimapProjection {
    record Marker(double dx, double dz, int distance, String label) {}

    private DrehmalMinimapProjection() {}

    static Marker navigation(FieldUiSnapshot.Navigation navigation, double playerX, double playerZ, double radius) {
        if (navigation == null || !navigation.active() || !Double.isFinite(radius) || radius <= 0.0) return null;
        double dx = navigation.x() - playerX;
        double dz = navigation.z() - playerZ;
        if (Math.abs(dx) > radius || Math.abs(dz) > radius) return null;
        return new Marker(dx, dz, (int)Math.round(Math.hypot(dx, dz)), navigation.label());
    }
}
