package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RadiaAuthoredTopologyTest {
    @Test
    void hubFacilitiesTutorialPadsAndApproachDoorsStayOnAuthoredLand() {
        int[][] anchors = {
                {0, 12}, {0, 24},
                {-56, 22}, {56, 22}, {-57, 55}, {57, 38}, {-82, -54},
                {0, 104}, {-28, -47}, {22, -49}, {72, -11},
                {50, 49}, {50, 59}, {50, 69},
                {62, 48}, {62, 59}, {62, 70},
                {-44, 20}, {44, 20}, {63, -21},
                {0, -62}
        };
        for (int[] anchor : anchors) {
            assertTrue(AsterMarchTerrainPlan.radiaLand(anchor[0], anchor[1]),
                    "authored Radia anchor must stay on land: " + anchor[0] + "," + anchor[1]);
        }
    }

    @Test
    void chapterReturnsAndSouthGateSeamStayPhysicallyWalkable() {
        int[][] arrivals = {
                {0, -104},
                {-121, 20},
                {119, -80},
                {0, 106},
                {0, 118}
        };
        for (int[] arrival : arrivals) {
            assertTrue(AsterMarchTerrainPlan.radiaLand(arrival[0], arrival[1]),
                    "chapter arrival must stay on authored land: " + arrival[0] + "," + arrival[1]);
        }
    }
}
