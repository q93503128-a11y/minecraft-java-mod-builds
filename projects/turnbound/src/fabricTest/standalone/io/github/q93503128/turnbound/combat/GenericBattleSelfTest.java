package io.github.q93503128.turnbound.combat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** JDK-only smoke of the migrated battle state machine. No game or Gradle needed. */
public final class GenericBattleSelfTest {
    private static SkillDefinition basic() {
        return new SkillDefinition("basic", "Basic", TargetRule.ENEMY_SINGLE, 0,
                List.of(SkillEffect.damage(1.0)));
    }
    private static CombatantState unit(String id, CombatantSide side, int speed, List<SkillDefinition> skills) {
        var definition = new CombatantDefinition(id, id, new BattleStats(100, 20, 10, speed),
                "basic", skills);
        return new CombatantState(id, definition, side, 0);
    }
    public static void main(String[] args) {
        var invalid = new SkillDefinition("bad", "Bad", TargetRule.ENEMY_SINGLE, 2,
                List.of(SkillEffect.mark("", 0)));
        var hero = unit("hero", CombatantSide.ALLY, 200, List.of(basic(), invalid));
        var enemy = unit("enemy", CombatantSide.ENEMY, 100, List.of(basic()));
        var state = new BattleState(List.of(hero, enemy));
        var engine = new GenericBattleResolver(state);
        if (!engine.beginTurn().instanceId().equals("hero")) throw new AssertionError("turn ordering");
        long gauge = hero.gauge();
        try {
            engine.resolve("hero", "bad", "enemy");
            throw new AssertionError("malformed status was accepted");
        } catch (IllegalArgumentException expected) {
            if (hero.gauge() != gauge || hero.cooldown("bad") != 0)
                throw new AssertionError("rejected action changed combat state");
        }
        int oldHp = enemy.hp();
        engine.resolve("hero", "basic", "enemy");
        if (enemy.hp() >= oldHp) throw new AssertionError("attack did no damage");
        if (state.currentActorId() != null) throw new AssertionError("turn was not released");
        var registry = new BattleSessionRegistry();
        UUID firstPlayer = UUID.randomUUID();
        UUID secondPlayer = UUID.randomUUID();
        var secondState = new BattleState(List.of(
                unit("owned", CombatantSide.ALLY, 240, List.of(basic())),
                unit("opponent", CombatantSide.ENEMY, 90, List.of(basic()))));
        var session = registry.open("minecraft:overworld", secondState,
                Map.of(firstPlayer, Set.of("owned")));
        var request = new BattleSessionAuthority.Action(session.id(), session.revision(),
                "owned", "basic", List.of("opponent"));
        if (session.submit(secondPlayer, "minecraft:overworld", request)
                != BattleSessionAuthority.Status.NOT_PARTICIPANT) throw new AssertionError("foreign action");
        if (session.submit(firstPlayer, "minecraft:the_nether", request)
                != BattleSessionAuthority.Status.WRONG_WORLD) throw new AssertionError("cross-world action");
        if (session.submit(firstPlayer, "minecraft:overworld", request)
                != BattleSessionAuthority.Status.ACCEPTED) throw new AssertionError("valid authorized action");
        int healthAfter = secondState.combatant("opponent").hp();
        if (session.submit(firstPlayer, "minecraft:overworld", request)
                != BattleSessionAuthority.Status.STALE_REVISION
                || secondState.combatant("opponent").hp() != healthAfter)
            throw new AssertionError("replayed turn changed combat");
        registry.close(session.id());
        if (registry.activeCount() != 0) throw new AssertionError("session not released");
        var firstEnemy = unit("firstEnemy", CombatantSide.ENEMY, 320, List.of(basic()));
        var firstHero = unit("firstHero", CombatantSide.ALLY, 110, List.of(basic()));
        var enemyFirstState = new BattleState(List.of(firstHero, firstEnemy));
        var owner2 = UUID.randomUUID();
        var enemyFirstSession = registry.open("minecraft:overworld", enemyFirstState,
                Map.of(owner2, Set.of("firstHero")));
        if (!"firstEnemy".equals(enemyFirstState.currentActorId())) {
            throw new AssertionError("enemy is not the first actor");
        }
        if (!enemyFirstSession.advanceEnemyTurn()) throw new AssertionError("enemy turn was not processed");
        if (firstHero.hp() >= firstHero.maxHp()) throw new AssertionError("enemy did no damage");
        registry.close(enemyFirstSession.id());
        System.out.println("PASS: actor scheduling, atomic prevalidation, damage and turn completion");
    }
}
