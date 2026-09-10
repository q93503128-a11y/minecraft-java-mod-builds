package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.content.ContentId;
import kr.moonseungjun.riftfrontier.content.CoreDefinition;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class AttackStateMachineTest {
    private static CoreDefinition.AttackPattern pattern() {
        return new CoreDefinition.AttackPattern(
            ContentId.parse("riftfrontier:state_machine_test"),
            "slam",
            2,
            2,
            1,
            Set.of("backstep"),
            "raised_slam"
        );
    }

    @Test
    void emitsPhaseBoundariesFromTheSharedTimelineAndFinishesOnce() {
        AttackStateMachine machine = new AttackStateMachine();
        assertFalse(machine.isExecuting());
        assertTrue(machine.advance(0).snapshot().isEmpty());

        AttackExecution.Snapshot begin = machine.begin(pattern(), 10);
        assertEquals(AttackTimeline.Phase.TELEGRAPH, begin.presentationPhase());
        assertTrue(machine.isExecuting());

        AttackStateMachine.Step telegraph = machine.advance(11);
        assertFalse(telegraph.phaseChanged());
        assertFalse(telegraph.hitWindowOpen());

        AttackStateMachine.Step active = machine.advance(12);
        assertTrue(active.phaseChanged());
        assertEquals(AttackTimeline.Phase.ACTIVE, active.snapshot().orElseThrow().presentationPhase());
        assertTrue(active.hitWindowOpen());

        AttackStateMachine.Step recovery = machine.advance(14);
        assertTrue(recovery.phaseChanged());
        assertEquals(AttackTimeline.Phase.RECOVERY, recovery.snapshot().orElseThrow().presentationPhase());
        assertFalse(recovery.hitWindowOpen());

        AttackStateMachine.Step complete = machine.advance(15);
        assertTrue(complete.phaseChanged());
        assertTrue(complete.finished());
        assertEquals(AttackTimeline.Phase.COMPLETE, complete.snapshot().orElseThrow().presentationPhase());
        assertFalse(machine.isExecuting());

        AttackStateMachine.Step after = machine.advance(16);
        assertTrue(after.snapshot().isEmpty());
        assertFalse(after.finished());
    }

    @Test
    void rejectsOverlapAndSupportsExplicitInterruption() {
        AttackStateMachine machine = new AttackStateMachine();
        machine.begin(pattern(), 30);
        assertThrows(IllegalStateException.class, () -> machine.begin(pattern(), 31));
        assertTrue(machine.cancel().isPresent());
        assertFalse(machine.isExecuting());
        assertTrue(machine.cancel().isEmpty());
        assertDoesNotThrow(() -> machine.begin(pattern(), 40));
    }

    @Test
    void acceptsRepeatedSameTickSamplingButRejectsAndCancelsClockRewind() {
        AttackStateMachine machine = new AttackStateMachine();
        machine.begin(pattern(), 50);

        AttackStateMachine.Step first = machine.advance(52);
        assertEquals(AttackTimeline.Phase.ACTIVE, first.snapshot().orElseThrow().presentationPhase());
        assertDoesNotThrow(() -> machine.advance(52));

        IllegalArgumentException rewind = assertThrows(
            IllegalArgumentException.class,
            () -> machine.advance(51)
        );
        assertTrue(rewind.getMessage().contains("cannot move backwards"));
        assertFalse(machine.isExecuting(), "rewind must invalidate the authoritative execution");
        assertTrue(machine.currentExecution().isEmpty());

        assertDoesNotThrow(() -> machine.begin(pattern(), 60));
    }

    @Test
    void readOnlyObservationSharesTheMonotonicClockBoundaryWithoutConsumingPhaseTransition() {
        AttackStateMachine machine = new AttackStateMachine();
        machine.begin(pattern(), 70);

        AttackExecution.Snapshot observed = machine.observeCurrent(74).orElseThrow();
        assertEquals(AttackTimeline.Phase.RECOVERY, observed.presentationPhase());

        AttackStateMachine.Step sameTickAdvance = machine.advance(74);
        assertTrue(sameTickAdvance.phaseChanged(), "observation must not consume lifecycle phase transition bookkeeping");
        assertEquals(AttackTimeline.Phase.RECOVERY, sameTickAdvance.snapshot().orElseThrow().presentationPhase());

        machine.observeCurrent(75);
        IllegalArgumentException rewind = assertThrows(
            IllegalArgumentException.class,
            () -> machine.observeCurrent(74)
        );
        assertTrue(rewind.getMessage().contains("cannot move backwards"));
        assertFalse(machine.isExecuting(), "rewound observation must invalidate the same authoritative execution");
    }

    @Test
    void rejectsNegativeBeginTickWithoutCreatingExecution() {
        AttackStateMachine machine = new AttackStateMachine();
        assertThrows(IllegalArgumentException.class, () -> machine.begin(pattern(), -1));
        assertFalse(machine.isExecuting());
    }
}
