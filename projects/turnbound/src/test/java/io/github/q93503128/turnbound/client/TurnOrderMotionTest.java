package io.github.q93503128.turnbound.client;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TurnOrderMotionTest {
    @Test
    void ordinaryAdvanceKeepsTheLaterDuplicateAttachedToItsRealOccurrence() {
        List<TurnOrderMotion.Move> moves = TurnOrderMotion.plan(
                List.of("A", "B", "C", "A", "D"),
                List.of("B", "C", "A", "D", "E"), 7);

        assertEquals(1, moves.get(0).fromIndex());
        assertEquals(2, moves.get(1).fromIndex());
        assertEquals(3, moves.get(2).fromIndex());
        assertEquals(4, moves.get(3).fromIndex());
        assertEquals(4, moves.get(4).fromIndex()); // new entrant appears at destination
        assertFalse(moves.get(0).tempoJump());
    }

    @Test
    void gaugeReorderRecoversTheMovedTokenInsteadOfSnappingIt() {
        List<TurnOrderMotion.Move> moves = TurnOrderMotion.plan(
                List.of("A", "B", "C", "D", "E"),
                List.of("A", "D", "B", "C", "E"), 7);

        TurnOrderMotion.Move d = moves.get(1);
        assertEquals("D", d.unitId());
        assertEquals(3, d.fromIndex());
        assertEquals(1, d.toIndex());
        assertTrue(d.tempoJump());
    }

    @Test
    void clippingAndEasingStayBoundedForHudUse() {
        List<TurnOrderMotion.Move> moves = TurnOrderMotion.plan(
                List.of("A", "B", "C", "D", "E", "F", "G", "H"),
                List.of("H", "A", "B", "C", "D", "E", "F", "G"), 7);
        assertEquals(7, moves.size());
        assertEquals(0.0, TurnOrderMotion.easedProgress(-20, 260), 0.0001);
        assertTrue(TurnOrderMotion.easedProgress(130, 260) > 0.5);
        assertEquals(1.0, TurnOrderMotion.easedProgress(400, 260), 0.0001);
    }
}
