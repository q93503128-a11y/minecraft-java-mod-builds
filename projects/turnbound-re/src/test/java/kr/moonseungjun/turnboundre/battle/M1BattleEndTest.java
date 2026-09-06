package kr.moonseungjun.turnboundre.battle;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class M1BattleEndTest {
    private static DamageService.DamageRequest lethal() {
        return new DamageService.DamageRequest(
                DamageTag.MELEE, AffinityGrade.NORMAL, 1000, 0, 100, 100,
                false, 0.0D, 1.5D, 1.0D);
    }

    @Test void defeatedActorIsSkippedWithoutSoftLock() {
        var battle = new BattleInstance(UUID.fromString("00000000-0000-0000-0000-000000000701"), 701L, List.of(
                new BattleParticipant("p", BattleTeam.PLAYER, 0, 30, 100, 100, 100, 20),
                new BattleParticipant("e_fast", BattleTeam.ENEMY, 1, 20, 100, 100, 100, 20),
                new BattleParticipant("e_slow", BattleTeam.ENEMY, 2, 10, 100, 100, 100, 20)
        ));
        battle.start();
        assertEquals(BattleInstance.CommandResult.ACCEPTED,
                battle.submit(new BattleCommand(battle.revision(), "p", "basic")));
        battle.resolveDamage("p", "e_fast", lethal());
        assertFalse(battle.combatState("e_fast").alive());

        battle.finishResolution();

        assertEquals("e_slow", battle.currentActorId());
        assertEquals(BattleState.RESOLVING, battle.state());
        assertTrue(battle.eventLog().stream().anyMatch(e -> e.type().equals("DEFEATED_ACTOR_SKIPPED")));
    }

    @Test void lastEnemyDefeatTransitionsThroughVictoryToRewardThenCleanup() {
        var battle = new BattleInstance(UUID.fromString("00000000-0000-0000-0000-000000000702"), 702L, List.of(
                new BattleParticipant("p", BattleTeam.PLAYER, 0, 20, 100, 100, 100, 20),
                new BattleParticipant("e", BattleTeam.ENEMY, 1, 10, 100, 100, 100, 20)
        ));
        battle.start();
        battle.submit(new BattleCommand(battle.revision(), "p", "guard"));
        assertEquals(15, battle.combatState("p").energy());
        battle.resolveDamage("p", "e", lethal());

        battle.finishResolution();

        assertEquals(BattleInstance.Outcome.VICTORY, battle.outcome());
        assertEquals(BattleState.REWARD, battle.state());
        assertTrue(battle.eventLog().stream().anyMatch(e -> e.type().equals("BATTLE_RESULT") && e.detail().equals("VICTORY")));
        assertTrue(battle.eventLog().stream().anyMatch(e -> e.type().equals("REWARD_READY")));
        assertEquals(BattleInstance.CommandResult.WRONG_PHASE,
                battle.submit(new BattleCommand(battle.revision(), "p", "basic")));

        battle.cleanup();
        assertEquals(BattleState.NOT_IN_BATTLE, battle.state());
        assertEquals(0, battle.combatState("p").energy());
        assertTrue(battle.combatState("p").statuses().activeIds().isEmpty());
        assertTrue(battle.eventLog().stream().anyMatch(e -> e.type().equals("CLEANUP_COMPLETE")));
    }

    @Test void lastPlayerDefeatTransitionsThroughDefeatToRewardThenCleanup() {
        var battle = new BattleInstance(UUID.fromString("00000000-0000-0000-0000-000000000704"), 704L, List.of(
                new BattleParticipant("e", BattleTeam.ENEMY, 0, 30, 100, 100, 100, 20),
                new BattleParticipant("p", BattleTeam.PLAYER, 1, 10, 100, 100, 100, 20)
        ));
        battle.start();
        assertEquals("e", battle.currentActorId());
        assertEquals(BattleState.RESOLVING, battle.state());

        battle.resolveDamage("e", "p", lethal());
        battle.finishResolution();

        assertEquals(BattleInstance.Outcome.DEFEAT, battle.outcome());
        assertEquals(BattleState.REWARD, battle.state());
        assertTrue(battle.eventLog().stream().anyMatch(e -> e.type().equals("BATTLE_RESULT") && e.detail().equals("DEFEAT")));
        assertTrue(battle.eventLog().stream().anyMatch(e -> e.type().equals("REWARD_READY") && e.detail().contains("DEFEAT")));

        battle.cleanup();
        assertEquals(BattleState.NOT_IN_BATTLE, battle.state());
    }

    @Test void terminalFlowIsDeterministicForSameSeedAndInputs() {
        var id = UUID.fromString("00000000-0000-0000-0000-000000000703");
        var participants = List.of(
                new BattleParticipant("p", BattleTeam.PLAYER, 0, 20, 100, 100, 100, 20),
                new BattleParticipant("e", BattleTeam.ENEMY, 1, 10, 100, 100, 100, 20)
        );
        var left = new BattleInstance(id, 703L, participants);
        var right = new BattleInstance(id, 703L, participants);
        runVictory(left);
        runVictory(right);
        assertEquals(left.outcome(), right.outcome());
        assertEquals(left.state(), right.state());
        assertEquals(left.revision(), right.revision());
        assertEquals(left.rngDraws(), right.rngDraws());
        assertEquals(left.eventLog(), right.eventLog());
    }

    private static void runVictory(BattleInstance battle) {
        battle.start();
        battle.submit(new BattleCommand(battle.revision(), "p", "basic"));
        battle.resolveDamage("p", "e", lethal());
        battle.finishResolution();
        battle.cleanup();
    }
}
