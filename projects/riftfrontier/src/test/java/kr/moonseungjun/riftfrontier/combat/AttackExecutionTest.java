package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.content.ContentId;
import kr.moonseungjun.riftfrontier.content.CoreDefinition;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class AttackExecutionTest {
    private static CoreDefinition.AttackPattern pattern(int recoveryTicks) {
        return new CoreDefinition.AttackPattern(
            ContentId.parse("riftfrontier:timeline_test"),
            "arc_melee",
            3,
            2,
            recoveryTicks,
            Set.of("backstep", "guard"),
            "full_body_windup"
        );
    }

    @Test
    void oneCadenceDrivesTelegraphHitWindowRecoveryAndCompletion() {
        AttackExecution attack = AttackExecution.start(pattern(2), 100);

        AttackExecution.Snapshot telegraph = attack.sample(102);
        assertEquals(AttackTimeline.Phase.TELEGRAPH, telegraph.presentationPhase());
        assertFalse(telegraph.mayApplyHit());
        assertEquals(1, telegraph.timeline().phaseTicksRemaining());

        AttackExecution.Snapshot activeStart = attack.sample(103);
        assertEquals(AttackTimeline.Phase.ACTIVE, activeStart.presentationPhase());
        assertTrue(activeStart.mayApplyHit());
        assertEquals(0, activeStart.timeline().phaseTick());
        assertEquals("arc_melee", activeStart.delivery());
        assertEquals("full_body_windup", activeStart.presentationCue());

        AttackExecution.Snapshot activeEnd = attack.sample(104);
        assertTrue(activeEnd.mayApplyHit());
        assertEquals(1, activeEnd.timeline().phaseTicksRemaining());

        AttackExecution.Snapshot recovery = attack.sample(105);
        assertEquals(AttackTimeline.Phase.RECOVERY, recovery.presentationPhase());
        assertFalse(recovery.mayApplyHit());

        AttackExecution.Snapshot complete = attack.sample(107);
        assertEquals(AttackTimeline.Phase.COMPLETE, complete.presentationPhase());
        assertFalse(complete.mayApplyHit());
        assertEquals(107, attack.completesAtGameTick());
    }

    @Test
    void zeroRecoveryTransitionsDirectlyFromActiveToComplete() {
        AttackExecution attack = AttackExecution.start(pattern(0), 20);
        assertEquals(AttackTimeline.Phase.ACTIVE, attack.sample(23).presentationPhase());
        assertEquals(AttackTimeline.Phase.ACTIVE, attack.sample(24).presentationPhase());
        assertEquals(AttackTimeline.Phase.COMPLETE, attack.sample(25).presentationPhase());
    }

    @Test
    void runtimeRejectsTimeTravelAndPreservesPatternMetadata() {
        AttackExecution attack = AttackExecution.start(pattern(2), 50);
        assertThrows(IllegalArgumentException.class, () -> attack.sample(49));
        assertThrows(IllegalArgumentException.class, () -> new AttackTimeline(pattern(2)).sample(-1));
        assertEquals(ContentId.parse("riftfrontier:timeline_test"), attack.patternId());
        assertEquals(Set.of("backstep", "guard"), attack.sample(50).counterplay());
    }
}
