package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrehmalFieldEncounterPolicyTest {
    @Test
    void currentCapitalValleyBeatsMapToDistinctFieldArchetypes() {
        var first = DrehmalFirstRouteCatalog.encounterByCombatId("CV_FIRST_COMMON");
        var cave = DrehmalFirstRouteCatalog.encounterByCombatId("CV_WARNING_CAVE_ELITE");
        var road = DrehmalFirstRouteCatalog.encounterByCombatId("CV_DRABYEL_ROAD");

        var firstPolicy = DrehmalFieldEncounterPolicy.forEncounter(
                first, DrehmalFirstRouteCatalog.site(first.siteLocator()));
        var cavePolicy = DrehmalFieldEncounterPolicy.forEncounter(
                cave, DrehmalFirstRouteCatalog.site(cave.siteLocator()));
        var roadPolicy = DrehmalFieldEncounterPolicy.forEncounter(
                road, DrehmalFirstRouteCatalog.site(road.siteLocator()));

        assertEquals(DrehmalFieldEncounterPolicy.Archetype.ROADSIDE_THREAT, firstPolicy.archetype());
        assertEquals(DrehmalFieldEncounterPolicy.Archetype.OPTIONAL_DANGER, cavePolicy.archetype());
        assertEquals(DrehmalFieldEncounterPolicy.Archetype.PATROL, roadPolicy.archetype());
    }

    @Test
    void warningBeatStaysBriefWhileOptionalEliteGetsMoreReadingTime() {
        var first = DrehmalFirstRouteCatalog.encounterByCombatId("CV_FIRST_COMMON");
        var cave = DrehmalFirstRouteCatalog.encounterByCombatId("CV_WARNING_CAVE_ELITE");
        var road = DrehmalFirstRouteCatalog.encounterByCombatId("CV_DRABYEL_ROAD");

        int roadsideTicks = DrehmalFieldEncounterPolicy.forEncounter(
                first, DrehmalFirstRouteCatalog.site(first.siteLocator())).alertPreludeTicks();
        int eliteTicks = DrehmalFieldEncounterPolicy.forEncounter(
                cave, DrehmalFirstRouteCatalog.site(cave.siteLocator())).alertPreludeTicks();
        int patrolTicks = DrehmalFieldEncounterPolicy.forEncounter(
                road, DrehmalFirstRouteCatalog.site(road.siteLocator())).alertPreludeTicks();

        assertTrue(roadsideTicks > 0 && roadsideTicks < 20);
        assertTrue(patrolTicks > 0 && patrolTicks < 20);
        assertTrue(eliteTicks > roadsideTicks);
    }
}
