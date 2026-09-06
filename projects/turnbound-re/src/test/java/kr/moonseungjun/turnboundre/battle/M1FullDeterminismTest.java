package kr.moonseungjun.turnboundre.battle;

import kr.moonseungjun.turnboundre.data.ActionDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class M1FullDeterminismTest {
    private static final ActionDefinition BASIC = new ActionDefinition("basic", "BASIC", 0, 0, 0);
    private static final List<BattleParticipant> PARTICIPANTS = List.of(
            new BattleParticipant("p", BattleTeam.PLAYER, 0, 20, 100, 20, 10, 30),
            new BattleParticipant("e", BattleTeam.ENEMY, 1, 10, 100, 20, 10, 30)
    );

    @Test
    void hundredCyclesRemainLiveAndProduceIdenticalEventStreams() {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000888");
        BattleInstance left = new BattleInstance(id, 888L, PARTICIPANTS);
        BattleInstance right = new BattleInstance(id, 888L, PARTICIPANTS);

        run(left, 100);
        run(right, 100);

        assertEquals(BattleInstance.Outcome.ONGOING, left.outcome());
        assertEquals(BattleState.AWAIT_COMMAND, left.state());
        assertEquals("p", left.currentActorId());
        assertEquals(left.cycle(), right.cycle());
        assertEquals(left.revision(), right.revision());
        assertEquals(left.rngDraws(), right.rngDraws());
        assertEquals(left.eventLog(), right.eventLog());
    }

    private static void run(BattleInstance battle, int cycles) {
        battle.start();
        BattleCommandService commands = new BattleCommandService(battle, PARTICIPANTS);
        for (int i = 0; i < cycles; i++) {
            assertEquals(BattleState.AWAIT_COMMAND, battle.state());
            assertEquals("p", battle.currentActorId());
            BattleCommand command = new BattleCommand(
                    battle.revision(), "p", "basic", "cycle-" + i, List.of("e"));
            assertEquals(BattleCommandService.Result.ACCEPTED,
                    commands.submit(command, BASIC, ActionUsePolicy.singleEnemy()));
            battle.finishResolution();

            assertEquals(BattleState.RESOLVING, battle.state());
            assertEquals("e", battle.currentActorId());
            battle.resolveEnemyStub();
            battle.finishResolution();
        }
    }
}
