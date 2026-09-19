package io.github.q93503128.turnbound.world;

import java.util.List;

/** Horizontal safety-ring policy shared by Drehmal exploration, encounter runtime and route validation. */
final class DrehmalRouteZoneRules {
    private DrehmalRouteZoneRules() {}

    static boolean insideSafetyZone(List<DrehmalFirstRouteCatalog.Site> sites, double x, double z) {
        if (sites == null || sites.isEmpty()) return false;
        for (DrehmalFirstRouteCatalog.Site site : sites) {
            if (site == null || !site.productionEnabled() || !site.verifiedIn26_2()
                    || site.safetyRadius() <= 0 || site.runtimePosition() == null) {
                continue;
            }
            double dx = x - (site.runtimePosition().x() + 0.5D);
            double dz = z - (site.runtimePosition().z() + 0.5D);
            double radius = site.safetyRadius();
            if (dx * dx + dz * dz <= radius * radius) return true;
        }
        return false;
    }
}
