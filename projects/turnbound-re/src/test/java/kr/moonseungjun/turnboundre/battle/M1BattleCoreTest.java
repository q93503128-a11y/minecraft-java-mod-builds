package kr.moonseungjun.turnboundre.battle;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class M1BattleCoreTest {
    private static final UUID BATTLE_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");

    private static List<BattleParticipant> participants() {
        return List.of(
                new BattleParticipant("player_fast", BattleTeam.PLAYER, 0, 12, 100, 100, 100, 20),
                new BattleParticipant("enemy_tie_late", BattleTeam.ENEMY, 2, 10, 100, 100, 100, 20),
                new BattleParticipant("enemy_tie_early", BattleTeam.ENEMY, 1, 10, 100, 100, 100, 20)
        );
    }

    @Test void initiativeUsesSpeedThenStableOrdinal() {
        assertEquals(List.of("player_fast", "enemy_tie_early", "enemy_tie_late"),
                InitiativeService.order(participants()));
    }

    @Test void staleOrWrongActorCommandDoesNotMutateRevisionOrEvents() {
        var battle = new BattleInstance(BATTLE_ID, 77L, participants());
        battle.start();
        long revision = battle.revision();
        int events = battle.eventLog().size();

        assertEquals(BattleInstance.CommandResult.STALE_REVISION,
                battle.submit(new BattleCommand(revision + 1, "player_fast", "basic")));
        assertEquals(BattleInstance.CommandResult.WRONG_ACTOR,
                battle.submit(new BattleCommand(revision, "enemy_tie_early", "basic")));
        assertEquals(revision, battle.revision());
        assertEquals(events, battle.eventLog().size());
        assertEquals(BattleState.AWAIT_COMMAND, battle.state());
    }

    @Test void sameInputsProduceSameStateAndEventStreamAcrossCycles() {
        var left = new BattleInstance(BATTLE_ID, 99L, participants());
        var right = new BattleInstance(BATTLE_ID, 99L, participants());
        runTwoCycles(left);
        runTwoCycles(right);

        assertEquals(left.actorOrder(), right.actorOrder());
        assertEquals(left.cycle(), right.cycle());
        assertEquals(left.revision(), right.revision());
        assertEquals(left.state(), right.state());
        assertEquals(left.currentActorId(), right.currentActorId());
        assertEquals(left.eventLog(), right.eventLog());
        assertEquals(3, left.cycle());
        assertEquals(BattleState.AWAIT_COMMAND, left.state());
        assertEquals("player_fast", left.currentActorId());
    }

    @Test void participantValidationRejectsAmbiguousBattleMembership() {
        var player = new BattleParticipant("p", BattleTeam.PLAYER, 0, 10, 10, 10, 10, 10);
        assertThrows(IllegalArgumentException.class, () -> new BattleInstance(BATTLE_ID, 1L, List.of(player)));
        assertThrows(IllegalArgumentException.class, () -> new BattleInstance(BATTLE_ID, 1L, List.of(
                player,
                new BattleParticipant("e", BattleTeam.ENEMY, 0, 10, 10, 10, 10, 10))));
    }

    private static void runTwoCycles(BattleInstance battle) {
        battle.start();
        for (int cycle = 0; cycle < 2; cycle++) {
            assertEquals(BattleInstance.CommandResult.ACCEPTED,
                    battle.submit(new BattleCommand(battle.revision(), "player_fast", "basic")));
            battle.finishResolution();
            assertEquals("enemy_tie_early", battle.currentActorId());
            battle.resolveEnemyStub();
            battle.finishResolution();
            assertEquals("enemy_tie_late", battle.currentActorId());
            battle.resolveEnemyStub();
            battle.finishResolution();
        }
    }
}
