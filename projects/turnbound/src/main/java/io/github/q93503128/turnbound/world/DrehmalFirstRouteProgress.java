package io.github.q93503128.turnbound.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Durable first-route landmarks. These are presentation/navigation milestones, not hard quest gates.
 *
 * <p>Recording is cumulative so a reconnect, admin relocation, or later map migration can never make the HUD point
 * a progressed player backward along Capital Valley.</p>
 */
final class DrehmalFirstRouteProgress {
    static final String TOWER_REACHED = "ROUTE_TOWER_REACHED";
    static final String CAMP_REACHED = "ROUTE_CAMP_REACHED";
    static final String APPROACH_REACHED = "ROUTE_DRABYEL_APPROACH";
    static final String HUB_REACHED = "ROUTE_DRABYEL_REACHED";

    private static final String TOWER = "turnbound:site/capital_valley/tower";
    private static final String CAMP = "turnbound:site/capital_valley/explorer_camp";
    private static final String APPROACH = "turnbound:site/capital_valley/drabyel_approach";
    private static final String HUB = "turnbound:site/capital_valley/new_drabyel";

    private DrehmalFirstRouteProgress() {}

    static List<String> milestonesFor(DrehmalFirstRouteCatalog.Site site) {
        if (site == null || site.locator() == null) return List.of();
        List<String> out = new ArrayList<>();
        switch (site.locator()) {
            case TOWER -> out.add(TOWER_REACHED);
            case CAMP -> {
                out.add(TOWER_REACHED);
                out.add(CAMP_REACHED);
            }
            case APPROACH -> {
                out.add(TOWER_REACHED);
                out.add(CAMP_REACHED);
                out.add(APPROACH_REACHED);
            }
            case HUB -> {
                out.add(TOWER_REACHED);
                out.add(CAMP_REACHED);
                out.add(APPROACH_REACHED);
                out.add(HUB_REACHED);
            }
            default -> { }
        }
        return List.copyOf(out);
    }

    static void record(ExternalWorldSavedData data, UUID playerId, DrehmalFirstRouteCatalog.Site site) {
        if (data == null || playerId == null || site == null) return;
        for (String flag : milestonesFor(site)) data.markOnboardingFlag(playerId, flag);
    }

    static boolean reached(Set<String> flags, String milestone) {
        return flags != null && milestone != null && flags.contains(milestone);
    }
}
