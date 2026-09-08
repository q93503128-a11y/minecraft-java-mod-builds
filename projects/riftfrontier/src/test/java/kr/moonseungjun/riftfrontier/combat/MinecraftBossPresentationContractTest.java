package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.content.ContentId;
import kr.moonseungjun.riftfrontier.content.CoreDefinition;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class MinecraftBossPresentationContractTest {
    @Test
    void presentationFrameIsDerivedFromAuthoritativeAttackSnapshot() {
        CoreDefinition.AttackPattern pattern = new CoreDefinition.AttackPattern(
            ContentId.parse("riftfrontier:presentation_contract"),
            "line_charge",
            4,
            3,
            5,
            Set.of("sidestep", "interrupt"),
            "lowered_charge"
        );
        AttackExecution execution = AttackExecution.start(pattern, 100L);

        var telegraph = MinecraftBossCombatAdapter.PresentationFrame.from(1, execution.sample(102L));
        assertEquals(1, telegraph.bossPhase());
        assertEquals(pattern.id(), telegraph.patternId());
        assertEquals(AttackTimeline.Phase.TELEGRAPH, telegraph.attackPhase());
        assertEquals(0.5D, telegraph.phaseProgress(), 0.000001D);
        assertEquals("lowered_charge", telegraph.presentationCue());
        assertEquals("line_charge", telegraph.delivery());
        assertEquals(Set.of("sidestep", "interrupt"), telegraph.counterplay());
        assertFalse(telegraph.hitWindowOpen());

        var active = MinecraftBossCombatAdapter.PresentationFrame.from(1, execution.sample(105L));
        assertEquals(AttackTimeline.Phase.ACTIVE, active.attackPhase());
        assertEquals(1.0D / 3.0D, active.phaseProgress(), 0.000001D);
        assertTrue(active.hitWindowOpen(), "presentation ACTIVE must be the same authoritative hit window as damage");

        var recovery = MinecraftBossCombatAdapter.PresentationFrame.from(2, execution.sample(109L));
        assertEquals(2, recovery.bossPhase());
        assertEquals(AttackTimeline.Phase.RECOVERY, recovery.attackPhase());
        assertEquals(0.4D, recovery.phaseProgress(), 0.000001D);
        assertFalse(recovery.hitWindowOpen());
    }

    @Test
    void presentationFrameRejectsIndependentOrImpossibleTimingState() {
        ContentId patternId = ContentId.parse("riftfrontier:presentation_contract");
        assertThrows(IllegalArgumentException.class, () -> new MinecraftBossCombatAdapter.PresentationFrame(
            1,
            patternId,
            AttackTimeline.Phase.TELEGRAPH,
            0.25D,
            "cue",
            "slam",
            Set.of("step_out"),
            true
        ));
        assertThrows(IllegalArgumentException.class, () -> new MinecraftBossCombatAdapter.PresentationFrame(
            0,
            patternId,
            AttackTimeline.Phase.ACTIVE,
            0.0D,
            "cue",
            "slam",
            Set.of(),
            true
        ));
        assertThrows(IllegalArgumentException.class, () -> new MinecraftBossCombatAdapter.PresentationFrame(
            1,
            patternId,
            AttackTimeline.Phase.ACTIVE,
            1.1D,
            "cue",
            "slam",
            Set.of(),
            true
        ));
    }
}
