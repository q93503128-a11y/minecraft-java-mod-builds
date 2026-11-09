package io.github.q93503128.turnbound.combat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class BattleSessionAuthorityTest {
    private static SkillDefinition basic() {
        return new SkillDefinition("basic", "Basic", TargetRule.ENEMY_SINGLE, 0,
                List.of(SkillEffect.damage(1.0)));
    }
    private static CombatantState unit(String id, CombatantSide side, int speed) {
        var definition = new CombatantDefinition(id, id,
                new BattleStats(100, 20, 10, speed), "basic", List.of(basic()));
        return new CombatantState(id, definition, side, 0);
    }

    @Test void rejectsOtherPlayersWrongWorldStaleAndDuplicateWithoutDamage() {
        UUID owner = UUID.randomUUID();
        UUID outsider = UUID.randomUUID();
        var enemy = unit("enemy", CombatantSide.ENEMY, 100);
        var state = new BattleState(List.of(unit("hero", CombatantSide.ALLY, 200), enemy));
        var registry = new BattleSessionRegistry();
        var battle = registry.open("minecraft:overworld", state, Map.of(owner, Set.of("hero")));
        long firstRevision = battle.revision();
        var action = new BattleSessionAuthority.Action(
                battle.id(), firstRevision, "hero", "basic", List.of("enemy"));
        assertEquals(BattleSessionAuthority.Status.NOT_PARTICIPANT,
                battle.submit(outsider, "minecraft:overworld", action));
        assertEquals(BattleSessionAuthority.Status.WRONG_WORLD,
                registry.submit(owner, "minecraft:the_nether", action));
        assertEquals(BattleSessionAuthority.Status.ACTOR_NOT_OWNED,
                registry.submit(owner, "minecraft:overworld",
                        new BattleSessionAuthority.Action(battle.id(), firstRevision,
                                "enemy", "basic", List.of("hero"))));
        assertEquals(100, enemy.hp());
        assertEquals(BattleSessionAuthority.Status.ACCEPTED,
                registry.submit(owner, "minecraft:overworld", action));
        assertTrue(enemy.hp() < 100);
        int healthAfter = enemy.hp();
        assertEquals(BattleSessionAuthority.Status.STALE_REVISION,
                registry.submit(owner, "minecraft:overworld", action));
        assertEquals(healthAfter, enemy.hp());
        assertEquals(firstRevision + 1, battle.revision());
        registry.close(battle.id());
        assertEquals(BattleSessionAuthority.Status.NO_SESSION,
                registry.submit(owner, "minecraft:overworld", action));
    }

    @Test void sharedSessionAllowsTwoOwnersButNotControlOfEachOthersHero() {
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        var state = new BattleState(List.of(
                unit("aliceHero", CombatantSide.ALLY, 220),
                unit("bobHero", CombatantSide.ALLY, 200),
                unit("enemy", CombatantSide.ENEMY, 80)));
        var registry = new BattleSessionRegistry();
        var battle = registry.open("minecraft:overworld", state,
                Map.of(alice, Set.of("aliceHero"), bob, Set.of("bobHero")));
        assertEquals(2, battle.participants().size());
        var request = new BattleSessionAuthority.Action(battle.id(), battle.revision(),
                "aliceHero", "basic", List.of("enemy"));
        assertEquals(BattleSessionAuthority.Status.ACTOR_NOT_OWNED,
                registry.submit(bob, "minecraft:overworld", request));
        assertEquals("aliceHero", battle.viewFor(alice).currentActorId());
        assertEquals(1, battle.viewFor(alice).combatants().get(0).ownedSkills().size());
        assertEquals(0, battle.viewFor(bob).combatants().get(0).ownedSkills().size());
        assertThrows(IllegalStateException.class,
                () -> registry.open("minecraft:overworld", state,
                        Map.of(alice, Set.of("aliceHero"))));
        registry.close(battle.id());
        assertEquals(0, registry.activeCount());
    }

    @Test void refusesUngroundedAllyAssignmentAndKeepsRegistryClean() {
        UUID owner = UUID.randomUUID();
        var registry = new BattleSessionRegistry();
        var state = new BattleState(List.of(
                unit("hero", CombatantSide.ALLY, 200),
                unit("enemy", CombatantSide.ENEMY, 100)));
        assertThrows(IllegalArgumentException.class,
                () -> registry.open("minecraft:overworld", state,
                        Map.of(owner, Set.of("enemy"))));
        assertEquals(0, registry.activeCount());
    }
}
