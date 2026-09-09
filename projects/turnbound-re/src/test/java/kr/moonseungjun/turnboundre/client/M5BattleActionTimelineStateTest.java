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

class M5BattleActionTimelineStateTest {
    private static final long T0 = 2_000_000_000L;

    @AfterEach
    void clearState() {
        BattleActionTimelineState.clear();
        BattleStageFeedbackState.clear();
    }

    @Test
    void authoritativePlayerAndEnemyEventsBecomeSequentialActionBeats() {
        UUID battleId = UUID.randomUUID();
        BattleActionTimelineState.acceptEvents(events(battleId, 12L, 4,
                new BattleEvent(5L, "COMMAND_ACCEPTED", "p1", "turnbound_re:slash"),
                new BattleEvent(5L, "ACTION_PRESENTATION", "p1",
                        "action=turnbound_re:slash kind=BASIC tag=MELEE targets=e1"),
                new BattleEvent(6L, "DAMAGE", "p1", "target=e1 hp=18"),
                new BattleEvent(9L, "AI_COMMAND", "e1", "turnbound_re:bite"),
                new BattleEvent(9L, "ACTION_PRESENTATION", "e1",
                        "action=turnbound_re:bite kind=BASIC tag=MELEE targets=p1"),
                new BattleEvent(10L, "DAMAGE", "e1", "target=p1 hp=11")), T0);

        BattleActionTimelineState.Cue windup = BattleActionTimelineState
                .cue(battleId, T0 + 10_000_000L).orElseThrow();
        assertEquals("p1", windup.actorId());
        assertEquals("turnbound_re:slash", windup.actionId());
        assertEquals(List.of("e1"), windup.targetIds());
        assertEquals(BattleActionTimelineState.MotionStyle.CLOSE, windup.motionStyle());
        assertEquals(BattleActionTimelineState.Phase.WINDUP, windup.phase());
        assertEquals(0, windup.beatIndex());
        assertEquals(2, windup.beatCount());

        BattleActionTimelineState.Cue impact = BattleActionTimelineState
                .cue(battleId, T0 + BattleActionTimelineState.WINDUP_NANOS + 1L).orElseThrow();
        assertEquals(BattleActionTimelineState.Phase.IMPACT, impact.phase());

        BattleActionTimelineState.Cue enemy = BattleActionTimelineState
                .cue(battleId, T0 + BattleActionTimelineState.BEAT_NANOS + 1L).orElseThrow();
        assertEquals("e1", enemy.actorId());
        assertEquals("turnbound_re:bite", enemy.actionId());
        assertEquals(List.of("p1"), enemy.targetIds());
        assertEquals(BattleActionTimelineState.MotionStyle.CLOSE, enemy.motionStyle());
        assertEquals(1, enemy.beatIndex());

        assertFalse(BattleActionTimelineState.isPlaying(
                battleId, T0 + BattleActionTimelineState.BEAT_NANOS * 2L));
    }

    @Test
    void presentationMetadataClassifiesMotionWithoutUsingActionNames() {
        UUID battleId = UUID.randomUUID();
        BattleActionTimelineState.acceptEvents(events(battleId, 40L, 20,
                new BattleEvent(31L, "COMMAND_ACCEPTED", "p1", "anything"),
                new BattleEvent(31L, "ACTION_PRESENTATION", "p1",
                        "action=anything kind=SKILL tag=PROJECTILE targets=e1,e2")), T0);

        BattleActionTimelineState.Cue cue = BattleActionTimelineState.cue(battleId, T0 + 1L).orElseThrow();
        assertEquals(BattleActionTimelineState.MotionStyle.RANGED, cue.motionStyle());
        assertEquals(List.of("e1", "e2"), cue.targetIds());
    }

    @Test
    void castTagsUseCastMotionAndUnknownMetadataStaysUtility() {
        UUID castBattle = UUID.randomUUID();
        BattleActionTimelineState.acceptEvents(events(castBattle, 50L, 30,
                new BattleEvent(41L, "COMMAND_ACCEPTED", "p1", "spell"),
                new BattleEvent(41L, "ACTION_PRESENTATION", "p1",
                        "action=spell kind=SKILL tag=ARCANE targets=e1")), T0);
        assertEquals(BattleActionTimelineState.MotionStyle.CAST,
                BattleActionTimelineState.cue(castBattle, T0 + 1L).orElseThrow().motionStyle());

        UUID utilityBattle = UUID.randomUUID();
        BattleActionTimelineState.acceptEvents(events(utilityBattle, 60L, 40,
                new BattleEvent(51L, "COMMAND_ACCEPTED", "p1", "guard")), T0 + 100L);
        assertEquals(BattleActionTimelineState.MotionStyle.UTILITY,
                BattleActionTimelineState.cue(utilityBattle, T0 + 101L).orElseThrow().motionStyle());
    }

    @Test
    void impactDelaysTrackEachNamedTargetWithoutPredictingUnknownTargets() {
        UUID battleId = UUID.randomUUID();
        BattleActionTimelineState.acceptEvents(events(battleId, 20L, 8,
                new BattleEvent(13L, "COMMAND_ACCEPTED", "p1", "slash"),
                new BattleEvent(14L, "DAMAGE", "p1", "target=e1 hp=20"),
                new BattleEvent(18L, "AI_COMMAND", "e1", "bite"),
                new BattleEvent(19L, "DAMAGE", "e1", "target=p1 hp=9")), T0);

        assertEquals(BattleActionTimelineState.WINDUP_NANOS,
                BattleActionTimelineState.firstImpactDelayNanos(battleId, "e1", T0));
        assertEquals(BattleActionTimelineState.BEAT_NANOS + BattleActionTimelineState.WINDUP_NANOS,
                BattleActionTimelineState.firstImpactDelayNanos(battleId, "p1", T0));
        assertEquals(0L, BattleActionTimelineState.firstImpactDelayNanos(battleId, "missing", T0));
    }

    @Test
    void snapshotFeedbackWaitsForTheMatchingActionImpactBeat() {
        UUID battleId = UUID.randomUUID();
        BattleActionTimelineState.acceptEvents(events(battleId, 30L, 11,
                new BattleEvent(21L, "COMMAND_ACCEPTED", "p1", "slash"),
                new BattleEvent(22L, "DAMAGE", "p1", "target=e1 hp=20"),
                new BattleEvent(27L, "AI_COMMAND", "e1", "bite"),
                new BattleEvent(28L, "DAMAGE", "e1", "target=p1 hp=10")), T0);

        BattleNetworkPayloads.DecodedSnapshot before = snapshot(
                battleId, 20L, participant("p1", 100, 50), participant("e1", 100, 50));
        BattleNetworkPayloads.DecodedSnapshot after = snapshot(
                battleId, 30L, participant("p1", 90, 50), participant("e1", 80, 50));
        BattleStageFeedbackState.acceptSnapshot(null, before, T0 - 1L);
        BattleStageFeedbackState.acceptSnapshot(before, after, T0);

        assertTrue(BattleStageFeedbackState.cue(
                battleId, "p1", T0 + BattleActionTimelineState.WINDUP_NANOS + 10_000_000L).isEmpty());
        assertEquals(-20, BattleStageFeedbackState.cue(
                battleId, "e1", T0 + BattleActionTimelineState.WINDUP_NANOS + 10_000_000L)
                .orElseThrow().hpDelta());
        assertEquals(-10, BattleStageFeedbackState.cue(
                battleId, "p1",
                T0 + BattleActionTimelineState.BEAT_NANOS + BattleActionTimelineState.WINDUP_NANOS + 10_000_000L)
                .orElseThrow().hpDelta());
    }

    @Test
    void nonActionOrStaleEventBatchesDoNotReplaceAnActiveTimeline() {
        UUID battleId = UUID.randomUUID();
        BattleActionTimelineState.acceptEvents(events(battleId, 10L, 2,
                new BattleEvent(9L, "COMMAND_ACCEPTED", "p1", "slash")), T0);
        BattleActionTimelineState.acceptEvents(events(battleId, 9L, 1,
                new BattleEvent(8L, "AI_COMMAND", "e1", "stale")), T0 + 1L);
        BattleActionTimelineState.acceptEvents(events(battleId, 11L, 3,
                new BattleEvent(11L, "STATE_CHANGED", "p1", "AWAIT_COMMAND")), T0 + 2L);

        BattleActionTimelineState.Cue cue = BattleActionTimelineState.cue(battleId, T0 + 3L).orElseThrow();
        assertEquals("p1", cue.actorId());
        assertEquals("slash", cue.actionId());
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

    private static BattleNetworkPayloads.SnapshotParticipant participant(String id, int hp, int poise) {
        return new BattleNetworkPayloads.SnapshotParticipant(
                id, hp, 100, poise, 50, 0, false, false, false, true,
                id.startsWith("e") ? "ENEMY" : "PLAYER", id.startsWith("e") ? 1 : 0,
                "turnbound_re:zombie", List.of(), null, null);
    }
}
