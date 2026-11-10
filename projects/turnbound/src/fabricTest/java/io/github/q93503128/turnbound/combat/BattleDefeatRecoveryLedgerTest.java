package io.github.q93503128.turnbound.combat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class BattleDefeatRecoveryLedgerTest {
    private static CombatantState unit(String id, CombatantSide side) {
        var basic = new SkillDefinition("basic", "Basic", TargetRule.ENEMY_SINGLE, 0,
                List.of(SkillEffect.damage(1.0)));
        var definition = new CombatantDefinition(id, id,
                new BattleStats(100, 20, 10, 160), "basic", List.of(basic));
        return new CombatantState(id, definition, side, 0);
    }

    @Test void onlyAuthoritativeEnemyVictoryCanRecoverAnOwnedSameWorldParticipant() {
        var alice = UUID.randomUUID();
        var outsider = UUID.randomUUID();
        var ally = unit("hero", CombatantSide.ALLY);
        var enemy = unit("enemy", CombatantSide.ENEMY);
        var state = new BattleState(List.of(ally, enemy));
        var registry = new BattleSessionRegistry();
        var battle = registry.open("minecraft:overworld", state, Map.of(alice, Set.of("hero")));
        var ledger = new BattleDefeatRecoveryLedger();
        var attempts = new AtomicInteger();

        assertEquals(BattleOutcome.RUNNING, battle.outcome());
        assertFalse(ledger.recover(battle, alice, "minecraft:overworld",
                () -> { attempts.incrementAndGet(); return true; }));
        ally.forceDown();
        assertEquals(BattleOutcome.ENEMY_VICTORY, battle.outcome());
        assertFalse(ledger.recover(battle, outsider, "minecraft:overworld",
                () -> { attempts.incrementAndGet(); return true; }));
        assertFalse(ledger.recover(battle, alice, "minecraft:the_nether",
                () -> { attempts.incrementAndGet(); return true; }));
        assertEquals(0, attempts.get());

        assertTrue(ledger.recover(battle, alice, "minecraft:overworld",
                () -> { attempts.incrementAndGet(); return true; }));
        assertFalse(ledger.recover(battle, alice, "minecraft:overworld",
                () -> { attempts.incrementAndGet(); return true; }));
        assertEquals(1, attempts.get(), "Duplicate server ticks cannot move same player again");

        registry.close(battle.id());
        ledger.discard(battle.id());
        assertTrue(battle.closed());
        assertFalse(ledger.recover(battle, alice, "minecraft:overworld",
                () -> { attempts.incrementAndGet(); return true; }));
        assertEquals(1, attempts.get());
    }

    @Test void temporaryUnsafeCheckpointRemainsRetryableButOneSuccessIsFinal() {
        var alice = UUID.randomUUID();
        var ally = unit("hero", CombatantSide.ALLY);
        var state = new BattleState(List.of(ally, unit("enemy", CombatantSide.ENEMY)));
        var session = new BattleSessionRegistry().open(
                "minecraft:overworld", state, Map.of(alice, Set.of("hero")));
        var ledger = new BattleDefeatRecoveryLedger();
        ally.forceDown();

        assertFalse(ledger.recover(session, alice, "minecraft:overworld", () -> false));
        assertFalse(ledger.recover(session, alice, "minecraft:overworld", () -> false));
        assertTrue(ledger.recover(session, alice, "minecraft:overworld", () -> true));
        assertFalse(ledger.recover(session, alice, "minecraft:overworld", () -> true));
        ledger.clear();
        assertTrue(ledger.recover(session, alice, "minecraft:overworld", () -> true),
                "Server shutdown clears only transient session settlement state");
    }

    @Test void sharedDefeatSettlesEachParticipantIndependentlyAndNoVictoryRecovery() {
        var alice = UUID.randomUUID();
        var bob = UUID.randomUUID();
        var a = unit("alice", CombatantSide.ALLY);
        var b = unit("bob", CombatantSide.ALLY);
        var enemy = unit("enemy", CombatantSide.ENEMY);
        var state = new BattleState(List.of(a, b, enemy));
        var registry = new BattleSessionRegistry();
        var session = registry.open("minecraft:overworld", state,
                Map.of(alice, Set.of("alice"), bob, Set.of("bob")));
        var ledger = new BattleDefeatRecoveryLedger();
        a.forceDown();
        b.forceDown();
        assertEquals(BattleOutcome.ENEMY_VICTORY, session.outcome());
        assertTrue(ledger.recover(session, alice, "minecraft:overworld", () -> true));
        assertFalse(ledger.recover(session, bob, "minecraft:overworld", () -> false));
        assertTrue(ledger.recover(session, bob, "minecraft:overworld", () -> true));
        assertFalse(ledger.recover(session, alice, "minecraft:overworld", () -> true));
        registry.close(session.id());
        ledger.discard(session.id());

        var hero = unit("victor", CombatantSide.ALLY);
        var opponent = unit("defeated", CombatantSide.ENEMY);
        var winning = new BattleState(List.of(hero, opponent));
        var winningSession = new BattleSessionRegistry().open(
                "minecraft:overworld", winning, Map.of(alice, Set.of("victor")));
        opponent.forceDown();
        assertEquals(BattleOutcome.ALLY_VICTORY, winningSession.outcome());
        assertFalse(ledger.recover(winningSession, alice, "minecraft:overworld",
                () -> { fail("Victory must not teleport a player"); return true; }));
    }

    @Test void simultaneousKnockoutIsStillDefeatRatherThanReward() {
        var owner = UUID.randomUUID();
        var ally = unit("ally", CombatantSide.ALLY);
        var enemy = unit("enemy", CombatantSide.ENEMY);
        var state = new BattleState(List.of(ally, enemy));
        var session = new BattleSessionRegistry().open(
                "minecraft:overworld", state, Map.of(owner, Set.of("ally")));
        ally.forceDown();
        enemy.forceDown();
        assertEquals(BattleOutcome.ENEMY_VICTORY, session.outcome());
        assertTrue(new BattleDefeatRecoveryLedger().recover(
                session, owner, "minecraft:overworld", () -> true));
    }
}
