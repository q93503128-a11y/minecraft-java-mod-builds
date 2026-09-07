package kr.moonseungjun.turnboundre.client;

import kr.moonseungjun.turnboundre.battle.BattleInstance;
import kr.moonseungjun.turnboundre.battle.BattleManager;
import kr.moonseungjun.turnboundre.battle.BattleParticipant;
import kr.moonseungjun.turnboundre.battle.BattleTeam;
import kr.moonseungjun.turnboundre.battle.EntityParticipantBinding;
import kr.moonseungjun.turnboundre.network.BattleNetworkPayloads;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class M5WorldTargetMarkerContractsTest {
    @AfterEach
    void clearMarkerState() {
        BattleTargetMarkerState.clear();
    }

    @Test
    void serverSnapshotCarriesRegisteredEntityBindingsWithoutCorePathInventingThem() {
        UUID battleId = UUID.randomUUID();
        UUID playerEntityId = UUID.randomUUID();
        UUID enemyEntityId = UUID.randomUUID();
        BattleInstance battle = new BattleInstance(battleId, 77L, List.of(
                new BattleParticipant("p1", BattleTeam.PLAYER, 0, 20, 100, 10, 5, 30),
                new BattleParticipant("e1", BattleTeam.ENEMY, 1, 10, 90, 9, 4, 25)));
        BattleManager battles = new BattleManager();
        battles.register(battle, List.of(
                new EntityParticipantBinding("p1", playerEntityId),
                new EntityParticipantBinding("e1", enemyEntityId)));
        battle.start();

        BattleNetworkPayloads.DecodedSnapshot serverSnapshot =
                BattleNetworkPayloads.BattleSnapshotS2C.from(battle, null, battles).decode();
        assertEquals(playerEntityId, serverSnapshot.participants().get(0).entityId());
        assertEquals(enemyEntityId, serverSnapshot.participants().get(1).entityId());

        BattleNetworkPayloads.DecodedSnapshot coreSnapshot =
                BattleNetworkPayloads.BattleSnapshotS2C.from(battle).decode();
        assertNull(coreSnapshot.participants().get(0).entityId());
        assertNull(coreSnapshot.participants().get(1).entityId());
    }

    @Test
    void markerStateUsesOnlyPublishedParticipantsAndRejectsStaleBattleOrRevision() {
        UUID battleId = UUID.randomUUID();
        UUID hoveredEntityId = UUID.randomUUID();
        UUID selectedEntityId = UUID.randomUUID();
        UUID unboundEntityId = UUID.randomUUID();

        BattleNetworkPayloads.SnapshotParticipant hovered = participant("e1", 0, hoveredEntityId);
        BattleNetworkPayloads.SnapshotParticipant selected = participant("e2", 1, selectedEntityId);
        BattleNetworkPayloads.SnapshotParticipant unbound = participant("e3", 2, null);

        BattleTargetMarkerState.publish(
                battleId,
                12L,
                List.of(hovered, selected, unbound),
                Set.of("e2"),
                "e1");

        assertTrue(BattleTargetMarkerState.isPublishedFor(battleId, 12L));
        assertFalse(BattleTargetMarkerState.isPublishedFor(battleId, 13L));
        assertEquals(BattleTargetMarkerState.MarkerKind.HOVERED,
                BattleTargetMarkerState.markerFor(battleId, 12L, hoveredEntityId).orElseThrow());
        assertEquals(1,
                BattleTargetMarkerState.markerOrdinalFor(battleId, 12L, hoveredEntityId).orElseThrow());
        assertEquals(BattleTargetMarkerState.MarkerKind.SELECTED,
                BattleTargetMarkerState.markerFor(battleId, 12L, selectedEntityId).orElseThrow());
        assertEquals(2,
                BattleTargetMarkerState.markerOrdinalFor(battleId, 12L, selectedEntityId).orElseThrow());
        assertTrue(BattleTargetMarkerState.markerFor(battleId, 12L, unboundEntityId).isEmpty());
        assertTrue(BattleTargetMarkerState.markerOrdinalFor(battleId, 12L, unboundEntityId).isEmpty());
        assertTrue(BattleTargetMarkerState.markerFor(battleId, 13L, hoveredEntityId).isEmpty());
        assertTrue(BattleTargetMarkerState.markerOrdinalFor(battleId, 13L, hoveredEntityId).isEmpty());
        assertTrue(BattleTargetMarkerState.markerFor(UUID.randomUUID(), 12L, hoveredEntityId).isEmpty());

        BattleTargetMarkerState.publish(
                battleId,
                12L,
                List.of(selected),
                Set.of("e2"),
                "e2");
        assertEquals(BattleTargetMarkerState.MarkerKind.SELECTED,
                BattleTargetMarkerState.markerFor(battleId, 12L, selectedEntityId).orElseThrow(),
                "selected must outrank hover so multi-target confirmation remains unambiguous");
        assertEquals(1, BattleTargetMarkerState.markerOrdinalFor(battleId, 12L, selectedEntityId).orElseThrow());

        BattleTargetMarkerState.clear();
        assertFalse(BattleTargetMarkerState.isPublishedFor(battleId, 12L));
        assertTrue(BattleTargetMarkerState.markerFor(battleId, 12L, selectedEntityId).isEmpty());
        assertTrue(BattleTargetMarkerState.markerOrdinalFor(battleId, 12L, selectedEntityId).isEmpty());
    }

    private static BattleNetworkPayloads.SnapshotParticipant participant(
            String id,
            int ordinal,
            UUID entityId
    ) {
        return new BattleNetworkPayloads.SnapshotParticipant(
                id,
                50,
                50,
                20,
                20,
                0,
                false,
                false,
                false,
                true,
                "ENEMY",
                ordinal,
                "turnbound_re:zombie",
                List.of(),
                null,
                entityId);
    }
}
