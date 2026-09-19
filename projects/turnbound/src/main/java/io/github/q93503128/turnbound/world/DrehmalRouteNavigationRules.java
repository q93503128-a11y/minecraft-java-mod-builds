package io.github.q93503128.turnbound.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Server-side first-route waypoint policy.
 *
 * <p>Only 26.2-surveyed production sites may become HUD coordinates. Optional danger and combat-placement sites are
 * deliberately excluded so the main-route cue does not drag the player into side content or expose survey drafts.</p>
 */
final class DrehmalRouteNavigationRules {
    private static final Set<String> MAIN_ROUTE_KINDS = Set.of(
            "START_CANDIDATE",
            "BREATHING_ZONE",
            "REST_ZONE",
            "PATROL_ZONE",
            "HUB_SAFE");

    private DrehmalRouteNavigationRules() {}

    static FieldUiSnapshot.Navigation target(
            List<DrehmalFirstRouteCatalog.Site> sites,
            double playerX,
            double playerZ
    ) {
        if (sites == null || sites.isEmpty()) return FieldUiSnapshot.Navigation.none();

        List<DrehmalFirstRouteCatalog.Site> route = new ArrayList<>();
        for (var site : sites) if (eligible(site)) route.add(site);
        if (route.isEmpty()) return FieldUiSnapshot.Navigation.none();

        int nearestIndex = 0;
        double nearestDistanceSq = Double.MAX_VALUE;
        for (int i = 0; i < route.size(); i++) {
            var site = route.get(i);
            double distanceSq = horizontalDistanceSq(site, playerX, playerZ);
            if (distanceSq < nearestDistanceSq) {
                nearestDistanceSq = distanceSq;
                nearestIndex = i;
            }
        }

        var nearest = route.get(nearestIndex);
        boolean reached = nearestDistanceSq <= advanceRadiusSq(nearest);
        int targetIndex = reached ? nearestIndex + 1 : nearestIndex;
        if (targetIndex >= route.size()) return FieldUiSnapshot.Navigation.none();

        var target = route.get(targetIndex);
        var position = target.runtimePosition();
        return new FieldUiSnapshot.Navigation(
                target.locator(),
                target.playerLabel(),
                position.x() + 0.5D,
                position.z() + 0.5D);
    }

    static boolean eligible(DrehmalFirstRouteCatalog.Site site) {
        return site != null
                && site.productionEnabled()
                && site.verifiedIn26_2()
                && site.runtimePosition() != null
                && MAIN_ROUTE_KINDS.contains(site.kind())
                && !site.playerLabel().isBlank()
                && !site.playerLabel().contains("후보");
    }

    private static double horizontalDistanceSq(
            DrehmalFirstRouteCatalog.Site site,
            double playerX,
            double playerZ
    ) {
        double dx = playerX - (site.runtimePosition().x() + 0.5D);
        double dz = playerZ - (site.runtimePosition().z() + 0.5D);
        return dx * dx + dz * dz;
    }

    private static double advanceRadiusSq(DrehmalFirstRouteCatalog.Site site) {
        double radius = Math.max(12.0D, Math.max(site.safetyRadius(), site.encounterRadius()));
        return radius * radius;
    }
}
