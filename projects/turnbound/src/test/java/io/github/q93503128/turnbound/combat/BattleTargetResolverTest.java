package io.github.q93503128.turnbound.combat;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

final class BattleTargetResolverTest {
    private static CombatantState unit(String instance, CombatantSide side, int seed) {
        SkillDefinition basic = new SkillDefinition("basic", "Basic", TargetRule.ENEMY_SINGLE, 0,
                List.of(SkillEffect.damage(1.0)));
        CombatantDefinition definition = new CombatantDefinition("generic", "Unit",
                new BattleStats(100, 20, 10, 100), "basic", List.of(basic));
        return new CombatantState(instance, definition, side, seed);
    }

    private static SkillDefinition skill(TargetRule targeting) {
        return new SkillDefinition("skill", "Target", targeting, 1,
                List.of(SkillEffect.damage(1.0)));
    }

    @Test void singleAndMultipleTargetsAreValidatedWithoutAuthoredCharacters() {
        CombatantState actor = unit("ally", CombatantSide.ALLY, 0);
        CombatantState ally = unit("ally2", CombatantSide.ALLY, 1);
        CombatantState down = unit("ally_down", CombatantSide.ALLY, 2);
        CombatantState enemy = unit("enemy", CombatantSide.ENEMY, 3);
        CombatantState enemy2 = unit("enemy2", CombatantSide.ENEMY, 4);
        down.forceDown();
        BattleState state = new BattleState(List.of(actor, ally, down, enemy, enemy2));

        assertEquals(List.of(actor), BattleTargetResolver.resolve(state, actor, skill(TargetRule.SELF)));
        assertEquals(List.of(actor, ally), BattleTargetResolver.resolve(state, actor, skill(TargetRule.ALLY_ALL)));
        assertEquals(List.of(enemy, enemy2), BattleTargetResolver.resolve(state, actor, skill(TargetRule.ENEMY_ALL)));
        assertEquals(List.of(enemy), BattleTargetResolver.resolve(state, actor, skill(TargetRule.ENEMY_SINGLE), "enemy"));
        assertEquals(List.of(down), BattleTargetResolver.resolve(state, actor, skill(TargetRule.DEAD_ALLY_SINGLE), "ally_down"));
        assertEquals(List.of(enemy, enemy2), BattleTargetResolver.resolve(state, actor, skill(TargetRule.ENEMY_TWO), "enemy", "enemy2"));

        assertThrows(IllegalArgumentException.class,
                () -> BattleTargetResolver.resolve(state, actor, skill(TargetRule.DEAD_ALLY_SINGLE), "ally2"));
        assertThrows(IllegalArgumentException.class,
                () -> BattleTargetResolver.resolve(state, actor, skill(TargetRule.ENEMY_SINGLE), "ally"));
        assertThrows(IllegalArgumentException.class,
                () -> BattleTargetResolver.resolve(state, actor, skill(TargetRule.ENEMY_TWO), "enemy", "enemy"));
        assertThrows(IllegalArgumentException.class,
                () -> BattleTargetResolver.resolve(state, actor, skill(TargetRule.ENEMY_TWO), "enemy"));
    }

    @Test void tauntRequiresTaunterForSingleAndTwoTargets() {
        CombatantState actor = unit("ally", CombatantSide.ALLY, 0);
        CombatantState a = unit("enemy_a", CombatantSide.ENEMY, 1);
        CombatantState b = unit("enemy_b", CombatantSide.ENEMY, 2);
        CombatantState c = unit("enemy_c", CombatantSide.ENEMY, 3);
        BattleState state = new BattleState(List.of(actor, a, b, c));
        actor.putStatus(new StatusInstance("taunt", a.instanceId(), 2, 0.0));
        assertEquals(List.of(a), BattleTargetResolver.resolve(state, actor, skill(TargetRule.ENEMY_SINGLE), "enemy_a"));
        assertThrows(IllegalArgumentException.class,
                () -> BattleTargetResolver.resolve(state, actor, skill(TargetRule.ENEMY_SINGLE), "enemy_b"));
        assertEquals(List.of(a, b), BattleTargetResolver.resolve(state, actor, skill(TargetRule.ENEMY_TWO), "enemy_a", "enemy_b"));
        assertThrows(IllegalArgumentException.class,
                () -> BattleTargetResolver.resolve(state, actor, skill(TargetRule.ENEMY_TWO), "enemy_b", "enemy_c"));
    }
}
