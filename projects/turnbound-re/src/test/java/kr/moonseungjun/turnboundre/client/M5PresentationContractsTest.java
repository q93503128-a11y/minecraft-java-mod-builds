package kr.moonseungjun.turnboundre.client;

import kr.moonseungjun.turnboundre.battle.BattleInstance;
import kr.moonseungjun.turnboundre.battle.BattleParticipant;
import kr.moonseungjun.turnboundre.battle.BattleTeam;
import kr.moonseungjun.turnboundre.network.BattleNetworkPayloads;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class M5PresentationContractsTest {
    @AfterEach
    void clearClientState() {
        BattleClientState.clear();
    }

    @Test
    void snapshotCarriesTeamStatusesAndPublishedEnemyIntentWithoutClientInference() {
        UUID battleId = UUID.randomUUID();
        BattleInstance battle = new BattleInstance(battleId, 123L, List.of(
                new BattleParticipant("p1", BattleTeam.PLAYER, 0, 20, 100, 10, 5, 30),
                new BattleParticipant("e1", BattleTeam.ENEMY, 1, 10, 90, 9, 4, 25)));
        battle.start();
        battle.combatState("p1").setGuard(true);

        BattleNetworkPayloads.BattleSnapshotS2C payload = BattleNetworkPayloads.BattleSnapshotS2C.from(battle);
        BattleNetworkPayloads.DecodedSnapshot snapshot = payload.decode();

        assertEquals(List.of("p1", "e1"), snapshot.participants().stream().map(BattleNetworkPayloads.SnapshotParticipant::id).toList());
        BattleNetworkPayloads.SnapshotParticipant player = snapshot.participants().get(0);
        BattleNetworkPayloads.SnapshotParticipant enemy = snapshot.participants().get(1);

        assertEquals("PLAYER", player.team());
        assertTrue(player.guard());
        assertFalse(player.statuses().isEmpty());
        assertNull(player.intent());

        assertEquals("ENEMY", enemy.team());
        assertNotNull(enemy.intent());
        assertEquals("basic", enemy.intent().actionId());
        assertEquals("ATTACK", enemy.intent().type());
        assertEquals("NORMAL", enemy.intent().risk());
    }

    @Test
    void productionClientStateBuildsReadOnlyPartyEnemyAndCurrentActorProjection() {
        UUID battleId = UUID.randomUUID();
        BattleInstance battle = new BattleInstance(battleId, 987L, List.of(
                new BattleParticipant("p1", BattleTeam.PLAYER, 0, 30, 100, 10, 5, 30),
                new BattleParticipant("p2", BattleTeam.PLAYER, 1, 20, 100, 10, 5, 30),
                new BattleParticipant("e1", BattleTeam.ENEMY, 2, 10, 100, 10, 5, 30)));
        battle.start();

        BattleClientState.accept(BattleNetworkPayloads.BattleSnapshotS2C.from(battle));
        BattlePresentationModel model = BattleClientState.presentation().orElseThrow();

        assertEquals(battleId, model.battleId());
        assertEquals(2, model.playerParty().size());
        assertEquals(1, model.enemies().size());
        assertEquals("p1", model.currentActor().orElseThrow().id());
        assertTrue(model.awaitingPlayerCommand());
        assertThrows(UnsupportedOperationException.class, () -> model.turnOrder().add(model.enemies().getFirst()));
    }
}
