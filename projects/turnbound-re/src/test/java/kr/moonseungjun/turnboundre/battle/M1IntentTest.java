package kr.moonseungjun.turnboundre.battle;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class M1IntentTest {
    private static List<BattleParticipant> participants() {
        return List.of(
                new BattleParticipant("p", BattleTeam.PLAYER, 0, 20, 100, 100, 100, 20),
                new BattleParticipant("e", BattleTeam.ENEMY, 1, 10, 100, 100, 100, 20)
        );
    }

    private static DamageService.DamageRequest poiseBreak() {
        return new DamageService.DamageRequest(
                DamageTag.MELEE, AffinityGrade.WEAK, 1, 20, 100, 100,
                false, 0.0D, 1.5D, 1.0D);
    }

    @Test void enemyPublishesIntentBeforeItsTurnAndAiResolvesExactlyThatAction() {
        var battle = new BattleInstance(UUID.fromString("00000000-0000-0000-0000-000000000401"), 41L, participants());
        battle.start();
        assertEquals("basic", battle.enemyIntent("e").actionId());
        assertTrue(battle.eventLog().stream().anyMatch(e -> e.type().equals("INTENT_REVEALED") && e.actorId().equals("e")));

        battle.submit(new BattleCommand(battle.revision(), "p", "basic"));
        battle.finishResolution();
        assertEquals("e", battle.currentActorId());
        battle.resolveEnemyStub();

        assertEquals("basic", battle.eventLog().stream()
                .filter(e -> e.type().equals("AI_COMMAND") && e.actorId().equals("e"))
                .reduce((a, b) -> b).orElseThrow().detail());
    }

    @Test void cancelableIntentChangesToRecoverBeforeEnemyAiCommand() {
        var battle = new BattleInstance(UUID.fromString("00000000-0000-0000-0000-000000000402"), 42L, participants());
        battle.start();
        battle.setEnemyIntent("e", new EnemyIntent("heavy_strike", EnemyIntent.Type.ATTACK,
                EnemyIntent.Targeting.SINGLE, EnemyIntent.Risk.DANGEROUS, true, null));

        battle.submit(new BattleCommand(battle.revision(), "p", "basic"));
        battle.resolveDamage("p", "e", poiseBreak());
        assertEquals("recover", battle.enemyIntent("e").actionId());

        int changedIndex = indexOfLast(battle, "INTENT_CHANGED");
        battle.finishResolution();
        battle.resolveEnemyStub();
        int aiIndex = indexOfLast(battle, "AI_COMMAND");
        assertTrue(changedIndex >= 0 && changedIndex < aiIndex, "INTENT_CHANGED must precede changed AI action");
        assertEquals("recover", battle.eventLog().get(aiIndex).detail());
        assertTrue(battle.eventLog().stream().anyMatch(e -> e.type().equals("RECOVER") && e.actorId().equals("e")));
    }

    @Test void nonCancelableIntentUsesConfiguredDowngradeInsteadOfRecover() {
        var battle = new BattleInstance(UUID.fromString("00000000-0000-0000-0000-000000000403"), 43L, participants());
        battle.start();
        battle.setEnemyIntent("e", new EnemyIntent("ultimate_blast", EnemyIntent.Type.SPECIAL,
                EnemyIntent.Targeting.ALL, EnemyIntent.Risk.ULTIMATE, false, "weak_blast"));

        battle.submit(new BattleCommand(battle.revision(), "p", "basic"));
        battle.resolveDamage("p", "e", poiseBreak());
        assertEquals("weak_blast", battle.enemyIntent("e").actionId());
        assertFalse(battle.enemyIntent("e").breakCancelable());
        assertEquals(EnemyIntent.Risk.NORMAL, battle.enemyIntent("e").risk());
        assertTrue(battle.eventLog().stream().anyMatch(e -> e.type().equals("INTENT_CHANGED")
                && e.detail().contains("ultimate_blast->weak_blast")
                && e.detail().contains("poise_break_downgrade")));
    }

    @Test void identicalIntentBreakFlowProducesIdenticalEventStream() {
        var id = UUID.fromString("00000000-0000-0000-0000-000000000404");
        var left = new BattleInstance(id, 44L, participants());
        var right = new BattleInstance(id, 44L, participants());
        runCancelableBreak(left);
        runCancelableBreak(right);
        assertEquals(left.eventLog(), right.eventLog());
        assertEquals(left.enemyIntent("e"), right.enemyIntent("e"));
    }

    private static void runCancelableBreak(BattleInstance battle) {
        battle.start();
        battle.setEnemyIntent("e", new EnemyIntent("heavy_strike", EnemyIntent.Type.ATTACK,
                EnemyIntent.Targeting.SINGLE, EnemyIntent.Risk.DANGEROUS, true, null));
        battle.submit(new BattleCommand(battle.revision(), "p", "basic"));
        battle.resolveDamage("p", "e", poiseBreak());
    }

    private static int indexOfLast(BattleInstance battle, String type) {
        for (int i = battle.eventLog().size() - 1; i >= 0; i--) {
            if (battle.eventLog().get(i).type().equals(type)) return i;
        }
        return -1;
    }
}
