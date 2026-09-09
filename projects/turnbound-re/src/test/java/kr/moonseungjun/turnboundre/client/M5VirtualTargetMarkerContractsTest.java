package kr.moonseungjun.turnboundre.client;

import kr.moonseungjun.turnboundre.network.BattleNetworkPayloads;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class M5VirtualTargetMarkerContractsTest {
    @AfterEach
    void clearMarkerState() {
        BattleTargetMarkerState.clear();
    }

    @Test
    void unboundVirtualParticipantsKeepAuthoritativeTargetOrdinalsByParticipantId() {
        UUID battleId = UUID.randomUUID();
        BattleNetworkPayloads.SnapshotParticipant first = participant("e1", 0);
        BattleNetworkPayloads.SnapshotParticipant second = participant("e2", 1);

        BattleTargetMarkerState.publish(
                battleId,
                41L,
                List.of(first, second),
                Set.of("e2"),
                "e1");

        assertEquals(BattleTargetMarkerState.MarkerKind.HOVERED,
                BattleTargetMarkerState.markerForParticipant(battleId, 41L, "e1").orElseThrow());
        assertEquals(1,
                BattleTargetMarkerState.markerOrdinalForParticipant(battleId, 41L, "e1").orElseThrow());
        assertEquals(BattleTargetMarkerState.MarkerKind.SELECTED,
                BattleTargetMarkerState.markerForParticipant(battleId, 41L, "e2").orElseThrow());
        assertEquals(2,
                BattleTargetMarkerState.markerOrdinalForParticipant(battleId, 41L, "e2").orElseThrow());

        assertTrue(BattleTargetMarkerState.markerForParticipant(battleId, 42L, "e1").isEmpty());
        assertTrue(BattleTargetMarkerState.markerForParticipant(UUID.randomUUID(), 41L, "e1").isEmpty());
        assertTrue(BattleTargetMarkerState.markerFor(battleId, 41L, UUID.randomUUID()).isEmpty(),
                "virtual participant markers must not invent world entity bindings");
    }

    private static BattleNetworkPayloads.SnapshotParticipant participant(String id, int ordinal) {
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
                null);
    }
}
