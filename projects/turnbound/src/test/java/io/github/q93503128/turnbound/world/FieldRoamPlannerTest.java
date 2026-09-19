package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldRoamPlannerTest {
    private static final List<FieldRoamPlanner.Point> POINTS = List.of(
            new FieldRoamPlanner.Point(0, 0),
            new FieldRoamPlanner.Point(2, 0),
            new FieldRoamPlanner.Point(8, 0),
            new FieldRoamPlanner.Point(0, 9));

    @Test
    void roamSkipsTinyNextHopWhenAReadableMoveExists() {
        int next = FieldRoamPlanner.nextIndex(POINTS, 0, 12L, 36.0D);
        assertNotEquals(0, next);
        var candidate = POINTS.get(next);
        assertTrue(candidate.x() * candidate.x() + candidate.z() * candidate.z() >= 36.0D);
    }

    @Test
    void roamIsDeterministicForSharedMultiplayerPresentation() {
        assertEquals(
                FieldRoamPlanner.nextIndex(POINTS, 2, 9981L, 36.0D),
                FieldRoamPlanner.nextIndex(POINTS, 2, 9981L, 36.0D));
    }

    @Test
    void dwellAlwaysStaysInsideAuthoredRange() {
        for (long seed = 0; seed < 100; seed++) {
            int dwell = FieldRoamPlanner.dwellTicks(30, 90, seed);
            assertTrue(dwell >= 30 && dwell <= 90);
        }
    }
}
