package kr.moonseungjun.riftfrontier.expedition;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
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

    @Test
    void threatSpawnStagingUsesCombatSpaceWithoutBlockingObjectivesOrCover() {
        var hunters = Region01FieldArenaPlan.hunterSpawnCells();
        var scouts = Region01FieldArenaPlan.scoutSpawnCells();
        var elite = Region01FieldArenaPlan.eliteSpawnCell();

        assertEquals(3, hunters.size(), "Pressure plan can request up to three hunters");
        assertEquals(3, scouts.size(), "Pressure plan can request up to three scouts");
        assertTrue(hunters.stream().allMatch(spawn -> spawn.dx() < 0), "Hunters stage on the west flank");
        assertTrue(scouts.stream().allMatch(spawn -> spawn.dx() > 0), "Scouts stage on the east flank");

        Set<String> occupied = new HashSet<>();
        hunters.forEach(spawn -> assertTrue(occupied.add(key(spawn)), "Hunter spawn cells must be unique"));
        scouts.forEach(spawn -> assertTrue(occupied.add(key(spawn)), "Scout spawn cells must be unique"));
        assertTrue(occupied.add(key(elite)), "Elite spawn must not overlap another role");

        Set<String> blocked = Region01FieldArenaPlan.coverPillars().stream()
            .map(cover -> cover.dx() + "," + cover.dz())
            .collect(java.util.stream.Collectors.toSet());
        Set<String> reservedObjectives = Set.of(
            "0,-4",
            "0,0",
            "0,5",
            "-3,-3",
            "3,-3",
            "-3,3",
            "3,3"
        );

        for (String spawn : occupied) {
            assertFalse(blocked.contains(spawn), "Threat spawn must not overlap cover: " + spawn);
            assertFalse(reservedObjectives.contains(spawn), "Threat spawn must not occupy an objective: " + spawn);
        }

        hunters.forEach(Region01FieldArenaPlanTest::assertInsideCombatOwnedSpace);
        scouts.forEach(Region01FieldArenaPlanTest::assertInsideCombatOwnedSpace);
        assertInsideCombatOwnedSpace(elite);
    }

    private static void assertInsideCombatOwnedSpace(Region01FieldArenaPlan.SpawnCell spawn) {
        assertTrue(Math.abs(spawn.dx()) <= Region01FieldArenaPlan.FIELD_RADIUS);
        assertTrue(spawn.dz() >= -Region01FieldArenaPlan.FIELD_RADIUS);
        assertTrue(spawn.dz() <= Region01FieldArenaPlan.PRESENTATION_MAX_Z);
    }

    private static String key(Region01FieldArenaPlan.SpawnCell spawn) {
        return spawn.dx() + "," + spawn.dz();
    }
}
