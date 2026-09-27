package io.github.q93503128.turnbound.world;

import java.util.ArrayList;
import java.util.List;

/** Data-only route survey plan that is safe to inspect without loading Minecraft runtime classes. */
public final class DrehmalRouteSurveyPlan {
    private DrehmalRouteSurveyPlan() {}

    public static List<String> routeSeedLines() {
        List<String> lines = new ArrayList<>();
        for (DrehmalFirstRouteCatalog.Site site : DrehmalFirstRouteCatalog.route().sites()) {
            DrehmalWorldProfile.Anchor anchor = DrehmalWorldProfile.enabled(site.surveySeedAnchor());
            String coordinate = anchor == null ? "missing" : anchor.x() + " " + anchor.y() + " " + anchor.z();
            String state = site.productionEnabled() ? "PRODUCTION" : site.verifiedIn26_2() ? "VERIFIED" : "PENDING";
            lines.add(site.playerLabel() + " · " + state + " · seed " + coordinate
                    + " · " + site.surveySeedAnchor());
        }
        return List.copyOf(lines);
    }
}
