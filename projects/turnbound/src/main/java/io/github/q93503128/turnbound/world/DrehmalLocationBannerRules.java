package io.github.q93503128.turnbound.world;

import java.util.List;
import java.util.Set;

/** Pure first-route location-title eligibility/radius rules for the R_PG-style transient area banner. */
final class DrehmalLocationBannerRules {
    private static final Set<String> BANNER_KINDS = Set.of(
            "START_CANDIDATE",
            "BREATHING_ZONE",
            "ELITE_ZONE",
            "REST_ZONE",
            "PATROL_ZONE",
            "HUB_SAFE");

    private DrehmalLocationBannerRules() {}

    static boolean eligible(DrehmalFirstRouteCatalog.Site site) {
        return site != null
                && site.productionEnabled()
                && site.verifiedIn26_2()
                && site.runtimePosition() != null
                && BANNER_KINDS.contains(site.kind())
                && radius(site) > 0
                && !site.playerLabel().isBlank();
    }

    static double radius(DrehmalFirstRouteCatalog.Site site) {
        if (site == null) return 0.0D;
        return Math.max(site.safetyRadius(), site.encounterRadius());
    }

    static DrehmalFirstRouteCatalog.Site current(List<DrehmalFirstRouteCatalog.Site> sites, double x, double z) {
        if (sites == null || sites.isEmpty()) return null;
        DrehmalFirstRouteCatalog.Site best = null;
        double bestDistanceSq = Double.MAX_VALUE;
        for (DrehmalFirstRouteCatalog.Site site : sites) {
            if (!eligible(site)) continue;
            double dx = x - (site.runtimePosition().x() + 0.5D);
            double dz = z - (site.runtimePosition().z() + 0.5D);
            double distanceSq = dx * dx + dz * dz;
            double radius = radius(site);
            if (distanceSq <= radius * radius && distanceSq < bestDistanceSq) {
                best = site;
                bestDistanceSq = distanceSq;
            }
        }
        return best;
    }
}
