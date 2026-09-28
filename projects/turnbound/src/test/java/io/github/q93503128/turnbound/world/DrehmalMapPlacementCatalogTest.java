package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;
import java.util.Set;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

class DrehmalMapPlacementCatalogTest {
    @Test void catalogIsValid() {
        assertTrue(DrehmalMapPlacementCatalog.validate().isEmpty(),()->String.join("; ",DrehmalMapPlacementCatalog.validate()));
    }
    @Test void routeHasPurposefulZones() {
        var zones=DrehmalMapPlacementCatalog.plan().zones();
        assertEquals(6,zones.size());
        assertEquals("roadhead_reveal",zones.getFirst().id());
        assertEquals("drabyel_hub",zones.getLast().id());
        Set<String> roles=zones.stream().map(DrehmalMapPlacementCatalog.Zone::role).collect(Collectors.toSet());
        assertTrue(roles.containsAll(Set.of("SAFE_REVEAL","FIRST_COMBAT","CHOICE_ELITE","BREATHING","ROAD_PATROL","SAFE_HUB")));
    }
    @Test void encounterZonesHaveFallbacks() {
        var first=DrehmalMapPlacementCatalog.placement("turnbound:site/capital_valley/first_common");
        var cave=DrehmalMapPlacementCatalog.placement("turnbound:site/capital_valley/warning_cave");
        var road=DrehmalMapPlacementCatalog.placement("turnbound:site/capital_valley/drabyel_approach");
        assertNotNull(first);assertNotNull(cave);assertNotNull(road);
        assertNotEquals(first.zoneId(),cave.zoneId());assertNotEquals(cave.zoneId(),road.zoneId());
        assertTrue(first.arenaSeeds().size()>=4);assertTrue(cave.arenaSeeds().size()>=4);assertTrue(road.arenaSeeds().size()>=4);
        assertTrue(road.patrolSeeds().size()>=4);
    }
    @Test void sourceRevisionIsPinned() {
        assertEquals("zachaa/DrehmalMap",DrehmalMapPlacementCatalog.plan().source().repository());
        assertEquals("72d82180cbe3f950f068cf2d8e8668c6b09d5c58",DrehmalMapPlacementCatalog.plan().source().commit());
    }
}
