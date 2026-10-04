package dev.moonseungjun.openworldrpg.multiplayer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProjectDownedRuntimeStateTest {
    private static final UUID A = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID B = UUID.fromString("20000000-0000-0000-0000-000000000002");

    @Test
    void canonicalRescueReviveAndFatigueTiming() {
        var state = new ProjectDownedRuntimeState();
        assertEquals(ProjectDownedRuntimeState.EnterStatus.ENTERED, state.tryEnter(100L));
        assertEquals(300L, state.snapshot(100L).rescueTicksRemaining());
        assertEquals(ProjectDownedRuntimeState.BeginReviveStatus.STARTED, state.tryBeginRevive(A, 200L));
        assertEquals(50L, state.snapshot(200L).reviveChannelTicksRemaining());
        assertEquals(ProjectDownedRuntimeState.CompleteReviveStatus.TOO_EARLY, state.tryCompleteRevive(A, 249L));
        assertEquals(ProjectDownedRuntimeState.CompleteReviveStatus.REVIVED, state.tryCompleteRevive(A, 250L));
        assertFalse(state.downed());
        assertEquals(400L, state.snapshot(250L).rescueFatigueTicksRemaining());
        assertEquals(ProjectDownedRuntimeState.EnterStatus.RESCUE_FATIGUE, state.tryEnter(649L));
        assertEquals(ProjectDownedRuntimeState.EnterStatus.ENTERED, state.tryEnter(650L));
    }

    @Test
    void interruptionKeepsDownedAndAllowsNewReviver() {
        var state = new ProjectDownedRuntimeState();
        state.tryEnter(0L);
        assertEquals(ProjectDownedRuntimeState.BeginReviveStatus.STARTED, state.tryBeginRevive(A, 20L));
        assertFalse(state.interruptRevive(B));
        assertTrue(state.interruptRevive(A));
        assertTrue(state.downed());
        assertTrue(state.reviverId().isEmpty());
        assertEquals(ProjectDownedRuntimeState.BeginReviveStatus.STARTED, state.tryBeginRevive(B, 30L));
    }

    @Test
    void expiredWindowRemainsDownedUntilDefeatAuthorityClearsIt() {
        var state = new ProjectDownedRuntimeState();
        state.tryEnter(10L);
        assertFalse(state.snapshot(309L).rescueExpired());
        assertTrue(state.snapshot(310L).rescueExpired());
        assertTrue(state.downed());
        assertEquals(ProjectDownedRuntimeState.BeginReviveStatus.RESCUE_EXPIRED, state.tryBeginRevive(A, 310L));
        state.clearAfterDefeat();
        assertFalse(state.downed());
    }

    @Test
    void channelMustFinishBeforeRescueDeadlineAndOwnerMustMatch() {
        var state = new ProjectDownedRuntimeState();
        state.tryEnter(0L);
        assertEquals(ProjectDownedRuntimeState.BeginReviveStatus.INSUFFICIENT_RESCUE_TIME, state.tryBeginRevive(A, 250L));

        state.clearAfterDefeat();
        state.tryEnter(0L);
        state.tryBeginRevive(A, 10L);
        assertEquals(ProjectDownedRuntimeState.CompleteReviveStatus.NOT_CHANNEL_OWNER, state.tryCompleteRevive(B, 60L));
        assertTrue(state.downed());
    }
}
