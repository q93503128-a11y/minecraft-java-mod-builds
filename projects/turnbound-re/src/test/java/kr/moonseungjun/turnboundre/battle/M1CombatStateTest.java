package kr.moonseungjun.turnboundre.battle;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class M1CombatStateTest {
    private static List<BattleParticipant> participants() {
        return List.of(
                new BattleParticipant("p", BattleTeam.PLAYER, 0, 20, 100, 100, 100, 20),
                new BattleParticipant("e", BattleTeam.ENEMY, 1, 10, 100, 100, 100, 20)
        );
    }

    @Test void participantStateStartsCanonicalAndEnergyIsBounded() {
        var state = new ParticipantCombatState(participants().getFirst());
        assertEquals(100, state.hp());
        assertEquals(20, state.poise());
        assertEquals(0, state.energy());
        assertFalse(state.exposed());
        assertFalse(state.poiseGuard());

        state.gainEnergy(95);
        state.gainEnergy(20);
        assertEquals(100, state.energy());
        assertFalse(state.spendEnergy(101));
        assertTrue(state.spendEnergy(60));
        assertEquals(40, state.energy());
    }

    @Test void battleOwnedDamageMutatesHpConsumesSingleRngAndBreaksPoise() {
        var battle = new BattleInstance(UUID.fromString("00000000-0000-0000-0000-000000000201"), 123L, participants());
        battle.start();
        assertEquals(BattleInstance.CommandResult.ACCEPTED,
                battle.submit(new BattleCommand(battle.revision(), "p", "basic")));

        long beforeRevision = battle.revision();
        var result = battle.resolveDamage("p", "e", new DamageService.DamageRequest(
                DamageTag.MELEE, AffinityGrade.NORMAL, 100, 25, 100, 100,
                false, 0.0D, 1.5D, 1.0D));

        assertEquals(2, battle.rngDraws());
        assertEquals(beforeRevision + 1, battle.revision());
        assertEquals(100 - result.finalDamage(), battle.combatState("e").hp());
        assertEquals(0, battle.combatState("e").poise());
        assertTrue(battle.combatState("e").exposed());
        assertTrue(battle.eventLog().stream().anyMatch(e -> e.type().equals("EXPOSED_APPLIED")));
    }

    @Test void exposedRecoversAtTargetTurnStartAndGetsOneTurnPoiseGuard() {
        var battle = new BattleInstance(UUID.fromString("00000000-0000-0000-0000-000000000202"), 321L, participants());
        battle.start();
        battle.submit(new BattleCommand(battle.revision(), "p", "basic"));
        battle.resolveDamage("p", "e", new DamageService.DamageRequest(
                DamageTag.MELEE, AffinityGrade.WEAK, 1, 20, 100, 100,
                false, 0.0D, 1.5D, 1.0D));
        assertTrue(battle.combatState("e").exposed());

        battle.finishResolution();
        assertEquals("e", battle.currentActorId());
        assertFalse(battle.combatState("e").exposed());
        assertEquals(20, battle.combatState("e").poise());
        assertTrue(battle.combatState("e").poiseGuard());

        battle.combatState("e").applyPoiseDamage(10);
        assertEquals(15, battle.combatState("e").poise(), "POISE_GUARD must halve Poise damage");
    }

    @Test void identicalBattleDamageCommandsProduceIdenticalMutableStateAndEvents() {
        var id = UUID.fromString("00000000-0000-0000-0000-000000000203");
        var left = new BattleInstance(id, 777L, participants());
        var right = new BattleInstance(id, 777L, participants());
        runHit(left);
        runHit(right);
        assertEquals(left.combatState("e").hp(), right.combatState("e").hp());
        assertEquals(left.combatState("e").poise(), right.combatState("e").poise());
        assertEquals(left.rngDraws(), right.rngDraws());
        assertEquals(left.eventLog(), right.eventLog());
    }

    private static void runHit(BattleInstance battle) {
        battle.start();
        battle.submit(new BattleCommand(battle.revision(), "p", "basic"));
        battle.resolveDamage("p", "e", DamageService.DamageRequest.standard(
                DamageTag.PROJECTILE, AffinityGrade.RESIST, 40, 4, 100, 100, false));
    }
}
