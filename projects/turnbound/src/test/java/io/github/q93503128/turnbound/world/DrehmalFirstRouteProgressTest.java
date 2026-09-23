package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrehmalFirstRouteProgressTest {
    @Test
    void laterLandmarksImplyEarlierRouteMilestones() {
        var tower = site("turnbound:site/capital_valley/tower", "BREATHING_ZONE");
        var camp = site("turnbound:site/capital_valley/explorer_camp", "REST_ZONE");
        var approach = site("turnbound:site/capital_valley/drabyel_approach", "PATROL_ZONE");
        var hub = site("turnbound:site/capital_valley/new_drabyel", "HUB_SAFE");

        assertEquals(List.of(DrehmalFirstRouteProgress.TOWER_REACHED),
                DrehmalFirstRouteProgress.milestonesFor(tower));
        assertEquals(List.of(
                        DrehmalFirstRouteProgress.TOWER_REACHED,
                        DrehmalFirstRouteProgress.CAMP_REACHED),
                DrehmalFirstRouteProgress.milestonesFor(camp));
        assertTrue(DrehmalFirstRouteProgress.milestonesFor(approach)
                .contains(DrehmalFirstRouteProgress.APPROACH_REACHED));
        assertEquals(List.of(
                        DrehmalFirstRouteProgress.TOWER_REACHED,
                        DrehmalFirstRouteProgress.CAMP_REACHED,
                        DrehmalFirstRouteProgress.APPROACH_REACHED,
                        DrehmalFirstRouteProgress.HUB_REACHED),
                DrehmalFirstRouteProgress.milestonesFor(hub));
    }

    private static DrehmalFirstRouteCatalog.Site site(String locator, String kind) {
        return new DrehmalFirstRouteCatalog.Site(
                locator, kind, "turnbound:landmark/primal_caverns", "test",
                new DrehmalFirstRouteCatalog.Position(0, 70, 0),
                24, 0, true, true);
    }
}
