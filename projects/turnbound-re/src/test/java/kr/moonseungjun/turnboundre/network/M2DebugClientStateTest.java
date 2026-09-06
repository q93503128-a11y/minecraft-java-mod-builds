package kr.moonseungjun.turnboundre.network;

import kr.moonseungjun.turnboundre.battle.BattleEvent;
import kr.moonseungjun.turnboundre.battle.BattleInstance;
import kr.moonseungjun.turnboundre.battle.BattleParticipant;
import kr.moonseungjun.turnboundre.battle.BattleTeam;
import kr.moonseungjun.turnboundre.debug.DebugBattleClientState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class M2DebugClientStateTest {
    @AfterEach
    void clearDebugState() {
        DebugBattleClientState.clear();
    }

    @Test
    void snapshotAndEventPayloadsDecodeIntoDebugOnlyInspectionState() {
        UUID battleId = UUID.randomUUID();
        BattleInstance battle = new BattleInstance(battleId, 77L, List.of(
                new BattleParticipant("p1", BattleTeam.PLAYER, 0, 20, 100, 10, 5, 30),
                new BattleParticipant("e1", BattleTeam.ENEMY, 1, 10, 90, 9, 4, 25)));
        battle.start();

        BattleNetworkPayloads.BattleSnapshotS2C snapshotPayload = BattleNetworkPayloads.BattleSnapshotS2C.from(battle);
        BattleNetworkPayloads.DecodedSnapshot snapshot = snapshotPayload.decode();
        assertEquals(battleId, snapshot.battleId());
        assertEquals(battle.revision(), snapshot.revision());
        assertEquals(battle.state().name(), snapshot.state());
        assertEquals(2, snapshot.participants().size());
        assertEquals("p1", snapshot.currentActorId());

        var eventsPayload = BattleNetworkPayloads.BattleEventsS2C.from(
                battleId, battle.revision(), 0, battle.eventLog());
        var decodedEvents = eventsPayload.decode();
        assertEquals(battleId, decodedEvents.battleId());
        assertFalse(decodedEvents.events().isEmpty());

        DebugBattleClientState.accept(snapshotPayload);
        DebugBattleClientState.accept(eventsPayload);
        assertEquals(battleId, DebugBattleClientState.latestSnapshot().orElseThrow().battleId());
        assertEquals(decodedEvents.events().size(), DebugBattleClientState.latestEvents().orElseThrow().events().size());
        assertTrue(DebugBattleClientState.summary().startsWith("DEBUG_ONLY battle="));
    }

    @Test
    void olderSnapshotCannotOverwriteNewerInspectionStateForSameBattle() {
        UUID battleId = UUID.randomUUID();
        String newerWire = BattleNetworkPayloads.join(
                battleId.toString(), "9", "AWAIT_COMMAND", "2", "p1", "");
        String olderWire = BattleNetworkPayloads.join(
                battleId.toString(), "4", "AWAIT_COMMAND", "1", "p1", "");

        DebugBattleClientState.accept(new BattleNetworkPayloads.BattleSnapshotS2C(newerWire));
        DebugBattleClientState.accept(new BattleNetworkPayloads.BattleSnapshotS2C(olderWire));

        assertEquals(9L, DebugBattleClientState.latestSnapshot().orElseThrow().revision());
        assertEquals(2, DebugBattleClientState.latestSnapshot().orElseThrow().cycle());
    }

    @Test
    void explicitDebugCleanupEventClearsStaleClientSnapshot() {
        UUID battleId = UUID.randomUUID();
        String snapshotWire = BattleNetworkPayloads.join(
                battleId.toString(), "5", "AWAIT_COMMAND", "1", "p1", "");
        DebugBattleClientState.accept(new BattleNetworkPayloads.BattleSnapshotS2C(snapshotWire));
        assertTrue(DebugBattleClientState.latestSnapshot().isPresent());

        var clearPayload = BattleNetworkPayloads.BattleEventsS2C.from(
                battleId,
                5L,
                -1,
                List.of(new BattleEvent(5L, DebugBattleClientState.CLEAR_EVENT_TYPE, "", "test")));
        DebugBattleClientState.accept(clearPayload);

        assertTrue(DebugBattleClientState.latestSnapshot().isEmpty());
        assertTrue(DebugBattleClientState.latestEvents().isEmpty());
    }
}
