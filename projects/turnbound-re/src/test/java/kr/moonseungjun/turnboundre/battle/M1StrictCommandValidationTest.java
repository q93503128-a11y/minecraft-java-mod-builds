package kr.moonseungjun.turnboundre.battle;

import kr.moonseungjun.turnboundre.data.ActionDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class M1StrictCommandValidationTest {
    private static final BattleParticipant PLAYER = new BattleParticipant("p1", BattleTeam.PLAYER, 0, 20, 100, 25, 10, 30);
    private static final BattleParticipant ENEMY = new BattleParticipant("e1", BattleTeam.ENEMY, 1, 10, 100, 20, 10, 30);
    private static final ActionDefinition BASIC = new ActionDefinition("basic", "BASIC", 0, 0, 0);

    @Test
    void strictGateAcceptsOwnedReadyEligibleSingleEnemyTargetAndRejectsRetransmit() {
        Fixture f = fixture();
        BattleCommand command = new BattleCommand(f.battle.revision(), "p1", "basic", "cmd-1", List.of("e1"));

        assertEquals(BattleCommandService.Result.ACCEPTED,
                f.service.submit(command, BASIC, ActionUsePolicy.singleEnemy()));
        assertEquals(BattleCommandService.Result.DUPLICATE_COMMAND,
                f.service.submit(command, BASIC, ActionUsePolicy.singleEnemy()));
    }

    @Test
    void everyStrictValidationFailureIsMutationFree() {
        assertMutationFree(new ActionUsePolicy(false, true, true, 1, 1, ActionUsePolicy.TargetRule.ENEMY), List.of("e1"), BattleCommandService.Result.ACTION_NOT_OWNED);
        assertMutationFree(new ActionUsePolicy(true, false, true, 1, 1, ActionUsePolicy.TargetRule.ENEMY), List.of("e1"), BattleCommandService.Result.ACTION_ON_COOLDOWN);
        assertMutationFree(new ActionUsePolicy(true, true, false, 1, 1, ActionUsePolicy.TargetRule.ENEMY), List.of("e1"), BattleCommandService.Result.STATUS_BLOCKED);
        assertMutationFree(ActionUsePolicy.singleEnemy(), List.of(), BattleCommandService.Result.INVALID_TARGET_COUNT);
        assertMutationFree(ActionUsePolicy.singleEnemy(), List.of("p1"), BattleCommandService.Result.INVALID_TARGET);
        assertMutationFree(ActionUsePolicy.singleEnemy(), List.of("missing"), BattleCommandService.Result.INVALID_TARGET);
    }

    @Test
    void defeatedTargetIsRejectedWithoutCommandMutation() {
        Fixture f = fixture();
        f.battle.combatState("e1").applyHpDamage(999);
        long revision = f.battle.revision();
        int events = f.battle.eventLog().size();
        BattleState state = f.battle.state();
        BattleCommand command = new BattleCommand(revision, "p1", "basic", "cmd-dead-target", List.of("e1"));

        assertEquals(BattleCommandService.Result.INVALID_TARGET,
                f.service.submit(command, BASIC, ActionUsePolicy.singleEnemy()));
        assertEquals(revision, f.battle.revision());
        assertEquals(events, f.battle.eventLog().size());
        assertEquals(state, f.battle.state());
    }

    @Test
    void duplicateTargetsCannotSatisfyMultiTargetPolicy() {
        Fixture f = fixture();
        long revision = f.battle.revision();
        int events = f.battle.eventLog().size();
        BattleCommand command = new BattleCommand(revision, "p1", "basic", "cmd-dup-target", List.of("e1", "e1"));
        ActionUsePolicy policy = new ActionUsePolicy(true, true, true, 2, 2, ActionUsePolicy.TargetRule.ENEMY);

        assertEquals(BattleCommandService.Result.INVALID_TARGET, f.service.submit(command, BASIC, policy));
        assertEquals(revision, f.battle.revision());
        assertEquals(events, f.battle.eventLog().size());
    }

    private static void assertMutationFree(ActionUsePolicy policy, List<String> targets, BattleCommandService.Result expected) {
        Fixture f = fixture();
        long revision = f.battle.revision();
        int events = f.battle.eventLog().size();
        int energy = f.battle.combatState("p1").energy();
        BattleState state = f.battle.state();
        BattleCommand command = new BattleCommand(revision, "p1", "basic", "cmd-invalid", targets);

        assertEquals(expected, f.service.submit(command, BASIC, policy));
        assertEquals(revision, f.battle.revision());
        assertEquals(events, f.battle.eventLog().size());
        assertEquals(energy, f.battle.combatState("p1").energy());
        assertEquals(state, f.battle.state());
    }

    private static Fixture fixture() {
        List<BattleParticipant> participants = List.of(PLAYER, ENEMY);
        BattleInstance battle = new BattleInstance(UUID.fromString("00000000-0000-0000-0000-000000000123"), 12345L, participants);
        battle.start();
        return new Fixture(battle, new BattleCommandService(battle, participants));
    }

    private record Fixture(BattleInstance battle, BattleCommandService service) {}
}
