package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.runtime.HunterQuarryFocusRuntimeState;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HunterQuarryFocusRuntimeStateTest {
    private static final long NO_COMBAT = Long.MIN_VALUE / 4;

    @Test
    void longRangeHitMarksQuarryAndRespectsFocusIcd() {
        var state = new HunterQuarryFocusRuntimeState();
        UUID target = UUID.randomUUID();

        var first = state.recordRangedHit(
                target,
                7.0,
                false,
                100L,
                NO_COMBAT
        );
        assertTrue(first.newlyMarked());
        assertTrue(first.longRangeEligible());
        assertTrue(first.longRangeFocusGranted());
        assertEquals(1, first.focusAfter());

        var insideIcd = state.recordRangedHit(
                target,
                9.0,
                false,
                114L,
                100L
        );
        assertFalse(insideIcd.longRangeFocusGranted());
        assertEquals(1, insideIcd.focusAfter());

        var afterIcd = state.recordRangedHit(
                target,
                9.0,
                false,
                115L,
                114L
        );
        assertTrue(afterIcd.longRangeFocusGranted());
        assertEquals(2, afterIcd.focusAfter());
    }

    @Test
    void weakPointHasIndependentFocusAndUltimateIcds() {
        var state = new HunterQuarryFocusRuntimeState();
        UUID target = UUID.randomUUID();

        var first = state.recordRangedHit(
                target,
                3.0,
                true,
                0L,
                NO_COMBAT
        );
        assertTrue(first.weakPointFocusGranted());
        assertTrue(first.weakPointUltimatePublicationClaimed());
        assertEquals(1, first.focusAfter());

        var ultimateReadyBeforeFocus = state.recordRangedHit(
                target,
                3.0,
                true,
                20L,
                19L
        );
        assertFalse(
                ultimateReadyBeforeFocus.weakPointFocusGranted()
        );
        assertTrue(
                ultimateReadyBeforeFocus
                        .weakPointUltimatePublicationClaimed()
        );
        assertEquals(
                1,
                ultimateReadyBeforeFocus.focusAfter()
        );

        var focusReady = state.recordRangedHit(
                target,
                3.0,
                true,
                30L,
                29L
        );
        assertTrue(focusReady.weakPointFocusGranted());
        assertFalse(
                focusReady.weakPointUltimatePublicationClaimed()
        );
        assertEquals(2, focusReady.focusAfter());
    }

    @Test
    void directHpDamageRemovesExactlyOneFocus() {
        var state = new HunterQuarryFocusRuntimeState();
        UUID target = UUID.randomUUID();
        state.recordRangedHit(
                target,
                8.0,
                true,
                0L,
                NO_COMBAT
        );

        var loss = state.onDirectHpDamage(1L, 0L);
        assertEquals(2, loss.focusBefore());
        assertEquals(1, loss.focusAfter());

        var second = state.onDirectHpDamage(2L, 1L);
        assertEquals(1, second.focusBefore());
        assertEquals(0, second.focusAfter());

        var atZero = state.onDirectHpDamage(3L, 2L);
        assertEquals(0, atZero.focusBefore());
        assertEquals(0, atZero.focusAfter());
    }

    @Test
    void quarryExpiryClearsFocusAndNextHitStartsFresh() {
        var state = new HunterQuarryFocusRuntimeState();
        UUID firstTarget = UUID.randomUUID();
        UUID secondTarget = UUID.randomUUID();

        state.recordRangedHit(
                firstTarget,
                8.0,
                true,
                0L,
                NO_COMBAT
        );
        assertEquals(
                2,
                state.snapshot(159L, 158L).focus()
        );
        assertTrue(
                state.snapshot(160L, 159L)
                        .quarryId()
                        .isEmpty()
        );
        assertEquals(
                0,
                state.snapshot(160L, 159L).focus()
        );

        var next = state.recordRangedHit(
                secondTarget,
                8.0,
                false,
                160L,
                159L
        );
        assertEquals(1, next.focusAfter());
        assertTrue(next.newlyMarked());
    }

    @Test
    void replacingLiveQuarryPreservesFocusUntilExplicitClearRule() {
        var state = new HunterQuarryFocusRuntimeState();
        UUID firstTarget = UUID.randomUUID();
        UUID secondTarget = UUID.randomUUID();

        state.recordRangedHit(
                firstTarget,
                8.0,
                false,
                0L,
                NO_COMBAT
        );
        var replaced = state.recordRangedHit(
                secondTarget,
                3.0,
                false,
                5L,
                4L
        );

        assertTrue(replaced.replacedPreviousQuarry());
        assertEquals(1, replaced.focusBefore());
        assertEquals(1, replaced.focusAfter());
        assertEquals(
                secondTarget,
                state.snapshot(5L, 4L)
                        .quarryId()
                        .orElseThrow()
        );
    }

    @Test
    void outOfCombatAndSpenderRulesClearFocusWithoutExtraStacks() {
        var state = new HunterQuarryFocusRuntimeState();
        UUID target = UUID.randomUUID();

        state.recordRangedHit(
                target,
                8.0,
                true,
                0L,
                NO_COMBAT
        );
        state.recordRangedHit(
                target,
                8.0,
                false,
                15L,
                14L
        );
        assertEquals(3, state.snapshot(15L, 14L).focus());

        assertTrue(
                state.consumeFocusSpenderIfFull(16L, 15L)
        );
        assertEquals(0, state.snapshot(16L, 15L).focus());
        assertFalse(
                state.consumeFocusSpenderIfFull(17L, 16L)
        );

        state.recordRangedHit(
                target,
                8.0,
                false,
                30L,
                29L
        );
        assertEquals(1, state.snapshot(30L, 29L).focus());
        assertEquals(
                0,
                state.snapshot(190L, 30L).focus()
        );
    }
}
