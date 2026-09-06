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
        List<BattleParticipant> participants = List.of(player(), enemy());
        BattleInstance battle = new BattleInstance(battleId, 1234L, participants);
        BattleManager manager = new BattleManager();
        manager.register(battle, List.of(
                new EntityParticipantBinding("p1", playerEntity),
                new EntityParticipantBinding("e1", UUID.randomUUID())), participants);
        battle.start();

        BattleNetworkGateway gateway = new BattleNetworkGateway(manager);
        long revision = battle.revision();
        int events = battle.eventLog().size();

        var foreign = new BattleNetworkPayloads.DecodedCommand(battleId, revision, "p1", "basic", "cmd-a", List.of("e1"));
        assertEquals(BattleNetworkGateway.ResultCode.SENDER_NOT_BOUND, gateway.submit(UUID.randomUUID(), foreign).code());
        assertEquals(revision, battle.revision());
        assertEquals(events, battle.eventLog().size());

        var stale = new BattleNetworkPayloads.DecodedCommand(battleId, revision + 1, "p1", "basic", "cmd-b", List.of("e1"));
        assertEquals(BattleNetworkGateway.ResultCode.STALE_REVISION, gateway.submit(playerEntity, stale).code());
        assertEquals(revision, battle.revision());
        assertEquals(events, battle.eventLog().size());
    }

    @Test
    void boundBasicCommandMutatesThroughPersistentStrictGateAndReplayIsRejected() {
        UUID battleId = UUID.randomUUID();
        UUID playerEntity = UUID.randomUUID();
        List<BattleParticipant> participants = List.of(player(), enemy());
        BattleInstance battle = new BattleInstance(battleId, 9L, participants);
        BattleManager manager = new BattleManager();
        manager.register(battle, List.of(
                new EntityParticipantBinding("p1", playerEntity),
                new EntityParticipantBinding("e1", UUID.randomUUID())), participants);
        battle.start();

        long beforeRevision = battle.revision();
        int beforeEnergy = battle.combatState("p1").energy();
        BattleNetworkGateway gateway = new BattleNetworkGateway(manager);
        var incoming = new BattleNetworkPayloads.DecodedCommand(battleId, beforeRevision, "p1", "basic", "cmd-ok", List.of("e1"));
        BattleNetworkGateway.Result accepted = gateway.submit(playerEntity, incoming);

        assertTrue(accepted.accepted());
        assertEquals(beforeRevision + 1, battle.revision());
        assertEquals(Math.min(100, beforeEnergy + 10), battle.combatState("p1").energy());
        assertFalse(BattleNetworkGateway.eventsSince(accepted).isEmpty());
        assertFalse(BattleNetworkPayloads.BattleSnapshotS2C.from(battle).wire().isBlank());

        int eventsAfterAccept = battle.eventLog().size();
        int energyAfterAccept = battle.combatState("p1").energy();
        var replay = new BattleNetworkPayloads.DecodedCommand(battleId, battle.revision(), "p1", "basic", "cmd-ok", List.of("e1"));
        BattleNetworkGateway.Result rejected = gateway.submit(playerEntity, replay);
        assertEquals(BattleNetworkGateway.ResultCode.COMMAND_REJECTED, rejected.code());
        assertEquals("DUPLICATE_COMMAND", rejected.detail());
        assertEquals(eventsAfterAccept, battle.eventLog().size());
        assertEquals(energyAfterAccept, battle.combatState("p1").energy());
    }

    @Test
    void dataDefinedActionIsNotInventedByNetworkAdapter() {
        UUID battleId = UUID.randomUUID();
        UUID playerEntity = UUID.randomUUID();
        List<BattleParticipant> participants = List.of(player(), enemy());
        BattleInstance battle = new BattleInstance(battleId, 11L, participants);
        BattleManager manager = new BattleManager();
        manager.register(battle, List.of(
                new EntityParticipantBinding("p1", playerEntity),
                new EntityParticipantBinding("e1", UUID.randomUUID())), participants);
        battle.start();
        long revision = battle.revision();
        int events = battle.eventLog().size();

        var skill = new BattleNetworkPayloads.DecodedCommand(battleId, revision, "p1", "skill_unknown", "cmd-skill", List.of("e1"));
        BattleNetworkGateway.Result result = new BattleNetworkGateway(manager).submit(playerEntity, skill);
        assertEquals(BattleNetworkGateway.ResultCode.DATA_ACTION_NOT_RESOLVED, result.code());
        assertEquals(revision, battle.revision());
        assertEquals(events, battle.eventLog().size());
    }
}
