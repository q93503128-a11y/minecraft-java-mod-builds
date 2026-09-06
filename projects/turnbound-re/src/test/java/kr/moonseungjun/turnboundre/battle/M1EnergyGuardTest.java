package kr.moonseungjun.turnboundre.battle;

import kr.moonseungjun.turnboundre.data.ActionDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class M1EnergyGuardTest {
    private static final UUID BATTLE_ID = UUID.fromString("00000000-0000-0000-0000-000000000104");

    private static List<BattleParticipant> participants() {
        return List.of(
                new BattleParticipant("player", BattleTeam.PLAYER, 0, 12, 200, 100, 100, 20),
                new BattleParticipant("enemy", BattleTeam.ENEMY, 1, 10, 200, 100, 100, 20)
        );
    }

    @Test void basicAndGuardGenerateCanonicalEnergyWithCap() {
        var battle = new BattleInstance(BATTLE_ID, 7L, participants());
        battle.start();

        assertEquals(BattleInstance.CommandResult.ACCEPTED,
                battle.submit(new BattleCommand(battle.revision(), "player", "basic")));
        assertEquals(10, battle.combatState("player").energy());
        battle.finishResolution();
        battle.resolveEnemyStub();
        battle.finishResolution();

        assertEquals(BattleInstance.CommandResult.ACCEPTED,
                battle.submit(new BattleCommand(battle.revision(), "player", "guard")));
        assertEquals(25, battle.combatState("player").energy());
        assertTrue(battle.combatState("player").guard());

        battle.combatState("player").gainEnergy(100);
        assertEquals(100, battle.combatState("player").energy());
    }

    @Test void skillAndBurstSpendOnlyAfterSuccessfulValidation() {
        var battle = new BattleInstance(BATTLE_ID, 8L, participants());
        battle.start();
        battle.combatState("player").gainEnergy(60);
        var skill = new ActionDefinition("skill_fire", "SKILL", 40, 10, 100);
        var burst = new ActionDefinition("burst_void", "BURST", 100, 20, 200);

        long beforeRevision = battle.revision();
        int beforeEvents = battle.eventLog().size();
        assertEquals(BattleInstance.CommandResult.INSUFFICIENT_ENERGY,
                battle.submit(new BattleCommand(beforeRevision, "player", "burst_void"), burst));
        assertEquals(60, battle.combatState("player").energy());
        assertEquals(beforeRevision, battle.revision());
        assertEquals(beforeEvents, battle.eventLog().size());
        assertEquals(BattleState.AWAIT_COMMAND, battle.state());

        assertEquals(BattleInstance.CommandResult.ACCEPTED,
                battle.submit(new BattleCommand(battle.revision(), "player", "skill_fire"), skill));
        assertEquals(20, battle.combatState("player").energy());
    }

    @Test void actionMismatchAndInvalidPassiveArePureRejections() {
        var battle = new BattleInstance(BATTLE_ID, 9L, participants());
        battle.start();
        long revision = battle.revision();
        int events = battle.eventLog().size();

        assertEquals(BattleInstance.CommandResult.ACTION_MISMATCH,
                battle.submit(new BattleCommand(revision, "player", "skill_a"),
                        new ActionDefinition("skill_b", "SKILL", 0, 0, 0)));
        assertEquals(BattleInstance.CommandResult.INVALID_ACTION,
                battle.submit(new BattleCommand(revision, "player", "passive"),
                        new ActionDefinition("passive", "PASSIVE", 0, 0, 0)));
        assertEquals(revision, battle.revision());
        assertEquals(events, battle.eventLog().size());
        assertEquals(0, battle.combatState("player").energy());
    }

    @Test void guardHalvesIncomingFinalDamageUntilNextActorTurn() {
        var guarded = new BattleInstance(BATTLE_ID, 101L, participants());
        var normal = new BattleInstance(BATTLE_ID, 101L, participants());
        guarded.start();
        normal.start();

        assertEquals(BattleInstance.CommandResult.ACCEPTED,
                guarded.submit(new BattleCommand(guarded.revision(), "player", "guard")));
        assertEquals(BattleInstance.CommandResult.ACCEPTED,
                normal.submit(new BattleCommand(normal.revision(), "player", "basic")));
        guarded.finishResolution();
        normal.finishResolution();

        var hit = new DamageService.DamageRequest(
                DamageTag.MELEE, AffinityGrade.NORMAL, 100, 0, 100, 100,
                false, 0.0D, 1.5D, 1.0D);
        guarded.resolveDamage("enemy", "player", hit);
        normal.resolveDamage("enemy", "player", hit);

        int guardedLoss = 200 - guarded.combatState("player").hp();
        int normalLoss = 200 - normal.combatState("player").hp();
        assertEquals((int) Math.floor(normalLoss * 0.5D), guardedLoss);
        assertTrue(guarded.combatState("player").guard());

        guarded.finishResolution();
        assertEquals("player", guarded.currentActorId());
        assertFalse(guarded.combatState("player").guard());
        assertTrue(guarded.eventLog().stream().anyMatch(event -> event.type().equals("GUARD_EXPIRED")));
    }

    @Test void identicalEnergyCommandsProduceIdenticalEventStream() {
        var left = new BattleInstance(BATTLE_ID, 111L, participants());
        var right = new BattleInstance(BATTLE_ID, 111L, participants());
        left.start();
        right.start();
        left.combatState("player").gainEnergy(50);
        right.combatState("player").gainEnergy(50);
        var skill = new ActionDefinition("skill", "SKILL", 30, 0, 0);

        assertEquals(BattleInstance.CommandResult.ACCEPTED,
                left.submit(new BattleCommand(left.revision(), "player", "skill"), skill));
        assertEquals(BattleInstance.CommandResult.ACCEPTED,
                right.submit(new BattleCommand(right.revision(), "player", "skill"), skill));

        assertEquals(left.combatState("player").energy(), right.combatState("player").energy());
        assertEquals(left.eventLog(), right.eventLog());
    }
}
