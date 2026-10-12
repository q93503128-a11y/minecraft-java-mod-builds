package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GachaPresentationFlowTest {
    @Test
    void holdNeverAdvancesWithoutExplicitInput() {
        var flow = new GachaPresentationFlow(List.of(new GachaPresentationFlow.Pull(5, true)));
        tickUntil(flow, GachaPresentationFlow.Stage.HOLD);
        assertEquals(0, flow.currentIndex());
        for (int i = 0; i < 2_000; i++) flow.tick();
        assertEquals(GachaPresentationFlow.Stage.HOLD, flow.stage());
        assertEquals(0, flow.currentIndex());
    }

    @Test
    void advanceMovesExactlyOnePullAndNeverSkipsAnother() {
        var flow = new GachaPresentationFlow(List.of(
                new GachaPresentationFlow.Pull(3, false),
                new GachaPresentationFlow.Pull(4, true),
                new GachaPresentationFlow.Pull(1, false)));
        tickUntil(flow, GachaPresentationFlow.Stage.HOLD);
        flow.advance();
        assertEquals(1, flow.currentIndex());
        assertEquals(GachaPresentationFlow.Stage.SILHOUETTE, flow.stage());
        tickUntil(flow, GachaPresentationFlow.Stage.HOLD);
        assertEquals(1, flow.currentIndex());
        flow.advance();
        assertEquals(2, flow.currentIndex());
    }

    @Test
    void lastHoldAdvancesToSummary() {
        var flow = new GachaPresentationFlow(List.of(new GachaPresentationFlow.Pull(2, false)));
        tickUntil(flow, GachaPresentationFlow.Stage.HOLD);
        flow.advance();
        assertEquals(GachaPresentationFlow.Stage.SUMMARY, flow.stage());
    }

    @Test
    void skipPreservesPullIndexAndJumpsOnlyToSummary() {
        var flow = new GachaPresentationFlow(List.of(
                new GachaPresentationFlow.Pull(4, false),
                new GachaPresentationFlow.Pull(5, true)));
        for (int i = 0; i < 7; i++) flow.tick();
        flow.skip();
        assertEquals(GachaPresentationFlow.Stage.SUMMARY, flow.stage());
        assertEquals(0, flow.currentIndex());
        assertEquals(2, flow.pullCount());
    }

    @Test
    void tenPullIndexNeverChangesFromTicksAlone() {
        List<GachaPresentationFlow.Pull> pulls = new ArrayList<>();
        for (int i = 0; i < 10; i++) pulls.add(new GachaPresentationFlow.Pull(i == 7 ? 5 : 2, i == 7));
        var flow = new GachaPresentationFlow(pulls);
        tickUntil(flow, GachaPresentationFlow.Stage.HOLD);
        for (int i = 0; i < 5_000; i++) flow.tick();
        assertEquals(0, flow.currentIndex());
        assertEquals(GachaPresentationFlow.Stage.HOLD, flow.stage());
    }

    @Test
    void fiveStarBatchMasqueradesAsFourStarUntilGoldBreak() {
        var flow = new GachaPresentationFlow(List.of(new GachaPresentationFlow.Pull(5, true)));
        tickUntil(flow, GachaPresentationFlow.Stage.OMEN);
        assertEquals(4, flow.omenStars());
        tickUntil(flow, GachaPresentationFlow.Stage.BREAK);
        assertFalse(flow.fiveStarUpgrade());
        assertEquals(4, flow.breakDisplayStars());
        for (int i = 0; i < 5; i++) flow.tick();
        assertTrue(flow.fiveStarUpgrade());
        assertEquals(5, flow.breakDisplayStars());
    }

    @Test
    void fourStarBatchNeverUsesGoldUpgrade() {
        var flow = new GachaPresentationFlow(List.of(new GachaPresentationFlow.Pull(4, true)));
        tickUntil(flow, GachaPresentationFlow.Stage.BREAK);
        while (flow.stage() == GachaPresentationFlow.Stage.BREAK) {
            assertFalse(flow.fiveStarUpgrade());
            assertEquals(4, flow.breakDisplayStars());
            flow.tick();
        }
    }

    private static void tickUntil(GachaPresentationFlow flow, GachaPresentationFlow.Stage target) {
        for (int guard = 0; guard < 2_000 && flow.stage() != target; guard++) flow.tick();
        assertEquals(target, flow.stage(), "flow did not reach " + target);
    }
}
