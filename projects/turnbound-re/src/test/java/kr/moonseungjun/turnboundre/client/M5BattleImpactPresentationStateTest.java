package kr.moonseungjun.turnboundre.client;

import kr.moonseungjun.turnboundre.battle.BattleEvent;
import kr.moonseungjun.turnboundre.network.BattleNetworkPayloads;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class M5BattleImpactPresentationStateTest {
    private static final long T0 = 4_000_000_000L;
    private static final long EVENT_EPOCH = 7L;

    @AfterEach
    void clearState() {
        BattleImpactPresentationState.clear();
        BattleActionTimelineState.clear();
    }

    @Test
    void damageHoldsPreviousThroughWindupEasesOnceAtImpactAndIsExactInRecovery() {
        UUID battleId = UUID.randomUUID();
        actionTimeline(battleId, 2L, 10, T0, "e1",
                new BattleEvent(1L, "DAMAGE", "p1", "target=e1 hp=30"),
                new BattleEvent(1L, "DAMAGE", "p1", "target=e1 hp=30"));
        var before = snapshot(battleId, 1L, participant("e1", 100, 50, false, true));
        var after = snapshot(battleId, 2L, participant("e1", 40, 50, false, true));

        BattleImpactPresentationState.acceptSnapshot(before, after, EVENT_EPOCH, T0);

        assertEquals(100, displayed(after, EVENT_EPOCH, T0 + BattleActionTimelineState.WINDUP_NANOS / 2L, "e1").hp());
        assertEquals(70, displayed(after, EVENT_EPOCH,
                T0 + BattleActionTimelineState.WINDUP_NANOS + BattleActionTimelineState.IMPACT_NANOS / 2L, "e1").hp());
        assertEquals(40, displayed(after, EVENT_EPOCH,
                T0 + BattleActionTimelineState.WINDUP_NANOS + BattleActionTimelineState.IMPACT_NANOS + 1L, "e1").hp());
    }

    @Test
    void healingUsesTheSameAuthoritativePreviousToFinalProjection() {
        UUID battleId = UUID.randomUUID();
        actionTimeline(battleId, 5L, 20, T0, "p1");
        var before = snapshot(battleId, 4L, participant("p1", 40, 30, false, true));
        var after = snapshot(battleId, 5L, participant("p1", 70, 30, false, true));

        BattleImpactPresentationState.acceptSnapshot(before, after, EVENT_EPOCH, T0);

        assertEquals(40, displayed(after, EVENT_EPOCH, T0 + 10_000_000L, "p1").hp());
        assertEquals(55, displayed(after, EVENT_EPOCH,
                T0 + BattleActionTimelineState.WINDUP_NANOS + BattleActionTimelineState.IMPACT_NANOS / 2L, "p1").hp());
        assertEquals(70, displayed(after, EVENT_EPOCH,
                T0 + BattleActionTimelineState.WINDUP_NANOS + BattleActionTimelineState.IMPACT_NANOS + 1L, "p1").hp());
    }

    @Test
    void poiseDamageEasesAtImpactWithoutInventingIntermediateServerHits() {
        UUID battleId = UUID.randomUUID();
        actionTimeline(battleId, 9L, 30, T0, "e1");
        var before = snapshot(battleId, 8L, participant("e1", 100, 50, false, true));
        var after = snapshot(battleId, 9L, participant("e1", 100, 10, false, true));

        BattleImpactPresentationState.acceptSnapshot(before, after, EVENT_EPOCH, T0);

        assertEquals(50, displayed(after, EVENT_EPOCH, T0 + BattleActionTimelineState.WINDUP_NANOS - 1L, "e1").poise());
        assertEquals(30, displayed(after, EVENT_EPOCH,
                T0 + BattleActionTimelineState.WINDUP_NANOS + BattleActionTimelineState.IMPACT_NANOS / 2L, "e1").poise());
        assertEquals(10, displayed(after, EVENT_EPOCH,
                T0 + BattleActionTimelineState.WINDUP_NANOS + BattleActionTimelineState.IMPACT_NANOS + 1L, "e1").poise());
    }

    @Test
    void poiseBreakAndExposedDoNotAppearBeforeImpact() {
        UUID battleId = UUID.randomUUID();
        actionTimeline(battleId, 12L, 40, T0, "e1");
        var before = snapshot(battleId, 11L, participant("e1", 100, 25, false, true));
        var after = snapshot(battleId, 12L, participant("e1", 100, 0, true, true));

        BattleImpactPresentationState.acceptSnapshot(before, after, EVENT_EPOCH, T0);

        var windup = displayed(after, EVENT_EPOCH, T0 + BattleActionTimelineState.WINDUP_NANOS / 2L, "e1");
        assertEquals(25, windup.poise());
        assertFalse(windup.exposed());

        var lateImpact = displayed(after, EVENT_EPOCH,
                T0 + BattleActionTimelineState.WINDUP_NANOS + BattleActionTimelineState.IMPACT_NANOS * 3L / 4L, "e1");
        assertTrue(lateImpact.exposed());
        assertTrue(lateImpact.poise() < 25);

        var recovery = displayed(after, EVENT_EPOCH,
                T0 + BattleActionTimelineState.WINDUP_NANOS + BattleActionTimelineState.IMPACT_NANOS + 1L, "e1");
        assertEquals(0, recovery.poise());
        assertTrue(recovery.exposed());
    }

    @Test
    void defeatRemainsAliveUntilImpactCommitsThenUsesExactFinalState() {
        UUID battleId = UUID.randomUUID();
        actionTimeline(battleId, 15L, 50, T0, "e1");
        var before = snapshot(battleId, 14L, participant("e1", 30, 5, false, true));
        var after = snapshot(battleId, 15L, participant("e1", 0, 0, false, false));

        BattleImpactPresentationState.acceptSnapshot(before, after, EVENT_EPOCH, T0);

        assertTrue(displayed(after, EVENT_EPOCH, T0 + BattleActionTimelineState.WINDUP_NANOS, "e1").alive());
        var lateImpact = displayed(after, EVENT_EPOCH,
                T0 + BattleActionTimelineState.WINDUP_NANOS + BattleActionTimelineState.IMPACT_NANOS * 3L / 4L, "e1");
        assertFalse(lateImpact.alive());
        assertTrue(lateImpact.hp() < 30);
        var recovery = displayed(after, EVENT_EPOCH,
                T0 + BattleActionTimelineState.WINDUP_NANOS + BattleActionTimelineState.IMPACT_NANOS + 1L, "e1");
        assertFalse(recovery.alive());
        assertEquals(0, recovery.hp());
    }

    @Test
    void noCueAndNonTargetChangesStayImmediate() {
        UUID noCueBattle = UUID.randomUUID();
        var noCueBefore = snapshot(noCueBattle, 1L, participant("e1", 100, 50, false, true));
        var noCueAfter = snapshot(noCueBattle, 2L, participant("e1", 60, 20, false, true));
        BattleImpactPresentationState.acceptSnapshot(noCueBefore, noCueAfter, EVENT_EPOCH, T0);
        assertEquals(60, displayed(noCueAfter, EVENT_EPOCH, T0 + 1L, "e1").hp());
        assertEquals(20, displayed(noCueAfter, EVENT_EPOCH, T0 + 1L, "e1").poise());

        BattleImpactPresentationState.clear();
        BattleActionTimelineState.clear();
        UUID targetedBattle = UUID.randomUUID();
        actionTimeline(targetedBattle, 4L, 60, T0, "e2");
        var before = snapshot(targetedBattle, 3L,
                participant("e1", 100, 50, false, true), participant("e2", 100, 50, false, true));
        var after = snapshot(targetedBattle, 4L,
                participant("e1", 65, 30, false, true), participant("e2", 80, 40, false, true));
        BattleImpactPresentationState.acceptSnapshot(before, after, EVENT_EPOCH, T0);

        assertEquals(65, displayed(after, EVENT_EPOCH, T0 + 1L, "e1").hp());
        assertEquals(100, displayed(after, EVENT_EPOCH, T0 + 1L, "e2").hp());
    }

    @Test
    void multiTargetParticipantsProjectIndependentlyFromTheSameAggregateSnapshot() {
        UUID battleId = UUID.randomUUID();
        actionTimeline(battleId, 22L, 70, T0, "e1,e2");
        var before = snapshot(battleId, 21L,
                participant("e1", 100, 50, false, true), participant("e2", 80, 40, false, true));
        var after = snapshot(battleId, 22L,
                participant("e1", 70, 20, false, true), participant("e2", 20, 10, false, true));
        BattleImpactPresentationState.acceptSnapshot(before, after, EVENT_EPOCH, T0);

        assertEquals(100, displayed(after, EVENT_EPOCH, T0 + 1L, "e1").hp());
        assertEquals(80, displayed(after, EVENT_EPOCH, T0 + 1L, "e2").hp());

        long midpoint = T0 + BattleActionTimelineState.WINDUP_NANOS + BattleActionTimelineState.IMPACT_NANOS / 2L;
        assertEquals(85, displayed(after, EVENT_EPOCH, midpoint, "e1").hp());
        assertEquals(50, displayed(after, EVENT_EPOCH, midpoint, "e2").hp());
        assertEquals(35, displayed(after, EVENT_EPOCH, midpoint, "e1").poise());
        assertEquals(25, displayed(after, EVENT_EPOCH, midpoint, "e2").poise());
    }

    @Test
    void battleAndRevisionChangesCannotLeakStagedValues() {
        UUID oldBattle = UUID.randomUUID();
        actionTimeline(oldBattle, 2L, 80, T0, "e1");
        var before = snapshot(oldBattle, 1L, participant("e1", 100, 50, false, true));
        var after = snapshot(oldBattle, 2L, participant("e1", 50, 20, false, true));
        BattleImpactPresentationState.acceptSnapshot(before, after, EVENT_EPOCH, T0);
        assertEquals(100, displayed(after, EVENT_EPOCH, T0 + 1L, "e1").hp());

        UUID newBattle = UUID.randomUUID();
        var fresh = snapshot(newBattle, 1L, participant("e1", 90, 45, false, true));
        BattleImpactPresentationState.acceptSnapshot(after, fresh, EVENT_EPOCH, T0 + 2L);
        assertEquals(90, displayed(fresh, EVENT_EPOCH, T0 + 3L, "e1").hp());

        BattleActionTimelineState.clear();
        actionTimeline(newBattle, 2L, 81, T0 + 10L, "e1");
        var rev2 = snapshot(newBattle, 2L, participant("e1", 60, 30, false, true));
        BattleImpactPresentationState.acceptSnapshot(fresh, rev2, EVENT_EPOCH + 1L, T0 + 10L);
        assertEquals(90, displayed(rev2, EVENT_EPOCH + 1L, T0 + 11L, "e1").hp());

        var rev3 = snapshot(newBattle, 3L, participant("e1", 25, 10, false, true));
        BattleImpactPresentationState.acceptSnapshot(rev2, rev3, EVENT_EPOCH + 1L, T0 + 12L);
        assertEquals(25, displayed(rev3, EVENT_EPOCH + 1L, T0 + 13L, "e1").hp());
    }

    @Test
    void cueReplacementOrInterruptionImmediatelyDropsStaleProjection() {
        UUID battleId = UUID.randomUUID();
        actionTimeline(battleId, 30L, 90, T0, "e1");
        var before = snapshot(battleId, 29L, participant("e1", 100, 50, false, true));
        var after = snapshot(battleId, 30L, participant("e1", 45, 15, false, true));
        BattleImpactPresentationState.acceptSnapshot(before, after, EVENT_EPOCH, T0);
        assertEquals(100, displayed(after, EVENT_EPOCH, T0 + 1L, "e1").hp());

        BattleActionTimelineState.acceptEvents(events(battleId, 30L, 91,
                new BattleEvent(30L, "COMMAND_ACCEPTED", "p2", "replacement"),
                new BattleEvent(30L, "ACTION_PRESENTATION", "p2",
                        "action=replacement kind=SKILL tag=PROJECTILE team=ENEMY shape=SINGLE count=1 targets=e2")), T0 + 2L);
        assertEquals(45, displayed(after, EVENT_EPOCH + 1L, T0 + 3L, "e1").hp());

        BattleActionTimelineState.clear();
        assertEquals(45, displayed(after, EVENT_EPOCH + 1L, T0 + 4L, "e1").hp());
    }

    private static void actionTimeline(
            UUID battleId,
            long revision,
            int fromIndex,
            long nowNanos,
            String targets,
            BattleEvent... extraEvents
    ) {
        List<BattleEvent> events = new java.util.ArrayList<>();
        events.add(new BattleEvent(revision, "COMMAND_ACCEPTED", "p1", "action"));
        events.add(new BattleEvent(revision, "ACTION_PRESENTATION", "p1",
                "action=action kind=SKILL tag=MELEE team=ENEMY shape=MULTI count=2 targets=" + targets));
        events.addAll(List.of(extraEvents));
        BattleActionTimelineState.acceptEvents(events(battleId, revision, fromIndex, events.toArray(BattleEvent[]::new)), nowNanos);
    }

    private static BattleNetworkPayloads.DecodedEvents events(
            UUID battleId,
            long revision,
            int fromIndex,
            BattleEvent... events
    ) {
        return new BattleNetworkPayloads.DecodedEvents(battleId, revision, fromIndex, List.of(events));
    }

    private static BattleNetworkPayloads.DecodedSnapshot snapshot(
            UUID battleId,
            long revision,
            BattleNetworkPayloads.SnapshotParticipant... participants
    ) {
        return new BattleNetworkPayloads.DecodedSnapshot(
                battleId, revision, "AWAIT_COMMAND", 1, "p1", List.of(participants), List.of());
    }

    private static BattleNetworkPayloads.SnapshotParticipant participant(
            String id,
            int hp,
            int poise,
            boolean exposed,
            boolean alive
    ) {
        return new BattleNetworkPayloads.SnapshotParticipant(
                id, hp, 100, poise, 50, 0, false, exposed, false, alive,
                id.startsWith("p") ? "PLAYER" : "ENEMY",
                id.startsWith("p") ? 0 : Math.max(1, id.charAt(id.length() - 1) - '0'),
                "turnbound_re:zombie", List.of(), null, null);
    }

    private static BattleNetworkPayloads.SnapshotParticipant displayed(
            BattleNetworkPayloads.DecodedSnapshot authoritative,
            long eventEpoch,
            long nowNanos,
            String participantId
    ) {
        return BattleImpactPresentationState.project(authoritative, eventEpoch, nowNanos)
                .participants().stream()
                .filter(participant -> participant.id().equals(participantId))
                .findFirst()
                .orElseThrow();
    }
}
