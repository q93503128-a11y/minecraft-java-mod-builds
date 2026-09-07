package kr.moonseungjun.turnboundre.client;

import kr.moonseungjun.turnboundre.battle.BattleCommand;
import kr.moonseungjun.turnboundre.battle.BattleDefinitionContext;
import kr.moonseungjun.turnboundre.battle.BattleInstance;
import kr.moonseungjun.turnboundre.battle.BattleParticipant;
import kr.moonseungjun.turnboundre.battle.BattleTeam;
import kr.moonseungjun.turnboundre.fixtures.ProductionDefinitionFixture;
import kr.moonseungjun.turnboundre.network.BattleNetworkPayloads;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class M5PresentationContractsTest {
    @AfterEach
    void clearClientState() {
        BattleClientState.clear();
    }

    @Test
    void snapshotCarriesTeamOrdinalStatusesAndPublishedEnemyIntentWithoutClientInference() {
        UUID battleId = UUID.randomUUID();
        BattleInstance battle = new BattleInstance(battleId, 123L, List.of(
                new BattleParticipant("p1", BattleTeam.PLAYER, 0, 20, 100, 10, 5, 30),
                new BattleParticipant("e1", BattleTeam.ENEMY, 1, 10, 90, 9, 4, 25)));
        battle.start();
        battle.combatState("p1").setGuard(true);

        BattleNetworkPayloads.DecodedSnapshot snapshot = BattleNetworkPayloads.BattleSnapshotS2C.from(battle).decode();

        assertEquals(List.of("p1", "e1"), snapshot.participants().stream().map(BattleNetworkPayloads.SnapshotParticipant::id).toList());
        BattleNetworkPayloads.SnapshotParticipant player = snapshot.participants().get(0);
        BattleNetworkPayloads.SnapshotParticipant enemy = snapshot.participants().get(1);

        assertEquals("PLAYER", player.team());
        assertEquals(0, player.participantOrdinal());
        assertTrue(player.guard());
        assertFalse(player.statuses().isEmpty());
        assertNull(player.intent());

        assertEquals("ENEMY", enemy.team());
        assertEquals(1, enemy.participantOrdinal());
        assertNotNull(enemy.intent());
        assertEquals("basic", enemy.intent().actionId());
        assertEquals("ATTACK", enemy.intent().type());
        assertEquals("NORMAL", enemy.intent().risk());
        assertTrue(snapshot.availableActions().isEmpty(), "core snapshot must not invent data-defined player actions");
    }

    @Test
    void presentationRotatesTurnRailButKeepsPartySlotsStable() {
        UUID battleId = UUID.randomUUID();
        BattleInstance battle = new BattleInstance(battleId, 987L, List.of(
                new BattleParticipant("p1", BattleTeam.PLAYER, 0, 30, 100, 10, 5, 30),
                new BattleParticipant("p2", BattleTeam.PLAYER, 1, 20, 100, 10, 5, 30),
                new BattleParticipant("e1", BattleTeam.ENEMY, 2, 10, 100, 10, 5, 30)));
        battle.start();

        assertEquals(BattleInstance.CommandResult.ACCEPTED, battle.submit(new BattleCommand(
                battle.revision(), "p1", "basic", "advance-turn", List.of("e1"))));
        battle.finishResolution();
        assertEquals("p2", battle.currentActorId());

        BattleClientState.accept(BattleNetworkPayloads.BattleSnapshotS2C.from(battle));
        BattlePresentationModel model = BattleClientState.presentation().orElseThrow();

        assertEquals(battleId, model.battleId());
        assertEquals(List.of("p2", "e1", "p1"), model.turnOrder().stream().map(BattleNetworkPayloads.SnapshotParticipant::id).toList());
        assertEquals(List.of("p1", "p2"), model.playerParty().stream().map(BattleNetworkPayloads.SnapshotParticipant::id).toList());
        assertEquals(1, model.enemies().size());
        assertEquals("p2", model.currentActor().orElseThrow().id());
        assertTrue(model.awaitingPlayerCommand());
        assertThrows(UnsupportedOperationException.class, () -> model.turnOrder().add(model.enemies().getFirst()));
    }

    @Test
    void productionSnapshotPublishesActionSlotsAndAuthoritativeEnergyAndTargetAvailability() throws Exception {
        var parsed = ProductionDefinitionFixture.load();
        UUID battleId = UUID.randomUUID();
        BattleInstance battle = new BattleInstance(battleId, 4321L, List.of(
                new BattleParticipant("p1", BattleTeam.PLAYER, 0, 30, 100, 10, 5, 30),
                new BattleParticipant("e1", BattleTeam.ENEMY, 1, 10, 100, 10, 5, 30)));
        BattleDefinitionContext context = new BattleDefinitionContext(
                parsed.registry(), parsed.hash(),
                Map.of("p1", "turnbound_re:skeleton", "e1", "turnbound_re:zombie"));
        battle.start();

        BattleNetworkPayloads.DecodedSnapshot emptyEnergy = BattleNetworkPayloads.BattleSnapshotS2C.from(battle, context).decode();
        assertEquals(List.of("BASIC", "SKILL_1", "SKILL_2", "GUARD", "BURST"),
                emptyEnergy.availableActions().stream().map(BattleNetworkPayloads.SnapshotAction::slot).toList());
        assertTrue(emptyEnergy.availableActions().get(0).usable());
        assertFalse(emptyEnergy.availableActions().get(1).usable());
        assertEquals("ENERGY", emptyEnergy.availableActions().get(1).disabledReason());
        assertTrue(emptyEnergy.availableActions().get(3).usable());
        assertFalse(emptyEnergy.availableActions().get(4).usable());

        battle.combatState("p1").gainEnergy(100);
        BattleNetworkPayloads.DecodedSnapshot fullEnergy = BattleNetworkPayloads.BattleSnapshotS2C.from(battle, context).decode();
        assertTrue(fullEnergy.availableActions().subList(0, 4).stream().allMatch(BattleNetworkPayloads.SnapshotAction::usable));
        BattleNetworkPayloads.SnapshotAction multiTargetBurst = fullEnergy.availableActions().get(4);
        assertFalse(multiTargetBurst.usable());
        assertEquals("TARGETS", multiTargetBurst.disabledReason());
        assertEquals(3, multiTargetBurst.targetCount());
        assertEquals("turnbound_re:skeleton", fullEnergy.participants().getFirst().characterId());
    }
}
