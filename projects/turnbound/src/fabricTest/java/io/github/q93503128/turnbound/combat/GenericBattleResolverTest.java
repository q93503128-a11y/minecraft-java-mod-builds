package io.github.q93503128.turnbound.combat;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class GenericBattleResolverTest {
    private static CombatantState unit(String id, CombatantSide side, int speed, List<SkillDefinition> skills) {
        CombatantDefinition def = new CombatantDefinition(id, id,
                new BattleStats(100, 20, 10, speed), "basic", skills);
        return new CombatantState(id, def, side, 0);
    }

    private static SkillDefinition basic() {
        return new SkillDefinition("basic", "Basic", TargetRule.ENEMY_SINGLE, 0,
                List.of(SkillEffect.damage(1.0)));
    }

    @Test void validatedAttackConsumesGaugeAndDealsDamageWithoutWorld() {
        CombatantState hero = unit("hero", CombatantSide.ALLY, 200, List.of(basic()));
        CombatantState enemy = unit("enemy", CombatantSide.ENEMY, 100, List.of(basic()));
        BattleState state = new BattleState(List.of(hero, enemy));
        GenericBattleResolver resolver = new GenericBattleResolver(state);
        assertEquals("hero", resolver.beginTurn().instanceId());
        int before = enemy.hp();
        resolver.resolve("hero", "basic", "enemy");
        assertTrue(enemy.hp() < before);
        assertNull(state.currentActorId());
        assertEquals(0L, hero.gauge());
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve("hero", "basic", "enemy"));
    }

    @Test void invalidTargetAndUnsupportedEffectCannotConsumeTurn() {
        SkillDefinition special = new SkillDefinition("special", "Complex", TargetRule.ENEMY_SINGLE, 3,
                List.of(SkillEffect.dotMaxHp(.1, 2)));
        CombatantState hero = unit("hero", CombatantSide.ALLY, 200, List.of(basic(), special));
        CombatantState enemy = unit("enemy", CombatantSide.ENEMY, 100, List.of(basic()));
        BattleState state = new BattleState(List.of(hero, enemy));
        GenericBattleResolver resolver = new GenericBattleResolver(state);
        resolver.beginTurn();
        long gauge = hero.gauge();
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve("hero", "basic", "hero"));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve("hero", "special", "enemy"));
        assertEquals(gauge, hero.gauge());
        assertEquals(0, hero.cooldown("special"));
        assertEquals("hero", state.currentActorId());
    }

    @Test void turnStateDoesNotAdvanceAfterBattleResolution() {
        CombatantState hero = unit("hero", CombatantSide.ALLY, 200, List.of(basic()));
        CombatantState enemy = unit("enemy", CombatantSide.ENEMY, 100, List.of(basic()));
        enemy.takeDamage(1000);
        BattleState state = new BattleState(List.of(hero, enemy));
        assertThrows(IllegalStateException.class, () -> new GenericBattleResolver(state).beginTurn());
    }
}
