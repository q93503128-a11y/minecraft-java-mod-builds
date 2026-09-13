package kr.moonseungjun.riftfrontier.expedition;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpeditionFieldExtractionRelayTest {
    @Test
    void relayUsesFarEdgeOfExistingTechnicalCellWithoutOverlappingSalvageNodes() {
        BlockPos center = ExpeditionGameplayService.technicalRegionCenter();
        BlockPos relay = ExpeditionFieldExtractionRelay.relayPosition();

        assertEquals(center.offset(0, 0, 5), relay);
        assertEquals(center.getY(), relay.getY());
        assertTrue(Math.abs(relay.getX() - center.getX()) <= 5);
        assertTrue(Math.abs(relay.getZ() - center.getZ()) <= 5);

        int[][] salvageNodes = {{-3,-3},{3,-3},{-3,3},{3,3},{0,0}};
        for (int[] node : salvageNodes) {
            assertTrue(!relay.equals(center.offset(node[0], 0, node[1])), "relay must not replace a salvage node");
        }
    }
}
