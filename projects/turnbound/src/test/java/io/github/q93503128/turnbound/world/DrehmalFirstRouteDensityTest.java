package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DrehmalFirstRouteDensityTest {
    @Test void firstRouteHasOpenWorldDensityWithoutHubCombatSpam(){
        var route=DrehmalFirstRouteCatalog.route();
        long common=route.encounters().stream().filter(e->"COMMON".equals(e.tier())).count();
        long elite=route.encounters().stream().filter(e->"ELITE".equals(e.tier())).count();
        assertEquals(6,common);
        assertEquals(1,elite);
        assertEquals(3,DrehmalFieldNpcCatalog.all().size());
        assertTrue(route.encounters().stream().noneMatch(e->e.siteLocator().endsWith("/new_drabyel")));
        assertTrue(route.encounters().stream().noneMatch(e->e.siteLocator().endsWith("/explorer_camp")));
    }
}
