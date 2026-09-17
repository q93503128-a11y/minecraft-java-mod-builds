package kr.moonseungjun.riftfrontier.expedition;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class Region01FieldArenaPlanTest {
    @Test void staggeredCoverKeepsObjectiveAndTraversalLanesOpen() {
        var covers = Region01FieldArenaPlan.coverPillars();
        assertEquals(4, covers.size());
        assertTrue(covers.stream().allMatch(cover -> cover.height() == 2));
        Set<String> blocked = covers.stream().map(cover -> cover.dx() + "," + cover.dz()).collect(java.util.stream.Collectors.toSet());
        assertFalse(blocked.contains("0,-4")); assertFalse(blocked.contains("0,0")); assertFalse(blocked.contains("0,5"));
        Region01FieldArenaPlan.salvageNodes().forEach(node -> assertFalse(blocked.contains(key(node)), "Salvage node must stay clear of cover"));
    }

    @Test void floorPlanPreservesRelayOwnedApproachAndMarksOpenCombatLanes() {
        var floor = Region01FieldArenaPlan.floorCells();
        assertEquals(88, floor.size());
        assertTrue(floor.stream().noneMatch(cell -> cell.dz() > Region01FieldArenaPlan.PRESENTATION_MAX_Z));
        assertTrue(floor.stream().anyMatch(cell -> cell.dx() == 0 && cell.dz() == -4 && cell.role() == Region01FieldArenaPlan.FloorRole.OPEN_LANE));
        assertTrue(floor.stream().anyMatch(cell -> cell.dx() == 0 && cell.dz() == 0 && cell.role() == Region01FieldArenaPlan.FloorRole.OPEN_LANE));
        assertTrue(floor.stream().anyMatch(cell -> cell.dx() == 4 && cell.dz() == 0 && cell.role() == Region01FieldArenaPlan.FloorRole.COMBAT_FIELD));
    }

    @Test void salvagePlanPreservesThreeRequiredPlusTwoOptionalRiskRewardNodes() {
        var nodes = Region01FieldArenaPlan.salvageNodes();
        assertEquals(5, nodes.size());
        assertEquals(5, nodes.stream().map(Region01FieldArenaPlanTest::key).distinct().count());
        assertTrue(nodes.stream().allMatch(node -> Math.abs(node.dx()) <= Region01FieldArenaPlan.FIELD_RADIUS && Math.abs(node.dz()) <= Region01FieldArenaPlan.FIELD_RADIUS));
        assertEquals(2, Region01FieldArenaPlan.remainingSalvageNodes(3));
        assertEquals(1, Region01FieldArenaPlan.remainingSalvageNodes(4));
        assertEquals(0, Region01FieldArenaPlan.remainingSalvageNodes(5));
        assertEquals(0, Region01FieldArenaPlan.remainingSalvageNodes(99));
    }

    @Test void threatSpawnStagingUsesCombatSpaceWithoutBlockingObjectivesOrCover() {
        var hunters = Region01FieldArenaPlan.hunterSpawnCells(); var scouts = Region01FieldArenaPlan.scoutSpawnCells(); var elite = Region01FieldArenaPlan.eliteSpawnCell();
        assertEquals(3, hunters.size()); assertEquals(3, scouts.size());
        Set<String> occupied = new HashSet<>(); hunters.forEach(spawn -> assertTrue(occupied.add(key(spawn)))); scouts.forEach(spawn -> assertTrue(occupied.add(key(spawn)))); assertTrue(occupied.add(key(elite)));
        Set<String> blocked = Region01FieldArenaPlan.coverPillars().stream().map(cover -> cover.dx() + "," + cover.dz()).collect(java.util.stream.Collectors.toSet());
        Set<String> reservedObjectives = new HashSet<>(); reservedObjectives.add("0,-4"); reservedObjectives.add("0,5"); Region01FieldArenaPlan.salvageNodes().forEach(node -> reservedObjectives.add(key(node)));
        for (String spawn : occupied) { assertFalse(blocked.contains(spawn)); assertFalse(reservedObjectives.contains(spawn)); }
        hunters.forEach(Region01FieldArenaPlanTest::assertInsideCombatOwnedSpace); scouts.forEach(Region01FieldArenaPlanTest::assertInsideCombatOwnedSpace); assertInsideCombatOwnedSpace(elite);
    }

    private static void assertInsideCombatOwnedSpace(Region01FieldArenaPlan.SpawnCell spawn) { assertTrue(Math.abs(spawn.dx()) <= Region01FieldArenaPlan.FIELD_RADIUS); assertTrue(spawn.dz() >= -Region01FieldArenaPlan.FIELD_RADIUS); assertTrue(spawn.dz() <= Region01FieldArenaPlan.PRESENTATION_MAX_Z); }
    private static String key(Region01FieldArenaPlan.SpawnCell spawn) { return spawn.dx() + "," + spawn.dz(); }
}
