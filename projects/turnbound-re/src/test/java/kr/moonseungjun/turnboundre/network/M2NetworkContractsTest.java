package kr.moonseungjun.turnboundre.network;

import kr.moonseungjun.turnboundre.battle.BattleCommand;
import kr.moonseungjun.turnboundre.battle.BattleInstance;
import kr.moonseungjun.turnboundre.battle.BattleManager;
import kr.moonseungjun.turnboundre.battle.BattleParticipant;
import kr.moonseungjun.turnboundre.battle.BattleTeam;
import kr.moonseungjun.turnboundre.battle.EntityParticipantBinding;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class M2NetworkContractsTest {
    private static BattleParticipant player() { return new BattleParticipant("p1", BattleTeam.PLAYER, 0, 20, 100, 10, 5, 30); }
    private static BattleParticipant enemy() { return new BattleParticipant("e1", BattleTeam.ENEMY, 1, 10, 100, 10, 5, 30); }

    @Test
    void commandWireRoundTripsStableIdentityAndTargets() {
        UUID battleId = UUID.randomUUID();
        BattleCommand command = new BattleCommand(7, "p1", "basic", "cmd-7", List.of("e1"));
        BattleNetworkPayloads.DecodedCommand decoded = BattleNetworkPayloads.BattleCommandC2S.of(battleId, command).decode();
        assertEquals(battleId, decoded.battleId());
        assertEquals(command, decoded.toCore());
    }

    @Test
    void authorityGateRejectsForeignSenderAndStaleRevisionWithoutMutation() {
        UUID battleId = UUID.randomUUID();
        UUID playerEntity = UUID.randomUUID();
        UUID enemyEntity = UUID.randomUUID();
        BattleInstance battle = new BattleInstance(battleId, 1234L, List.of(player(), enemy()));
        BattleManager manager = new BattleManager();
        manager.register(battle, List.of(
                new EntityParticipantBinding(playerEntity, "p1"),
                new EntityParticipantBinding(enemyEntity, "e1")));
        battle.start();

        BattleNetworkGateway gateway = new BattleNetworkGateway(manager);
        long revision = battle.revision();
        int events = battle.eventLog().size();

        var foreign = new BattleNetworkPayloads.DecodedCommand(battleId, revision, "p1", "basic", "cmd-a", List.of("e1"));
        assertEquals(BattleNetworkGateway.ResultCode.SENDER_NOT_BOUND, gateway.authorize(UUID.randomUUID(), foreign).code());
        assertEquals(revision, battle.revision());
        assertEquals(events, battle.eventLog().size());

        var stale = new BattleNetworkPayloads.DecodedCommand(battleId, revision + 1, "p1", "basic", "cmd-b", List.of("e1"));
        assertEquals(BattleNetworkGateway.ResultCode.STALE_REVISION, gateway.authorize(playerEntity, stale).code());
        assertEquals(revision, battle.revision());
        assertEquals(events, battle.eventLog().size());
    }

    @Test
    void authorityGateAcceptsOnlyBoundCurrentActorAndSnapshotIsNonEmpty() {
        UUID battleId = UUID.randomUUID();
        UUID playerEntity = UUID.randomUUID();
        BattleInstance battle = new BattleInstance(battleId, 9L, List.of(player(), enemy()));
        BattleManager manager = new BattleManager();
        manager.register(battle, List.of(
                new EntityParticipantBinding(playerEntity, "p1"),
                new EntityParticipantBinding(UUID.randomUUID(), "e1")));
        battle.start();

        var incoming = new BattleNetworkPayloads.DecodedCommand(battleId, battle.revision(), "p1", "basic", "cmd-ok", List.of("e1"));
        BattleNetworkGateway.Result result = new BattleNetworkGateway(manager).authorize(playerEntity, incoming);
        assertTrue(result.authorized());
        assertFalse(BattleNetworkPayloads.BattleSnapshotS2C.from(battle).wire().isBlank());
        assertFalse(BattleNetworkPayloads.BattleEventsS2C.rejection(battleId, battle.revision(), "test").wire().isBlank());
    }
}
