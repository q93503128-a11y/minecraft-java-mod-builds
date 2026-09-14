package kr.moonseungjun.riftfrontier.expedition;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class Region01FieldArenaPlanTest {
    @Test
    void staggeredCoverKeepsObjectiveAndTraversalLanesOpen() {
        var covers = Region01FieldArenaPlan.coverPillars();
        assertEquals(4, covers.size());
        assertTrue(covers.stream().allMatch(cover -> cover.height() == 2));
        assertTrue(covers.stream().anyMatch(cover -> cover.dx() < 0));
        assertTrue(covers.stream().anyMatch(cover -> cover.dx() > 0));

        Set<String> blocked = covers.stream()
            .map(cover -> cover.dx() + "," + cover.dz())
            .collect(java.util.stream.Collectors.toSet());

        assertFalse(blocked.contains("0,-4"), "Arrival lane must stay open");
        assertFalse(blocked.contains("0,0"), "Central salvage must stay open");
        assertFalse(blocked.contains("0,5"), "Extraction relay must stay open");
        assertFalse(blocked.contains("-3,-3"));
        assertFalse(blocked.contains("3,-3"));
        assertFalse(blocked.contains("-3,3"));
        assertFalse(blocked.contains("3,3"));
    }

    @Test
    void floorPlanPreservesRelayOwnedApproachAndMarksOpenCombatLanes() {
        var floor = Region01FieldArenaPlan.floorCells();
        assertEquals(88, floor.size(), "11-wide field across z=-5..2 must remain bounded");
        assertTrue(floor.stream().noneMatch(cell -> cell.dz() > Region01FieldArenaPlan.PRESENTATION_MAX_Z));
        assertTrue(floor.stream().anyMatch(cell -> cell.dx() == 0 && cell.dz() == -4 && cell.role() == Region01FieldArenaPlan.FloorRole.OPEN_LANE));
        assertTrue(floor.stream().anyMatch(cell -> cell.dx() == 0 && cell.dz() == 0 && cell.role() == Region01FieldArenaPlan.FloorRole.OPEN_LANE));
        assertTrue(floor.stream().anyMatch(cell -> cell.dx() == 4 && cell.dz() == 0 && cell.role() == Region01FieldArenaPlan.FloorRole.COMBAT_FIELD));
    }
}
